package io.pcast.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.pcast.model.feed.FeedsTable
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

private const val DATABASE_JDBC_URL = "jdbc:postgresql://localhost:5433/pcast"
private const val TEST_DATABASE_JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
private const val DATABASE_MIGRATIONS_LOCATION = "db/migration/postgres"
private const val TEST_DATABASE_MIGRATIONS_LOCATION = "db/migration/h2"
private const val POSTGRES_DRIVER = "org.postgresql.Driver"
private const val H2_DRIVER = "org.h2.Driver"
private const val POSTGRES_USER = "pcast"
private const val POSTGRES_PASSWORD = "pcast"

fun configureDatabase() = connectAndMigrate(
    dataSource = hikariDataSource {
        jdbcUrl = DATABASE_JDBC_URL
        driverClassName = POSTGRES_DRIVER
        username = POSTGRES_USER
        password = POSTGRES_PASSWORD
    },
    migrationLocation = DATABASE_MIGRATIONS_LOCATION
)

fun configureTestDatabase() = connectAndMigrate(
    dataSource = hikariDataSource {
        jdbcUrl = TEST_DATABASE_JDBC_URL
        driverClassName = H2_DRIVER
    },
    migrationLocation = TEST_DATABASE_MIGRATIONS_LOCATION
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
