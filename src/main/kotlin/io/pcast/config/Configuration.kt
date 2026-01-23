package io.pcast.config

data class Configuration(
    val database: Database,
    val jwt: JwtConfig,
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenExpireMinutes: Long = 60,
    val refreshTokenExpireDays: Long = 30,
)

data class ConfigFile(
    val file: String,
    val isOptional: Boolean = false,
    val allowEmpty: Boolean = false,
) {
    val relativeResourcePath = "/$file"
}

val CONFIG_FILES =
    listOf(
        ConfigFile("app.prod.conf", isOptional = true),
        ConfigFile("app.local.conf", isOptional = true),
        ConfigFile("app.conf", isOptional = true),
    )
