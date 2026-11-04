import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProps = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) propsFile.inputStream().use { load(it) }
}
fun buildConfigString(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "app.just.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.just.companion"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0"
        buildConfigField("String", "SUPABASE_URL", buildConfigString(localProps.getProperty("JUST_SUPABASE_URL", "")))
        buildConfigField("String", "SUPABASE_ANON_KEY", buildConfigString(localProps.getProperty("JUST_SUPABASE_ANON_KEY", "")))
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
