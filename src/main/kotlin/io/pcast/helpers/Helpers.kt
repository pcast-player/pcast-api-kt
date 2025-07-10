package io.pcast.helpers

import com.fasterxml.uuid.Generators
import io.viascom.nanoid.NanoId
import java.util.UUID

const val NANO_ID_LENGTH = 18
private const val NANO_ID_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

fun generateUuidV7(): UUID = Generators.timeBasedEpochGenerator().generate()

fun generateNanoId(): String = NanoId.generate(NANO_ID_LENGTH, alphabet = NANO_ID_ALPHABET)

/**
 * Builds a new [MutableMap] by populating a [MutableMap] using the given [builder]
 * and returning it as a result.
 *
 * @param builder A lambda that takes a [MutableMap] receiver and populates it.
 * @return A new [MutableMap] populated by the [builder].
 */
fun <K, V> buildMutableMap(builder: MutableMap<K, V>.() -> Unit) = mutableMapOf<K, V>().apply(builder)
