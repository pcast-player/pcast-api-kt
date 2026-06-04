package io.pcast.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.pcast.module.auth.passkey.request.PasskeyAuthenticationOptionsRequest
import io.pcast.module.auth.request.LoginRequest
import io.pcast.module.auth.request.RegisterRequest
import io.pcast.module.feed.request.FeedRequest
import io.pcast.module.sync.request.ValidateSyncPhraseRequest

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private const val PASSWORD_MIN_LENGTH = 12
private const val PASSWORD_MAX_BYTES = 72 // bcrypt hard limit
private const val FIELD_MAX_LENGTH = 255
private const val SYNC_PHRASE_WORD_COUNT = 24

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<LoginRequest> { req ->
            val errors = mutableListOf<String>()
            if (!EMAIL_REGEX.matches(req.email)) errors += "email: invalid format"
            if (req.password.isEmpty()) errors += "password: must not be blank"
            if (req.password.toByteArray().size > PASSWORD_MAX_BYTES) {
                errors += "password: exceeds maximum of $PASSWORD_MAX_BYTES bytes"
            }
            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors.joinToString("; "))
        }

        validate<RegisterRequest> { req ->
            val errors = mutableListOf<String>()
            if (!EMAIL_REGEX.matches(req.email)) errors += "email: invalid format"
            errors += validateNewPassword(req.password)
            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors.joinToString("; "))
        }

        validate<PasskeyAuthenticationOptionsRequest> { req ->
            val email = req.email
            if (email != null && email.isNotBlank() && !EMAIL_REGEX.matches(email)) {
                ValidationResult.Invalid("email: invalid format")
            } else {
                ValidationResult.Valid
            }
        }

        validate<FeedRequest> { req ->
            val errors = mutableListOf<String>()
            if (req.title.isBlank()) errors += "title: must not be blank"
            if (req.title.length > FIELD_MAX_LENGTH) errors += "title: exceeds $FIELD_MAX_LENGTH characters"
            if (req.url.isBlank()) errors += "url: must not be blank"
            if (req.url.length > FIELD_MAX_LENGTH) errors += "url: exceeds $FIELD_MAX_LENGTH characters"
            val scheme = req.url.substringBefore("://").lowercase()
            if (scheme != "http" && scheme != "https") errors += "url: must use http or https scheme"
            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors.joinToString("; "))
        }

        validate<ValidateSyncPhraseRequest> { req ->
            val wordCount =
                req.syncPhrase
                    .trim()
                    .split(Regex("\\s+"))
                    .size
            if (wordCount != SYNC_PHRASE_WORD_COUNT) {
                ValidationResult.Invalid("syncPhrase: must contain exactly $SYNC_PHRASE_WORD_COUNT words")
            } else {
                ValidationResult.Valid
            }
        }
    }
}

/**
 * Validates password length for account creation.
 * Must be called explicitly in the create-user path (not wired into RequestValidation
 * because there is currently no dedicated CreateUserRequest DTO).
 */
fun validateNewPassword(password: String): List<String> {
    val errors = mutableListOf<String>()
    if (password.length < PASSWORD_MIN_LENGTH) {
        errors += "password: minimum length is $PASSWORD_MIN_LENGTH characters"
    }
    if (password.toByteArray().size > PASSWORD_MAX_BYTES) {
        errors += "password: exceeds maximum of $PASSWORD_MAX_BYTES bytes"
    }
    return errors
}
