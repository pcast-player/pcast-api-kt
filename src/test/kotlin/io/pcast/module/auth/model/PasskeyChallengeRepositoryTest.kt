package io.pcast.module.auth.model

import io.pcast.module.AppModule
import io.pcast.module.testConfigModule
import io.pcast.module.testDbModule
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val REQUEST_JSON = """{"publicKey":{"challenge":"test"}}"""
private const val CONCURRENT_ATTEMPTS = 8

internal class PasskeyChallengeRepositoryTest {
    @Test
    fun testConsumeReturnsChallengeOnce() {
        withRepository { _, repository ->
            val challenge = "consume-once-${UUID.randomUUID()}"

            repository.create(
                userId = null,
                type = PasskeyChallengeType.Authentication,
                challenge = challenge,
                requestJson = REQUEST_JSON,
                expiresAt = LocalDateTime.now().plusMinutes(5),
            )

            val consumed = repository.consume(challenge, PasskeyChallengeType.Authentication)
            val replay = repository.consume(challenge, PasskeyChallengeType.Authentication)

            assertNotNull(consumed)
            assertEquals(REQUEST_JSON, consumed.requestJson)
            assertNull(replay)
        }
    }

    @Test
    fun testConsumeRejectsExpiredChallenge() {
        withRepository { _, repository ->
            val challenge = "expired-${UUID.randomUUID()}"

            repository.create(
                userId = null,
                type = PasskeyChallengeType.Authentication,
                challenge = challenge,
                requestJson = REQUEST_JSON,
                expiresAt = LocalDateTime.now().minusMinutes(1),
            )

            assertNull(repository.consume(challenge, PasskeyChallengeType.Authentication))
        }
    }

    @Test
    fun testConcurrentConsumeOnlySucceedsOnce() {
        withRepository { _, repository ->
            val challenge = "concurrent-${UUID.randomUUID()}"

            repository.create(
                userId = null,
                type = PasskeyChallengeType.Authentication,
                challenge = challenge,
                requestJson = REQUEST_JSON,
                expiresAt = LocalDateTime.now().plusMinutes(5),
            )

            val executor = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS)
            val ready = CountDownLatch(CONCURRENT_ATTEMPTS)
            val start = CountDownLatch(1)

            val futures =
                (1..CONCURRENT_ATTEMPTS).map {
                    executor.submit<PasskeyChallenge?> {
                        ready.countDown()
                        start.await()
                        repository.consume(challenge, PasskeyChallengeType.Authentication)
                    }
                }

            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()

            val results = futures.map { it.get(5, TimeUnit.SECONDS) }
            executor.shutdown()

            assertEquals(1, results.count { it != null })
        }
    }

    @Test
    fun testCreatePurgesExpiredAndConsumedChallenges() {
        withRepository { db, repository ->
            val consumedChallenge = "consumed-${UUID.randomUUID()}"

            repository.create(
                userId = null,
                type = PasskeyChallengeType.Authentication,
                challenge = "expired-${UUID.randomUUID()}",
                requestJson = REQUEST_JSON,
                expiresAt = LocalDateTime.now().minusMinutes(1),
            )
            repository.create(
                userId = null,
                type = PasskeyChallengeType.Authentication,
                challenge = consumedChallenge,
                requestJson = REQUEST_JSON,
                expiresAt = LocalDateTime.now().plusMinutes(5),
            )
            repository.consume(consumedChallenge, PasskeyChallengeType.Authentication)
            repository.create(
                userId = null,
                type = PasskeyChallengeType.Authentication,
                challenge = "active-${UUID.randomUUID()}",
                requestJson = REQUEST_JSON,
                expiresAt = LocalDateTime.now().plusMinutes(5),
            )

            val remainingRows =
                transaction(db) {
                    PasskeyChallengesTable
                        .selectAll()
                        .map { row ->
                            row[PasskeyChallengesTable.expiresAt] to row[PasskeyChallengesTable.consumedAt]
                        }
                }

            assertEquals(1, remainingRows.size)
            assertTrue(remainingRows.single().first.isAfter(LocalDateTime.now()))
            assertNull(remainingRows.single().second)
        }
    }

    private fun withRepository(block: (Database, PasskeyChallengeRepository) -> Unit) {
        val koinApplication =
            koinApplication {
                modules(testConfigModule, testDbModule, AppModule().module)
            }

        try {
            block(
                koinApplication.koin.get(),
                koinApplication.koin.get(),
            )
        } finally {
            koinApplication.close()
        }
    }
}
