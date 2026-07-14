# Build & Release Runbook

## Toolchain (verified working)

| Tool | Version |
|---|---|
| JDK | Temurin 17 (17.0.19) |
| Gradle | 8.7 (via wrapper) |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24 |
| compileSdk / targetSdk | 34 |
| minSdk | 21 |

Set `JAVA_HOME` to the JDK 17 install before invoking the wrapper. On this machine:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
```

## Common tasks

```powershell
# Unit tests (parser = security boundary)
.\gradlew.bat :app:testDebugUnitTest --console=plain

# Debug APK  → app/build/outputs/apk/debug/
.\gradlew.bat :app:assembleDebug

# Release APK (R8 + shrink) → app/build/outputs/apk/release/
.\gradlew.bat :app:assembleRelease

# Release App Bundle for Play → app/build/outputs/bundle/release/
.\gradlew.bat :app:bundleRelease
```

## Signing a release

1. Create a keystore (once):
   ```powershell
   & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore orubar-release.jks `
     -keyalg RSA -keysize 2048 -validity 10000 -alias orubar
   ```
2. Copy `keystore.properties.template` → `keystore.properties` and fill in the
   passwords/alias. Both the `.jks` and `keystore.properties` are git-ignored.
3. `assembleRelease` / `bundleRelease` will now sign automatically.

## Verify the trust claims before publishing

```powershell
# 1. Manifest must show CAMERA only and NO internet permission.
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\aapt.exe" dump permissions `
  app\build\outputs\apk\release\app-release.apk

# 2. No URL/host strings should exist in the APK (zero-network proof).
#    (decompile / strings scan — see Security self-audit in blueprint §8)

# 3. Publish the release APK SHA-256 next to the download.
Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
```

## APK size budget

Target < 4 MB (blueprint §5.1). Release build uses R8 + `shrinkResources` and
`resourceConfigurations` limited to en/ta/kn/hi. Check with:

```powershell
(Get-Item app\build\outputs\apk\release\app-release.apk).Length / 1MB
```
