package io.pcast.service.sync

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import io.pcast.extensions.humanReadableWords
import io.pcast.helpers.generateFriendlyId

class SyncService {
    fun createSyncPhrase() =
        Mnemonics
            .MnemonicCode(Mnemonics.WordCount.COUNT_24)
            .humanReadableWords()

    fun getSeedFromSyncPhrase(syncPhrase: CharArray) = Mnemonics.MnemonicCode(syncPhrase).toSeed()

    fun createFriendlyId() = generateFriendlyId(4)
}
