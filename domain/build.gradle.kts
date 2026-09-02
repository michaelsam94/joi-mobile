// Pure Kotlin module — no Android/Retrofit/Compose dependency allowed here.
// Mirrors the backend's domain/ + application/ layers: entities, repository interfaces
// ("ports" in the backend's terminology), use-cases, and the session contract.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}

kotlin {
    jvmToolchain(17)
}
