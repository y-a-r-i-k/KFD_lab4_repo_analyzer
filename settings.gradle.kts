pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "repo-analyzer"

// :core — обход, подсчёт и сортировка; :cli — точка входа и вывод.
include(":core", ":cli")
