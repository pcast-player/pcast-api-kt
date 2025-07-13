package io.pcast.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

fun loadConfiguration() =
    hoplite<Configuration> {
        addResourceSource("/app.prod.conf", optional = true)
        addResourceSource("/app.local.conf", optional = true)
        addResourceSource("/app.conf", optional = true)
    }

private inline fun <reified T : Any> hoplite(builder: ConfigLoaderBuilder.() -> Unit) =
    ConfigLoaderBuilder
        .default()
        .apply(builder)
        .build()
        .loadConfigOrThrow<T>()
