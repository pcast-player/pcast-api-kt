package io.pcast.config

data class Configuration(
    val database: Database,
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
