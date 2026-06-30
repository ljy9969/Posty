plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

kotlin {
    android {
        namespace = "com.bimatrix.posty.shared"
        compileSdk = 35
        minSdk = 26
    }

    // Compose Multiplatform 1.11.x 는 iosArm64(기기) + iosSimulatorArm64(애플실리콘 시뮬)만 발행.
    // (iosX64/인텔 시뮬은 더 이상 제공되지 않음)
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            // material-icons-extended 는 1.7.3 이 마지막 멀티플랫폼 발행본(iOS 포함).
            // CMP 1.11 런타임과 함께 써도 ImageVector API 가 안정적이라 호환된다.
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        }
        androidMain.dependencies {
            implementation("androidx.datastore:datastore-preferences:1.1.1")
            // Android 진입점(setContent) — 앱 모듈 대신 shared 가 Compose 를 호스팅.
            implementation("androidx.activity:activity-compose:1.9.3")
        }
    }
}
