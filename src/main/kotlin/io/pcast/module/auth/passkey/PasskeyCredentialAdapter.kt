package io.pcast.module.auth.passkey

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.ByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import io.pcast.module.auth.model.PasskeyCredentialRepository
import io.pcast.module.auth.model.UserRepository
import org.koin.core.annotation.Single
import java.util.Optional

@Single
class PasskeyCredentialAdapter(
    private val userRepository: UserRepository,
    private val credentialRepository: PasskeyCredentialRepository,
) : CredentialRepository {
    override fun getCredentialIdsForUsername(username: String): Set<PublicKeyCredentialDescriptor> =
        userRepository
            .findByEmail(username)
            ?.let { credentialRepository.findDescriptorsByUser(it.id) }
            ?: emptySet()

    override fun getUserHandleForUsername(username: String): Optional<ByteArray> =
        userRepository
            .findByEmail(username)
            ?.passkeyUserHandle
            ?.let { ByteArray.fromBase64Url(it) }
            .let { Optional.ofNullable(it) }

    override fun getUsernameForUserHandle(userHandle: ByteArray): Optional<String> =
        userRepository
            .findByPasskeyUserHandle(userHandle.base64Url)
            ?.email
            .let { Optional.ofNullable(it) }

    override fun lookup(
        credentialId: ByteArray,
        userHandle: ByteArray,
    ): Optional<RegisteredCredential> =
        credentialRepository
            .findRegisteredByCredentialIdAndUserHandle(credentialId, userHandle)
            .let { Optional.ofNullable(it) }

    override fun lookupAll(credentialId: ByteArray): Set<RegisteredCredential> =
        credentialRepository.findRegisteredByCredentialId(credentialId)
}
