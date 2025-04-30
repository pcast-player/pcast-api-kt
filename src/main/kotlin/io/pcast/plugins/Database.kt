package io.pcast.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.pcast.model.feed.FeedsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5433/pcast"
        driverClassName = "org.postgresql.Driver"
        username = "pcast"
        password = "pcast"
    }

    val dataSource = HikariDataSource(config)
    val db = Database.connect(dataSource)

    transaction {
        SchemaUtils.create(FeedsTable)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        db.connector().close()
        dataSource.close()
    })
}