rootProject.name = "api"

plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.27"
}

gitHooks {
    commitMsg { conventionalCommits() }
    preCommit {
        tasks("spotlessApply")
    }
    createHooks()
}
