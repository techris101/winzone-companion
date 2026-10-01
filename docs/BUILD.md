# WIN ZONE Companion App — Build & Release Documentation

## 1. Prerequisites
- **Java Development Kit:** OpenJDK 17 (`temurin` 17 recommended)
- **Android SDK:** Compile SDK 34, Min SDK 26
- **Gradle:** 8.7 (managed via included Gradle Wrapper `./gradlew`)
- **Android Gradle Plugin (AGP):** 8.4.0

---

## 2. Environment Variables & Properties

Create `local.properties` in the project root (never commit real credentials to VCS):

```properties
SUPABASE_URL=https://jfniylmbogodgozcczln.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_ZyP4LV6xjYu8T4tyPAgWFw_IQEPiVui
```

### Keystore Environment Variables (for Release Signing)
- `WINZONE_KEYSTORE_PASSWORD` — password for the keystore
- `WINZONE_KEY_ALIAS` — alias name (e.g. `winzone`)
- `WINZONE_KEY_PASSWORD` — password for the private key

---

## 3. Local Commands

### Assemble Debug APK
```bash
./gradlew :app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew :app:testDebugUnitTest
```

### Run Android Lint
```bash
./gradlew :app:lintDebug
```

### Assemble Release APK
```bash
./gradlew :app:assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

---

## 4. Keystore Generation
To generate a production signing key:
```bash
keytool -genkeypair -v \
  -keystore keystore/winzone-release.jks \
  -alias winzone \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass <YOUR_PASSWORD> \
  -keypass <YOUR_PASSWORD>
```

---

## 5. Verification
Verify the compiled APK signature:
```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```
