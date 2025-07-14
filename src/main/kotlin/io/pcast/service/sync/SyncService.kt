package io.pcast.service.sync

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import io.github.serpro69.kfaker.Faker
import io.pcast.extensions.humanReadableWords
import io.pcast.result.attempt

private const val WORD_COUNT = 3
private val FAKER = Faker()
private val RANDOM_WORD_FNS =
    listOf<() -> String>(
        { FAKER.animal.name() },
        { FAKER.appliance.equipment() },
        { FAKER.backToTheFuture.characters() },
        { FAKER.bigBangTheory.characters() },
        { FAKER.starTrek.character() },
        { FAKER.starWars.characters() },
        { FAKER.streetFighter.characters() },
        { FAKER.studioGhibli.characters() },
        { FAKER.superMario.characters() },
        { FAKER.superSmashBros.fighter() },
        { FAKER.bird.commonFamilyName() },
        { FAKER.coffee.blendName() },
        { FAKER.greekPhilosophers.names() },
        { FAKER.lordOfTheRings.characters() },
        { FAKER.buffy.characters() },
        { FAKER.gameOfThrones.characters() },
        { FAKER.gameOfThrones.cities() },
        { FAKER.gameOfThrones.dragons() },
        { FAKER.gameOfThrones.houses() },
        { FAKER.harryPotter.characters() },
        { FAKER.harryPotter.houses() },
        { FAKER.harryPotter.spells() },
        { FAKER.fallout.characters() },
        { FAKER.movie.title() },
        { FAKER.drWho.character() },
        { FAKER.programmingLanguage.name() },
        { FAKER.ghostBusters.characters() },
        { FAKER.natoPhoneticAlphabet.codeWord() },
        { FAKER.halfLife.character() },
        { FAKER.worldOfWarcraft.hero() },
        { FAKER.worldOfWarcraft.classNames() },
        { FAKER.worldOfWarcraft.races() },
        { FAKER.spongebob.characters() },
        { FAKER.tea.variety.black() },
        { FAKER.tea.variety.green() },
        { FAKER.tea.variety.herbal() },
        { FAKER.tea.variety.oolong() },
        { FAKER.tea.variety.white() },
        { FAKER.superhero.name() },
        { FAKER.southPark.characters() },
        { FAKER.warhammerFantasy.factions() },
        { FAKER.warhammerFantasy.creatures() },
        { FAKER.warhammerFantasy.heroes() },
        { FAKER.warhammerFantasy.locations() },
        { FAKER.music.bands() },
        { FAKER.music.albums() },
        { FAKER.music.genres() },
    )

private fun getRandomWord() =
    RANDOM_WORD_FNS
        .random()()
        .lowercase()
        .replace(Regex("\\s"), "-")
        .replace(Regex("[^a-z-]"), "")

private fun List<String>.slugify() = joinToString("-").replace(Regex("-+"), "-")

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
            List(WORD_COUNT) { getRandomWord() }.slugify()
        }
}
