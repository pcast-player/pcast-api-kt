package io.pcast.config

data class Database(
    val jdbcUrl: String,
    val driver: String,
    val migrationsLocation: String,
    val user: String,
    val password: String? = null,
    val maximumPoolSize: Int = 10,
)
