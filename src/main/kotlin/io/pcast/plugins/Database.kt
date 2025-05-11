package io.pcast.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.pcast.config.Configuration
import io.pcast.model.feed.FeedsTable
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun configureDatabase(config: Configuration) = connectAndMigrate(
    dataSource = hikariDataSource {
        jdbcUrl = config.database.jdbcUrl
        driverClassName = config.database.driver
        username = config.database.user
        password = config.database.password
    },
    migrationLocation = config.database.migrationsLocation
)

fun configureTestDatabase(config: Configuration) = connectAndMigrate(
    dataSource = hikariDataSource {
        jdbcUrl = config.database.jdbcUrl
        driverClassName = config.database.driver
    },
    migrationLocation = config.database.migrationsLocation
)

private fun connectAndMigrate(
    dataSource: HikariDataSource,
    migrationLocation: String
): Database {
    flyway {
        dataSource(dataSource)
        locations(migrationLocation)
    }

    val db = Database.connect(dataSource)

    transaction(db) {
        SchemaUtils.create(FeedsTable)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        db.connector().close()
        dataSource.close()
    })

    return db
}

private fun hikariDataSource(
    lambda: HikariConfig.() -> Unit
) = HikariDataSource(HikariConfig().apply(lambda))

private fun flyway(
    lambda: FluentConfiguration.() -> Unit
) = Flyway.configure().apply(lambda).load().migrate()
