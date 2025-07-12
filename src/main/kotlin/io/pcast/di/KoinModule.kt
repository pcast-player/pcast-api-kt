package io.pcast.di

import io.pcast.config.loadConfiguration
import io.pcast.controller.feed.FeedHandler
import io.pcast.controller.sync.SyncHandler
import io.pcast.model.feed.FeedRepository
import io.pcast.model.feed.FeedRepositoryImpl
import io.pcast.plugins.configureDatabase
import org.koin.dsl.module

val appModule =
    module {
        single { loadConfiguration() }
        single { configureDatabase(get()) }
        single<FeedRepository> { FeedRepositoryImpl(get()) }

        single { FeedHandler(get()) }
        single { SyncHandler() }
    }
