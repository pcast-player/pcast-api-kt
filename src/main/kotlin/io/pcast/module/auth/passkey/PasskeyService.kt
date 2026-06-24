package io.pcast.module.auth.passkey

import com.yubico.webauthn.AssertionRequest
import com.yubico.webauthn.FinishAssertionOptions
import com.yubico.webauthn.FinishRegistrationOptions
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartAssertionOptions
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.AttestationConveyancePreference
import com.yubico.webauthn.data.AuthenticatorAssertionResponse
import com.yubico.webauthn.data.AuthenticatorAttestationResponse
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs
import com.yubico.webauthn.data.PublicKeyCredential
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import com.yubico.webauthn.data.RelyingPartyIdentity
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.UserIdentity
import com.yubico.webauthn.data.UserVerificationRequirement
import com.yubico.webauthn.exception.AssertionFailedException
import com.yubico.webauthn.exception.RegistrationFailedException
import io.pcast.config.Configuration
import io.pcast.error.AbortError
import io.pcast.error.HttpError
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.model.PasskeyChallengeRepository
import io.pcast.module.auth.model.PasskeyChallengeType
import io.pcast.module.auth.model.PasskeyCredential
import io.pcast.module.auth.model.PasskeyCredentialRepository
import io.pcast.module.auth.model.UserRepository
import io.pcast.module.auth.response.TokenResponse
import org.koin.core.annotation.Single
import java.time.LocalDateTime
import java.util.UUID

