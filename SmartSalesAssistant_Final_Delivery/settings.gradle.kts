pluginManagement {
    repositories {
        maven { url = uri("${System.getProperty("user.home")}/.m2/repository") }
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("${System.getProperty("user.home")}/.m2/repository") }
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/central")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
    }
}

rootProject.name = "SmartSalesAssistant"
include(":app")
include(":aiFeatureTestApp")
