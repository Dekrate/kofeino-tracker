# Keystore

This directory contains the release signing keystore and credentials.

## IMPORTANT
- `release.keystore` — generated certificate, **never commit to git**
- `keystore.properties` — passwords and alias, **never commit to git**
- Both are excluded via `.gitignore`

## Setup
1. Change the placeholder passwords in `keystore.properties` to secure values:
   ```
   storePassword=YOUR_SECURE_STORE_PASSWORD
   keyPassword=YOUR_SECURE_KEY_PASSWORD
   ```

2. To generate a new keystore:
   ```bash
   keytool -genkey -v -keystore keystore/release.keystore \
     -alias YOUR_ALIAS -keyalg RSA -keysize 4096 -validity 10000
   ```

3. For CI (GitHub Actions), add these secrets:
   - `KEYSTORE_BASE64` — base64 encoded keystore
   - `KEYSTORE_PASSWORD` — store password
   - `KEY_ALIAS` — key alias
   - `KEY_PASSWORD` — key password

## Security Notes
- The keystore is RSA 4096-bit with 10000-day validity (~27 years)
- `build.gradle.kts` reads credentials from `keystore.properties` (only if it exists)
- Release signing fails at build time if credentials are missing/empty
