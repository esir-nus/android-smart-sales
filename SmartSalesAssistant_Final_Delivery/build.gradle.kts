// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.serialization) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

// Apply detekt and ktlint to all Kotlin Android modules
subprojects {
    // Apply to Android modules
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        
        // Detekt configuration
        afterEvaluate {
            configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
                baseline = file("$projectDir/detekt-baseline.xml")
                buildUponDefaultConfig = true
                
                // Disable type resolution as requested
                // (type resolution is disabled by default when not explicitly enabled)
                
                config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            }
        }
        
        // Ktlint configuration
        afterEvaluate {
            configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
                version = "1.1.1"  // Latest stable version compatible with plugin 12.1.1
                verbose = true
                android = true
                outputToConsole = true
                ignoreFailures = false
                
                // Use .editorconfig for configuration
                filter {
                    exclude("**/generated/**")
                    exclude("**/build/**")
                }
            }
        }
        
        // Configure detekt tasks
        tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            reports {
                html.required.set(true)
                xml.required.set(true)
                txt.required.set(true)
                sarif.required.set(true)
            }
        }
    }
}

// Register quality gate task
tasks.register("qualityGates") {
    group = "verification"
    description = "Run all quality checks (detekt, ktlint, lint)"
    
    // Add detekt tasks for all modules that have it applied
    subprojects.forEach { subproject ->
        subproject.pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
            dependsOn(":${subproject.name}:detekt")
        }
        subproject.pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
            dependsOn(":${subproject.name}:ktlintCheck")
        }
        subproject.pluginManager.withPlugin("com.android.application") {
            dependsOn(":${subproject.name}:lintDebug")
        }
        subproject.pluginManager.withPlugin("com.android.library") {
            dependsOn(":${subproject.name}:lintDebug")
        }
    }
}

// CI-friendly task that runs all checks and build
tasks.register("ciChecks") {
    group = "verification"
    description = "Run all CI checks (quality gates + assemble)"
    
    dependsOn("qualityGates")
    
    // Add assemble tasks for all Android modules
    subprojects.forEach { subproject ->
        subproject.pluginManager.withPlugin("com.android.application") {
            dependsOn(":${subproject.name}:assembleDebug")
        }
        subproject.pluginManager.withPlugin("com.android.library") {
            dependsOn(":${subproject.name}:assembleDebug")
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
