package io.pcast.helpers

import io.viascom.nanoid.NanoId

const val NANO_ID_LENGTH = 18
private const val NANO_ID_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

fun generateNanoId(): String = NanoId.generate(NANO_ID_LENGTH, alphabet = NANO_ID_ALPHABET)
