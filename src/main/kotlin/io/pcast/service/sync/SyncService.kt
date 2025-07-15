package io.pcast.service.sync

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import io.pcast.extensions.humanReadableWords
import io.pcast.helpers.generateFriendlyId
import io.pcast.result.attempt

class SyncService {
    fun createSyncPhrase() =
        attempt {
            Mnemonics
                .MnemonicCode(Mnemonics.WordCount.COUNT_24)
                .humanReadableWords()
        }

    fun getSeedFromSyncPhrase(syncPhrase: CharArray) =
        attempt {
            Mnemonics.MnemonicCode(syncPhrase).toSeed()
        }

    fun createFriendlyId() =
        attempt {
            generateFriendlyId(4)
        }
}
