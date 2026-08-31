pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }

        // Repositorio Maven do SmartPOS SDK da PayTime.
        // Credenciais ficam em local.properties (gitignored), nunca no repositorio.
        val paytimeProps = java.util.Properties().apply {
            val f = java.io.File(rootDir, "local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val paytimeUrl = paytimeProps.getProperty("paytimeDebugMavenUrl")
        val paytimeUser = paytimeProps.getProperty("paytimeDebugMavenUser")
        val paytimePass = paytimeProps.getProperty("paytimeDebugMavenPassword")
        if (!paytimeUrl.isNullOrBlank() && !paytimeUser.isNullOrBlank() && !paytimePass.isNullOrBlank()) {
            maven {
                url = uri(paytimeUrl)
                credentials {
                    username = paytimeUser
                    password = paytimePass
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }
}

rootProject.name = "PDVMaquineta"
include(":app")
