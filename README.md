# Dhobi Tracker 🧺

A native, production-quality Android utility application built with Kotlin and Jetpack Compose to reliably record laundry bags given to local dhobis (dry cleaners/washers), preserve photos of handwritten paper chits, extract quantities via on-device OCR, track returned items, and share formatted inventory summaries through WhatsApp.

---

## 🌟 Key Features

### 1. Dashboard & Status Summaries
- **Clean Banking-Inspired Visuals**: Warm off-white background, elegant white cards with generous rounded corners (18-20dp), subtle borders, modern typography, and refined status pills.
- **Real-Time Database Statistics**:
  - **Bags at Dhobi**: Current active bags handed over.
  - **Items Awaiting Return**: Real-time count of outstanding clothing garments.
  - **Partially Returned Bags**: Bags where some items have been returned while others are still pending.
  - **Collected Bags**: Completed laundry records.
- **Recent Bags**: Quick access to recent transactions with thumbnail previews of attached paper chits.

### 2. Fast One-Handed Laundry Entry
- **Auto-Generated Internal ID**: Clean identifier (e.g. `DT-261020-001`).
- **Flexible Metadata**: Date given (defaults to today), optional handwritten slip number, optional dhobi/shop name, optional amount (₹), expected return date, and custom notes.
- **14 Pre-Loaded Indian Clothing Categories**:
  1. Kurta
  2. Pajama
  3. Shirt
  4. T-Shirt
  5. Pant
  6. Lower
  7. Shorts
  8. Bedsheet
  9. Pillow Cover
  10. Towel
  11. Duppata
  12. Under Garments
  13. Socks
  14. Hankey
- **Custom Categories**: Add any custom garment (e.g., Blanket, Scarf, Blazer) on the fly.
- **Accessible Quantity Controls**: Fast increment/decrement steppers (`+`/`-`) plus a direct-input dialog to type quantities directly.
- **Instant Reactive Totals**: Live item count updates immediately. Zero-count items do not clutter final records.
- **Unsaved Changes Guard**: Prevents accidental discarding with a confirmation dialog.

### 3. Durable Paper Chit Photo Storage
- **Camera & Photo Picker**: Capture new photos using the phone camera or select existing photos from the gallery.
- **EXIF Rotation Correction**: Automatically corrects image orientation so captured photos never appear sideways or upside down.
- **Durable App-Controlled Storage**: Stored in the app's private sandbox directory (`files/chits/`), completely immune to temporary cache purges.
- **"Show Chit to Dhobi" Full-Screen View**: One-tap full-screen viewer with pinch-to-zoom and pan, designed specifically for showing the paper chit proof to the dhobi.

### 4. Automatic On-Device OCR Recognition (ML Kit)
- **100% Offline OCR**: Powered by Google ML Kit on-device text recognition. Runs locally on the phone without sending photos to cloud servers.
- **Smart Data Extraction**: Identifies slip numbers, dates, matching clothing category names, and adjacent quantities.
- **Total Mismatch Detection**: Compares the sum of detected clothing items against the recognized chit total (e.g., "Total: 10"). Warns the user if there is a discrepancy.
- **Editable Confirmation Flow**: Extracted values are presented in a dedicated verification screen before applying them to the record.
- **Graceful Fallback**: If OCR confidence is low or handwriting is ambiguous, the app never blocks the workflow and defaults to manual entry.

### 5. Collection & Missing-Item Tracking
- **Category-by-Category Return Counter**: Track individual returned quantities per garment.
- **Boundary Protection**: Prevents marked returns from exceeding originally handed-over quantities.
- **Three Clear States**:
  - **Pending**: None of the items returned.
  - **Partially Returned**: Some garments returned, some outstanding.
  - **Collected**: All items returned or explicitly confirmed.
- **Discrepancy Preservation**: If the user marks a bag as collected despite missing clothes (e.g., dhobi lost a shirt), the app warns the user and preserves the missing item record.
- **Audit Integrity**: Updating collection progress never overwrites the original inventory handed over.

### 6. WhatsApp Formatting & Native Sharing
- Formats confirmed inventories into a clean, human-readable WhatsApp message:
  ```text
  🧺 Laundry Bag: DT-0001
  📅 Date: 20 October 2026
  🔢 Slip No.: 12345
  🏪 Shop/Dhobi: Raju Dhobi

  Clothing Inventory:
  • Shirt: 2
  • T-Shirt: 3
  • Pant: 1
  • Towel: 1

  Total: 7 items

  Please confirm the count. Thank you!
  ```
- Directly attaches the paper chit photo via `FileProvider`.
- Falls back seamlessly to the Android native share sheet if WhatsApp is not installed.

### 7. Safe Local Backup & Restore
- **Storage Access Framework (SAF)**: Export a portable `.zip` backup containing structured JSON databases (`records.json`, `categories.json`, `metadata.json`) and all referenced high-resolution chit images.
- **Atomic Import**: Validates backup contents, avoids ID collision, protects against Zip Slip directory traversal vulnerabilities, and restores full records and images.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0 (Kotlin 2.0.21)
- **UI Toolkit**: Jetpack Compose with Material 3 Design Tokens
- **Architecture**: MVVM with Repository Pattern and Reactive Flows
- **Local Persistence**: Android Room Database (`2.6.1`) with KSP code generator
- **Image Pipeline**: Android Photo Picker, CameraX / Android Camera Intent, Coil Compose (`2.7.0`), ExifInterface
- **OCR Engine**: Google ML Kit On-Device Text Recognition (`16.0.1`)
- **Navigation**: Jetpack Navigation Compose (`2.8.3`)
- **Concurrency**: Kotlin Coroutines & StateFlow/SharedFlow
- **Serialization**: Google Gson for backup archive generation

---

## 📂 Project Structure

```
DhobiGO/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/dhobigo/tracker/
│       │   │   ├── DhobiApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── dao/
│       │   │   │   │   ├── database/
│       │   │   │   │   ├── entity/
│       │   │   │   │   └── model/
│       │   │   │   ├── storage/ImageStorageManager.kt
│       │   │   │   ├── ocr/TextRecognitionManager.kt
│       │   │   │   ├── backup/BackupRestoreManager.kt
│       │   │   │   └── repository/LaundryRepository.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   ├── components/
│       │   │   │   ├── screens/
│       │   │   │   │   ├── home/
│       │   │   │   │   ├── add/
│       │   │   │   │   ├── ocr/
│       │   │   │   │   ├── details/
│       │   │   │   │   ├── history/
│       │   │   │   │   ├── collection/
│       │   │   │   │   ├── fullscreen/
│       │   │   │   │   └── settings/
│       │   │   │   └── navigation/
│       │   │   └── util/
│       │   │       ├── DateUtils.kt
│       │   │       └── WhatsAppShareHelper.kt
│       │   └── res/
│       └── test/
│           └── java/com/dhobigo/tracker/
│               ├── LaundryRecordWithItemsTest.kt
│               └── WhatsAppShareHelperTest.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

---

## 🚀 How to Run in Android Studio

1. Open **Android Studio** (Koala / Ladybug / Meerkat or later).
2. Select **Open an Existing Project** and choose the `DhobiGO` folder.
3. Allow Gradle to perform an initial sync.
4. Select an Android emulator or connected device (Android 8.0 / API 26 or higher).
5. Click **Run 'app'** (`Shift + F10`).
