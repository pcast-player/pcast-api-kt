package io.pcast.di

import io.pcast.config.loadConfiguration
import io.pcast.model.feed.FeedRepository
import io.pcast.model.feed.FeedRepositoryImpl
import io.pcast.plugins.configureDatabase
import io.pcast.plugins.configureTestDatabase
import io.pcast.service.feed.FeedService
import io.pcast.service.sync.SyncService
import org.koin.dsl.module

val configModule =
    module {
        single(createdAtStart = true) { loadConfiguration() }
    }

val dbModule =
    module {
        single(createdAtStart = true) { configureDatabase(get()) }
    }

val testDbModule =
    module {
        single(createdAtStart = true) { configureTestDatabase(get()) }
    }

val appModule =
    module {
        single<FeedRepository> { FeedRepositoryImpl(get()) }

        single { FeedService(get()) }
        single { SyncService() }
    }
