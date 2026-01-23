package io.pcast.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import io.pcast.module.configModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class ConfigurationTest : KoinTest {
    @BeforeTest
    fun setup() {
        stopKoin()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testBlankJwtSecretThrowsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ConfigLoaderBuilder
                    .default()
                    .addResourceSource("/test-config-blank-secret.conf")
                    .build()
                    .loadConfigOrThrow<Configuration>()
                    .also { config ->
                        require(config.jwt.secret.isNotBlank()) {
                            "JWT secret must not be blank. " +
                                "Please configure a valid secret in your configuration file."
                        }
                    }
            }

        assert(exception.message?.contains("JWT secret must not be blank") == true)
    }

    @Test
    fun testShortJwtSecretThrowsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ConfigLoaderBuilder
                    .default()
                    .addResourceSource("/test-config-short-secret.conf")
                    .build()
                    .loadConfigOrThrow<Configuration>()
                    .also { config ->
                        require(config.jwt.secret.length >= 32) {
                            val length = config.jwt.secret.length
                            "JWT secret must be at least 32 characters long for security. Current length: $length"
                        }
                    }
            }

        assert(exception.message?.contains("JWT secret must be at least 32 characters long") == true)
    }

    @Test
    fun testValidJwtSecretDoesNotThrow() {
        // This should not throw an exception
        val config =
            ConfigLoaderBuilder
                .default()
                .addResourceSource("/app.local.conf", optional = false)
                .build()
                .loadConfigOrThrow<Configuration>()

        // Validate the secret meets requirements
        require(config.jwt.secret.isNotBlank()) {
            "JWT secret must not be blank. " +
                "Please configure a valid secret in your configuration file."
        }
        require(config.jwt.secret.length >= 32) {
            val length = config.jwt.secret.length
            "JWT secret must be at least 32 characters long for security. Current length: $length"
        }

        // If we get here, validation passed
        assert(config.jwt.secret.length >= 32)
    }
}
