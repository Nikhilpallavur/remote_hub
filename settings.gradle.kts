pluginManagement {
    val isWrapperBootstrap = gradle.startParameter.taskNames.any { taskName ->
        taskName == "wrapper" || taskName.endsWith(":wrapper")
    }

    if (!isWrapperBootstrap) {
        includeBuild("build-logic")
    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RemoteHub"

val isWrapperBootstrap = gradle.startParameter.taskNames.any { taskName ->
    taskName == "wrapper" || taskName.endsWith(":wrapper")
}

if (!isWrapperBootstrap) {
    include(":app")

    include(":core:model")
    include(":core:common")
    include(":core:designsystem")
    include(":core:drivers")
    include(":core:transport")
    include(":core:data")

    include(":feature:remote")

    include(":device:tv")
    include(":device:ir")
    include(":device:ac")
}
