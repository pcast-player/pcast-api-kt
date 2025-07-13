package io.pcast.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

fun loadConfiguration() =
    ConfigLoaderBuilder
        .default()
        .addResourceSource("/app.prod.conf", optional = true)
        .addResourceSource("/app.local.conf", optional = true)
        .addResourceSource("/app.conf", optional = true)
        .build()
        .loadConfigOrThrow<Configuration>()
