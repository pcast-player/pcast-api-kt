package io.pcast.module

import io.pcast.config.Configuration
import io.pcast.config.Database
import io.pcast.config.JwtConfig
import io.pcast.plugins.configureDatabase
import org.koin.dsl.module
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import java.util.UUID
import org.jetbrains.exposed.v1.jdbc.Database as ExposedDatabase

private const val POSTGRES_IMAGE = "postgres:17-alpine"
private const val POSTGRES_PORT = 5432
private const val POSTGRES_DRIVER = "org.postgresql.Driver"
private const val TEST_MAXIMUM_POOL_SIZE = 2
private const val TEST_JWT_SECRET = "test-secret-key-for-unit-tests-min-32-characters"

val testConfigModule =
    module {
        single { buildTestConfiguration(createDatabase()) }
    }

val testDbModule =
    module {
        single<ExposedDatabase>(createdAtStart = true) { configureDatabase(get()) }
    }

private val postgres =
    KPostgreSQLContainer(POSTGRES_IMAGE).apply {
        start()
    }

private fun createDatabase(): String {
    val databaseName = "test_${UUID.randomUUID().toString().replace("-", "")}"

    DriverManager
        .getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
        .use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE DATABASE $databaseName")
            }
        }

    return databaseName
}

private fun buildTestConfiguration(databaseName: String) =
    testJdbcUrl(databaseName).let { jdbcUrl ->
        Configuration(
            database =
                Database(
                    jdbcUrl = jdbcUrl,
                    driver = POSTGRES_DRIVER,
                    migrationsLocation = "db/migration/postgres",
                    user = postgres.username,
                    password = postgres.password,
                    maximumPoolSize = TEST_MAXIMUM_POOL_SIZE,
                ),
            jwt =
                JwtConfig(
                    secret = TEST_JWT_SECRET,
                    issuer = "pcast-api",
                    audience = "pcast-api",
                ),
        )
    }

private fun testJdbcUrl(databaseName: String) =
    "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(POSTGRES_PORT)}/$databaseName"

private class KPostgreSQLContainer(
    imageName: String,
) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
