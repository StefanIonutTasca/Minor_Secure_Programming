import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
    id("org.owasp.dependencycheck") version "8.2.1"
    id("org.sonarqube") version "4.2.1.3168"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config = files("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON", "XML")
    suppressionFile = "$projectDir/config/owasp/suppressions.xml"
    analyzers {
        assemblyEnabled = false
        nodeEnabled = false
    }
}

sonarqube {
    properties {
        property("sonar.projectName", "Secure Kotlin Mobile App")
        property("sonar.projectKey", "secure-kotlin-app")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "your-organization")
        property("sonar.qualitygate.wait", "true")
        
        // Security-specific properties
        property("sonar.security.sources.exclusions", "**/*.kt")
        property("sonar.kotlin.detekt.reportPaths", "build/reports/detekt/detekt.xml")
        property("sonar.dependencyCheck.reportPath", "build/reports/dependency-check-report.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
            // Enable compiler warnings as errors for more secure code
            allWarningsAsErrors = true
            // Enable explicit API mode for better API documentation
            freeCompilerArgs = freeCompilerArgs + listOf("-Xexplicit-api=strict")
        }
    }
    
    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.1")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.1")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.1")
    }
}
