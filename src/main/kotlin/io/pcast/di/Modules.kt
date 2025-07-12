package io.pcast.di

import io.pcast.config.loadConfiguration
import io.pcast.controller.feed.FeedHandler
import io.pcast.controller.sync.SyncHandler
import io.pcast.model.feed.FeedRepository
import io.pcast.model.feed.FeedRepositoryImpl
import io.pcast.plugins.configureDatabase
import io.pcast.plugins.configureTestDatabase
import org.koin.dsl.module

val configModule =
    module {
        single { loadConfiguration() }
    }

val dbModule =
    module {
        single { configureDatabase(get()) }
    }

val testDbModule =
    module {
        single { configureTestDatabase(get()) }
    }

val appModule =
    module {
        single<FeedRepository> { FeedRepositoryImpl(get()) }

        single { FeedHandler(get()) }
        single { SyncHandler() }
    }
