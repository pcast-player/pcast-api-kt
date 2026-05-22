package io.pcast.module.sync

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import io.pcast.extensions.humanReadableWords
import io.pcast.helpers.generateFriendlyId
import org.koin.core.annotation.Single

@Single
class SyncService {
    /**
     * Generates a new 24-word BIP-39 mnemonic phrase.
     *
     * TODO: This endpoint is temporary and will be removed.
     *       Key material should be generated on the client; the server
     *       only derives an encryption key from a phrase supplied by the client.
     *       Never log the returned phrase or include it in stored data.
     */
    fun createSyncPhrase(): String =
        Mnemonics
            .MnemonicCode(Mnemonics.WordCount.COUNT_24)
            .humanReadableWords()

    /**
     * Derives a 64-byte encryption seed from [syncPhrase] using BIP-39 PBKDF2
     * and immediately discards the result. Returns true if the phrase is valid.
     *
     * The derived seed is NOT returned to the caller; it exists only to confirm
     * the phrase is well-formed before the client uses it locally.
     */
    fun validateSyncPhrase(syncPhrase: CharArray): Boolean =
        runCatching {
            val seed = Mnemonics.MnemonicCode(syncPhrase).toSeed()
            seed.fill(0) // zero out seed bytes from heap immediately
            true
        }.getOrElse { false }

    fun createFriendlyId() = generateFriendlyId(4)
}
