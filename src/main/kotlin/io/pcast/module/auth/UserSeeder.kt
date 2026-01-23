package io.pcast.module.auth

import io.pcast.module.auth.model.UserRepository

/**
 * Utility for seeding users into the database.
 * Since there's no registration endpoint, use this to create users.
 *
 * Example usage in Application or a separate main:
 * ```
 * val seeder = UserSeeder(authService, userRepository)
 * seeder.seedDefaultUsers()
 * ```
 */
class UserSeeder(
    private val authService: AuthService,
    private val userRepository: UserRepository,
) {
    /**
     * Seeds a user if they don't already exist.
     * @return true if user was created, false if already exists
     */
    fun seedUser(
        email: String,
        password: String,
    ): Boolean {
        if (userRepository.findByEmail(email) != null) {
            return false
        }

        authService.createUser(email, password)
        return true
    }

    /**
     * Seeds multiple users from a list of email/password pairs.
     * Skips users that already exist.
     * @return count of users created
     */
    fun seedUsers(users: List<Pair<String, String>>): Int =
        users.count { (email, password) -> seedUser(email, password) }

    /**
     * Seeds default development users.
     * Only use this in development/testing environments.
     */
    fun seedDefaultUsers(): Int =
        seedUsers(
            listOf(
                "admin@example.com" to "admin123",
                "user@example.com" to "user123",
                "test@example.com" to "test123",
            ),
        )
}
