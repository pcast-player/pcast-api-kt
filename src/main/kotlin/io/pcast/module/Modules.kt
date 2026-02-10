package io.pcast.module

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import io.pcast.config.CONFIG_FILES
import io.pcast.config.Configuration
import io.pcast.plugins.configureDatabase
import io.pcast.plugins.configureTestDatabase
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module

val configModule =
    module {
        single(createdAtStart = true) {
            buildConfigurationFromFiles().also { config ->
                validateJwtSecret(config.jwt.secret)
            }
        }
    }

val dbModule =
    module {
        single<Database>(createdAtStart = true) { configureDatabase(get()) }
    }

val testDbModule =
    module {
        single<Database>(createdAtStart = true) { configureTestDatabase(get()) }
    }

private fun buildConfigurationFromFiles() =
    buildConfiguration<Configuration> {
        for (file in CONFIG_FILES) {
            addResourceSource(
                resource = file.relativeResourcePath,
                optional = file.isOptional,
                allowEmpty = file.allowEmpty,
            )
        }
    }

private inline fun <reified T : Any> buildConfiguration(builder: ConfigLoaderBuilder.() -> Unit) =
    ConfigLoaderBuilder
        .default()
        .apply(builder)
        .build()
        .loadConfigOrThrow<T>()

private fun validateJwtSecret(secret: String) {
    require(secret.isNotBlank()) {
        "JWT secret must not be blank. " +
            "Please configure a valid secret in your configuration file."
    }
    require(secret.length >= 32) {
        val length = secret.length
        "JWT secret must be at least 32 characters long for security. Current length: $length"
    }
}
