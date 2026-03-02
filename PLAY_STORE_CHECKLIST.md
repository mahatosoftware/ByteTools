# ETools Pro - Play Store Release Checklist

## 📦 App Bundle
- [ ] Create a release build variant.
- [ ] Sign the APK/Bundle with a production keystore.
- [ ] Ensure `versionCode` and `versionName` are correctly updated in `build.gradle.kts`.
- [ ] Enable ProGuard/R8 minification and check for any broken code.

## 📄 Store Listing
- [ ] Prepare high-quality app icon (512x512).
- [ ] Create feature graphic (1024x500).
- [ ] Take screenshots for phone, 7-inch tablet, and 10-inch tablet.
- [ ] Write a compelling App Description and Short Description.
- [ ] Define the App Category (Productivity / Utilities).

## 🔐 Legal & Privacy
- [ ] Generate a Privacy Policy and host it on a public URL.
- [ ] Disclose any data collection (ML Kit, AdMob).
- [ ] Complete the Data Safety section in Play Console.

## 💰 Monetization
- [ ] Link AdMob app to the Play Store listing.
- [ ] Verify Google Play Billing integration in a closed testing track.

## 🧪 Testing
- [ ] Run internal testing with a few users.
- [ ] Perform a full sanity check on all tools (QR Scanner, Unit Converter, etc.).
- [ ] Test on multiple Android versions (Min SDK 24 to latest).
