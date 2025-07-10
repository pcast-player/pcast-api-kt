package io.pcast.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

fun loadConfiguration() =
    ConfigLoaderBuilder
        .default()
        .addResourceSource("/app.conf")
        .build()
        .loadConfigOrThrow<Configuration>()
