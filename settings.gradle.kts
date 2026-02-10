rootProject.name = "api"

plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.9"

}

gitHooks {
    commitMsg { conventionalCommits() }
    preCommit {
        tasks("detekt", "spotlessCheck")
    }
    createHooks(true)
}
