plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "fr.perso.cnewsauto"
    compileSdk = 35
    defaultConfig { applicationId = "fr.perso.cnewsauto"; minSdk = 26; targetSdk = 35; versionCode = 5; versionName = "0.4.1"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions.unitTests.all {
        providers.gradleProperty("rssFixtures").orNull?.let { path -> it.systemProperty("rssFixtures", path) }
    }
}
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
    implementation("androidx.car.app:app:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
