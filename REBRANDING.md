# Rebranding Guide — Make It Yours

This starter kit ships under the app name **Posty** and the application ID
**`com.unboundapex.posty`**. Before you ship, rename both to your own brand.
Every place that needs a change is listed below — nothing else is branded.

## 1. Quick start (application ID only)

The fastest path: keep the Kotlin package `com.unboundapex.posty` internally and
change only the shipped identifiers. Stores only care about the application ID /
bundle ID, not internal package names.

| File | What to change |
|---|---|
| `app/build.gradle.kts` | `applicationId = "com.yourbrand.yourapp"` |
| `app/src/main/res/values/strings.xml` | `<string name="app_name">YourApp</string>` |
| `iosApp/project.yml` | `bundleIdPrefix`, both `PRODUCT_BUNDLE_IDENTIFIER` values, both `CFBundleDisplayName` values |
| `iosApp/iosApp/iosApp.entitlements` | App Group → `group.com.yourbrand.yourapp` |
| `iosApp/PostyWidget/PostyWidget.entitlements` | App Group → `group.com.yourbrand.yourapp` |
| `iosApp/PostyWidget/PostyWidget.swift` | `appGroup` constant → `group.com.yourbrand.yourapp` |
| `shared/src/iosMain/.../data/IosPostyStore.kt` | `suiteName` → `group.com.yourbrand.yourapp` |

> **The three iOS App Group values must match exactly** (both entitlements, the
> Swift constant, and the Kotlin `suiteName`) or the widget cannot read the
> app's data. Register the same App Group ID in your Apple Developer account.

## 2. Full rename (optional)

If you also want the internal package renamed, use your IDE's refactor
(Android Studio: right-click package `com.unboundapex.posty` → Refactor →
Rename). This updates the `namespace` values in `app/build.gradle.kts` and
`shared/build.gradle.kts` plus all source files. Then rename
`rootProject.name` in `settings.gradle.kts` if you care about the Gradle
project name.

## 3. Signing (Android release)

1. Create your own upload keystore (`keytool -genkeypair ...`).
2. Copy `keystore.properties.example` → `keystore.properties` and fill in your
   paths/passwords. This file is gitignored — never commit it.

## 4. Icons & colors

- Android launcher icons: `app/src/main/res/mipmap-*`
- iOS icons: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`
- Theme palette: `shared/src/commonMain/.../ui/theme/` (single source for both platforms)
