package io.pcast.module

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import io.pcast.config.CONFIG_FILES
import io.pcast.config.Configuration
import io.pcast.module.auth.AuthService
import io.pcast.module.auth.UserSeeder
import io.pcast.module.auth.model.RefreshTokenRepository
import io.pcast.module.auth.model.UserRepository
import io.pcast.module.feed.FeedService
import io.pcast.module.feed.model.FeedRepository
import io.pcast.module.sync.SyncService
import io.pcast.plugins.configureDatabase
import io.pcast.plugins.configureTestDatabase
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module

val configModule =
    module {
        single(createdAtStart = true) { buildConfigurationFromFiles() }
    }

val dbModule =
    module {
        single<Database>(createdAtStart = true) { configureDatabase(get()) }
    }

val testDbModule =
    module {
        single<Database>(createdAtStart = true) { configureTestDatabase(get()) }
    }

val appModule =
    module {
        single { FeedRepository(get()) }
        single { UserRepository(get()) }
        single { RefreshTokenRepository(get()) }

        single { FeedService(get()) }
        single { SyncService() }
        single { AuthService(get(), get(), get()) }
        single { UserSeeder(get(), get()) }
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
