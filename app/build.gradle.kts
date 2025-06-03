plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("io.gitlab.arturbosch.detekt")
    id("org.owasp.dependencycheck")
    id("jacoco")
}

android {
    namespace = "com.example.secureapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.secureapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Best practice: Enable StrictMode in debug builds
        buildConfigField("boolean", "ENABLE_STRICT_MODE", "true")
    }
    
    signingConfigs {
        create("release") {
            // These will be provided by CI/CD pipeline
            storeFile = file("../keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Security: Disable logging in release builds
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        
        // Security: Enable explicit-API mode for better API documentation
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xexplicit-api=strict")
        }
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        
        // Enable coverage for tests
        unitTests.all {
            it.testLogging {
                events("passed", "skipped", "failed")
            }
            it.extensions.configure(JacocoTaskExtension::class) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
    
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // Treat security issues as errors
        error += listOf(
            "MissingPermission",
            "ExportedContentProvider",
            "ExportedReceiver",
            "ExportedService",
            "SetJavaScriptEnabled",
            "UnsafeDynamicallyLoadedCode",
            "CustomErrors",
            "AllowBackup",
            "SSLCertificateSocketFactoryCreateSocket",
            "SSLCertificateSocketFactoryGetInsecure",
            "WorldReadableFiles",
            "WorldWriteableFiles"
        )
    }
}

// Setup coverage report generation
tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        // Exclude Hilt, DataBinding, etc.
        "**/*_Impl*.*",
        "**/*_Factory*.*",
        "**/*Binding*.*"
    )
    
    val debugTree = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(files("${project.buildDir}/jacoco/testDebugUnitTest.exec"))
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Security libraries
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.crypto.tink:tink-android:1.10.0")
    
    // Certificate pinning for network requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Secure data storage with encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.1.5")
    
    // Android testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    
    // Security scanning
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.1")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.1")
}
