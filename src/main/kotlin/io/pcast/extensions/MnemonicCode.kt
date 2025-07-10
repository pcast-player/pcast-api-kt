package io.pcast.extensions

import cash.z.ecc.android.bip39.Mnemonics

fun Mnemonics.MnemonicCode.humanReadableWords() = chars.joinToString("")
