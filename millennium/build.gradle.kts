/*
 * Millennium design system, vendored from https://github.com/team-spotube/millennium
 * (AGPL-3.0-or-later, commit c2f18c6, published upstream as org.spotube.millennium:millennium:0.1.1).
 *
 * Vendored so this fork can change the component internals its theme cannot reach (the hover
 * "interaction lighting" gloss). Upstream tests are not carried over.
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = "org.spotube.millennium"
version = "0.1.1-spotube"

kotlin {
    androidLibrary {
        namespace = "org.spotube.millennium"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Exported (api) so the app can use Compose Unstyled directly alongside Millennium.
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            implementation(libs.compose.uiToolingPreview)
            api(libs.composeunstyled)
            api(libs.composeunstyled.theming)
            api(libs.composeunstyled.primitives)
            api(libs.composeunstyled.colored.indication)
        }
    }
}
