package io.pcast.helpers

import java.security.SecureRandom

private val SECURE_RANDOM = SecureRandom()

private fun loadWordlist(resource: String): List<String> =
    object {}::class.java
        .getResourceAsStream(resource)
        ?.bufferedReader()
        ?.readLines()
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: error("Wordlist resource not found: $resource")

private val ADJECTIVES: List<String> by lazy { loadWordlist("/wordlist/adjectives.txt") }
private val NOUNS: List<String> by lazy { loadWordlist("/wordlist/nouns.txt") }

private fun pickRandom(list: List<String>): String = list[SECURE_RANDOM.nextInt(list.size)]

private fun randomWord(): String = if (SECURE_RANDOM.nextBoolean()) pickRandom(ADJECTIVES) else pickRandom(NOUNS)

fun generateFriendlyId(words: Int): String = List(words) { randomWord() }.joinToString("-")
