package io.pcast.module

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import io.pcast.config.CONFIG_FILES
import io.pcast.config.Configuration
import io.pcast.plugins.configureDatabase
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module

private const val POSTGRES_DRIVER = "org.postgresql.Driver"

/**
 * Environment name controlling startup validation.
 * Set PCAST_ENV=production in deployments to enforce strict config checks.
 * Recognised values: "development" (default), "test", "production".
 */
val PCAST_ENV: String get() = System.getenv("PCAST_ENV") ?: "development"

val configModule =
    module {
        single(createdAtStart = true) {
            validateEnvironment()
            buildConfigurationFromFiles().also { config ->
                validateJwtSecret(config.jwt.secret)
                validateDatabaseCredentials(config)
            }
        }
    }

val dbModule =
    module {
        single<Database>(createdAtStart = true) { configureDatabase(get()) }
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

/**
 * When PCAST_ENV=production, require app.prod.conf to be on the classpath.
 * This prevents the application from booting in production using only
 * placeholder defaults or, worse, the committed test secret.
 */
private fun validateEnvironment() {
    if (PCAST_ENV != "production") return

    val prodConf = object {}.javaClass.getResource("/app.prod.conf")
    require(prodConf != null) {
        "PCAST_ENV=production but /app.prod.conf was not found on the classpath. " +
            "Deploy app.prod.conf alongside the JAR or mount it as a secret."
    }
}

/**
 * When connecting to PostgreSQL, require non-blank username and password.
 */
private fun validateDatabaseCredentials(config: Configuration) {
    if (config.database.driver != POSTGRES_DRIVER) return

    require(config.database.user.isNotBlank()) {
        "database.user must not be blank when using the PostgreSQL driver."
    }
    require(!config.database.password.isNullOrBlank()) {
        "database.password must not be blank when using the PostgreSQL driver."
    }
}

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
