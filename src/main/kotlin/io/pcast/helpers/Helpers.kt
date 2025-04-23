package io.pcast.helpers

import com.fasterxml.uuid.Generators
import java.util.UUID

fun generateUuidV7(): UUID = Generators.timeBasedEpochGenerator().generate()

/**
 * Builds a new [MutableMap] by populating a [MutableMap] using the given [lambda]
 * and returning it as a result.
 *
 * @param lambda A lambda that takes a [MutableMap] receiver and populates it.
 * @return A new [MutableMap] populated by the [lambda].
 */
inline fun <K, V> buildMutableMap(
    lambda: MutableMap<K, V>.() -> Unit
) = mutableMapOf<K, V>().apply(lambda)
