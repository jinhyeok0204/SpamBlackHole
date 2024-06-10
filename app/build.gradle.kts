plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.spamblackhole2"
    compileSdk = 34

    packaging{
        resources{
            excludes += setOf("META-INF/DEPENDENCIES")
        }
    }
    defaultConfig {
        applicationId = "com.example.spamblackhole2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures{
        viewBinding = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.9.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.3.1")


    // RecyclerVIew
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation ("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("com.google.api-client:google-api-client-android:1.32.1")
    implementation ("com.google.api-client:google-api-client-gson:1.32.1")
    implementation ("com.google.apis:google-api-services-gmail:v1-rev110-1.25.0")

    // Google API Client Libraries for AndroidHttp
    implementation("com.google.http-client:google-http-client-android:1.41.5")

    // CardView
    implementation ("androidx.cardview:cardview:1.0.0")

    // DrawerLayout and NavigationView dependencies
    implementation ("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation ("com.google.android.material:material:1.4.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}