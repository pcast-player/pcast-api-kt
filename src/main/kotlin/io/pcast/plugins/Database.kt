package io.pcast.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.pcast.model.feed.FeedsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase(): Database {
    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5433/pcast"
        driverClassName = "org.postgresql.Driver"
        username = "pcast"
        password = "pcast"
    }

    val dataSource = HikariDataSource(config)
    val db = Database.connect(dataSource)

    createSchemas(db)

    Runtime.getRuntime().addShutdownHook(Thread {
        db.connector().close()
        dataSource.close()
    })

    return db
}

fun Application.configureTestDatabase(): Database {
    val db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    createSchemas(db)

    return db
}

private fun createSchemas(db: Database) {
    transaction(db) {
        SchemaUtils.create(FeedsTable)
    }
}