package io.pcast.helpers

import io.github.serpro69.kfaker.Faker

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
        { FAKER.breakingBad.character() },
        { FAKER.dnd.alignments() },
        { FAKER.dnd.cities() },
        { FAKER.dnd.monsters() },
        { FAKER.dnd.klasses() },
        { FAKER.dnd.races() },
        { FAKER.dnd.meleeWeapons() },
        { FAKER.dnd.meleeWeapons() },
        { FAKER.color.name() },
        { FAKER.futurama.characters() },
        { FAKER.simpsons.characters() },
    )

private fun getRandomWord() = RANDOM_WORD_FNS.random()()

private fun String.slugify() =
    lowercase()
        .replace(Regex("\\s"), "-")
        .replace(Regex("[^a-z-]"), "")
        .replace(Regex("-+"), "-")

fun generateFriendlyId(words: Int) = List(words) { getRandomWord() }.joinToString("-").slugify()