@Single
class PasskeyService(
    private val config: Configuration,
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val credentialRepository: PasskeyCredentialRepository,
    private val challengeRepository: PasskeyChallengeRepository,
    credentialAdapter: PasskeyCredentialAdapter,
) {
    private val relyingParty =
        RelyingParty
            .builder()
            .identity(
                RelyingPartyIdentity
                    .builder()
                    .id(config.passkey.rpId)
                    .name(config.passkey.rpName)
                    .build(),
            ).credentialRepository(credentialAdapter)
            .origins(config.passkey.allowedOrigins.toSet())
            .attestationConveyancePreference(AttestationConveyancePreference.NONE)
            .allowUntrustedAttestation(true)
            .validateSignatureCounter(true)
            .build()

    fun startRegistration(userId: UUID): String {
        val user = userRepository.findById(userId) ?: throw AbortError(HttpError.Unauthorized, "Unauthorized")
        val request =
            relyingParty.startRegistration(
                StartRegistrationOptions
                    .builder()
                    .user(
                        UserIdentity
                            .builder()
                            .name(user.email)
                            .displayName(user.email)
                            .id(ByteArray.fromBase64Url(user.passkeyUserHandle))
                            .build(),
                    ).authenticatorSelection(
                        AuthenticatorSelectionCriteria
                            .builder()
                            .residentKey(ResidentKeyRequirement.REQUIRED)
                            .userVerification(UserVerificationRequirement.REQUIRED)
                            .build(),
                    ).timeout(config.passkey.timeoutMillis)
                    .build(),
            )

        challengeRepository.create(
            userId = user.id,
            type = PasskeyChallengeType.Registration,
            challenge = request.challenge.base64Url,
            requestJson = request.toJson(),
            expiresAt = LocalDateTime.now().plusSeconds(config.passkey.challengeTtlSeconds),
        )

        return request.toCredentialsCreateJson()
    }

    fun startSignupRegistration(email: String): String {
        val normalizedEmail = email.trim().lowercase()
        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw AbortError(HttpError.Conflict, "User already exists")
        }

        val passkeyUserHandle = userRepository.generateUserHandle()
        val request =
            startRegistrationRequest(
                email = normalizedEmail,
                passkeyUserHandle = passkeyUserHandle,
            )

        challengeRepository.create(
            userId = null,
            type = PasskeyChallengeType.SignupRegistration,
            challenge = request.challenge.base64Url,
            requestJson = request.toJson(),
            expiresAt = LocalDateTime.now().plusSeconds(config.passkey.challengeTtlSeconds),
            email = normalizedEmail,
            passkeyUserHandle = passkeyUserHandle,
        )

        return request.toCredentialsCreateJson()
    }

    fun finishRegistration(
        userId: UUID,
        responseJson: String,
    ): PasskeyCredential {
        val response = parseRegistrationResponse(responseJson)
        val challenge =
            consumeChallenge(response.response.clientData.challenge.base64Url, PasskeyChallengeType.Registration)

        if (challenge.userId != userId) throw AbortError(HttpError.Unauthorized, "Unauthorized")

        val result = finishRegistrationOrAbort(challenge.requestJson, response)

        return credentialRepository.create(
            userId = userId,
            credentialId = result.keyId.id.base64Url,
            publicKeyCose = result.publicKeyCose.base64Url,
            signatureCount = result.signatureCount,
            transports = response.response.transports.joinToString(",") { it.id },
            nickname = null,
            backupEligible = result.isBackupEligible,
            backedUp = result.isBackedUp,
        )
    }

    fun finishSignupRegistration(responseJson: String): TokenResponse {
        val response = parseRegistrationResponse(responseJson)
        val challenge =
            consumeChallenge(response.response.clientData.challenge.base64Url, PasskeyChallengeType.SignupRegistration)
        val email =
            challenge.email ?: throw AbortError(HttpError.Unauthorized, "Passkey challenge expired or already used")
        val passkeyUserHandle =
            challenge.passkeyUserHandle ?: throw AbortError(
                HttpError.Unauthorized,
                "Passkey challenge expired or already used",
            )

        if (userRepository.findByEmail(email) != null) {
            throw AbortError(HttpError.Conflict, "User already exists")
        }

        val result = finishRegistrationOrAbort(challenge.requestJson, response)
        val user = userRepository.createPasskeyOnly(email, passkeyUserHandle)

        credentialRepository.create(
            userId = user.id,
            credentialId = result.keyId.id.base64Url,
            publicKeyCose = result.publicKeyCose.base64Url,
            signatureCount = result.signatureCount,
            transports = response.response.transports.joinToString(",") { it.id },
            nickname = null,
            backupEligible = result.isBackupEligible,
            backedUp = result.isBackedUp,
        )

        return authService.issueTokenPair(user)
    }

    fun startAuthentication(email: String?): String {
        val builder =
            StartAssertionOptions
                .builder()
                .userVerification(UserVerificationRequirement.REQUIRED)
                .timeout(config.passkey.timeoutMillis)

        if (!email.isNullOrBlank()) {
            userRepository.findByEmail(email)?.let { builder.username(it.email) }
        }

        val request = relyingParty.startAssertion(builder.build())
        challengeRepository.create(
            userId = email?.takeIf { it.isNotBlank() }?.let { userRepository.findByEmail(it)?.id },
            type = PasskeyChallengeType.Authentication,
            challenge = request.publicKeyCredentialRequestOptions.challenge.base64Url,
            requestJson = request.toJson(),
            expiresAt = LocalDateTime.now().plusSeconds(config.passkey.challengeTtlSeconds),
        )

        return request.toCredentialsGetJson()
    }

    fun finishAuthentication(responseJson: String): TokenResponse {
        val response = parseAssertionResponse(responseJson)
        val challenge =
            consumeChallenge(response.response.clientData.challenge.base64Url, PasskeyChallengeType.Authentication)
        val result = finishAssertionOrAbort(challenge.requestJson, response)

        if (!result.isSuccess) throw AbortError(HttpError.Unauthorized, "Unauthorized")

        credentialRepository.updateAfterAuthentication(
            credentialId = result.credential.credentialId.base64Url,
            signatureCount = result.signatureCount,
            backupEligible = result.isBackupEligible,
            backedUp = result.isBackedUp,
        )

        val user =
            userRepository.findByEmail(result.username)
                ?: throw AbortError(HttpError.Unauthorized, "Unauthorized")

        return authService.issueTokenPair(user)
    }

    fun listCredentials(userId: UUID): List<PasskeyCredential> = credentialRepository.listByUser(userId)

    fun deleteCredential(
        userId: UUID,
        credentialId: UUID,
    ): Boolean = credentialRepository.deleteForUser(userId, credentialId)

    private fun consumeChallenge(
        challenge: String,
        type: PasskeyChallengeType,
    ) = challengeRepository.consume(challenge, type)
        ?: throw AbortError(HttpError.Unauthorized, "Passkey challenge expired or already used")

    private fun parseRegistrationResponse(responseJson: String) =
        runCatching { PublicKeyCredential.parseRegistrationResponseJson(responseJson) }
            .getOrElse { throw AbortError(HttpError.BadRequest, "Invalid passkey registration response", it) }

    private fun startRegistrationRequest(
        email: String,
        passkeyUserHandle: String,
    ): PublicKeyCredentialCreationOptions =
        relyingParty.startRegistration(
            StartRegistrationOptions
                .builder()
                .user(
                    UserIdentity
                        .builder()
                        .name(email)
                        .displayName(email)
                        .id(ByteArray.fromBase64Url(passkeyUserHandle))
                        .build(),
                ).authenticatorSelection(
                    AuthenticatorSelectionCriteria
                        .builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build(),
                ).timeout(config.passkey.timeoutMillis)
                .build(),
        )

    private fun parseAssertionResponse(responseJson: String) =
        runCatching { PublicKeyCredential.parseAssertionResponseJson(responseJson) }
            .getOrElse { throw AbortError(HttpError.BadRequest, "Invalid passkey authentication response", it) }

    private fun finishRegistrationOrAbort(
        requestJson: String,
        response: PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs>,
    ) = runCatching {
        relyingParty.finishRegistration(
            FinishRegistrationOptions
                .builder()
                .request(PublicKeyCredentialCreationOptions.fromJson(requestJson))
                .response(response)
                .build(),
        )
    }.getOrElse { cause ->
        if (cause is RegistrationFailedException) {
            throw AbortError(HttpError.BadRequest, "Invalid passkey registration response", cause)
        }
        throw cause
    }

    private fun finishAssertionOrAbort(
        requestJson: String,
        response: PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs>,
    ) = runCatching {
        relyingParty.finishAssertion(
            FinishAssertionOptions
                .builder()
                .request(AssertionRequest.fromJson(requestJson))
                .response(response)
                .build(),
        )
    }.getOrElse { cause ->
        if (cause is AssertionFailedException) {
            throw AbortError(HttpError.Unauthorized, "Unauthorized", cause)
        }
        throw cause
    }
}
