# Where's My Money 💸

A modern, offline-first Android expense tracking app built with Jetpack Compose, Material 3 retro pixel aesthetics, Room Database, Firebase Authentication (Google Sign-In via Credential Manager), and Cloud Firestore.

---

## Features ✨

- **Onboarding Flow**: Quick personalization on first launch with user name entry and optional Google Sign-In.
- **Cross-Device Cloud Sync**: Real-time multi-device sync powered by Firebase Auth + Cloud Firestore.
- **Offline-First Storage**: Room Database handles fast local reads/writes, queuing sync updates when offline with collision-free client ID generation (`IdGenerator`).
- **Category & Expense Tracker**: Hierarchical parent and sub-categories (e.g., Fixed vs. Flexi expenses) with custom color tagging.
- **History & Analytics**: Monthly expense breakdown, category totals, and expense management.
- **Account Settings**: Integrated account management card supporting profile view, sign-out, and account deletion.
- **Pixel Theme**: Custom retro pixel design system with support for Light, Dark, and System themes.

---

## Tech Stack 🛠️

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose, Material 3, Navigation Compose
- **Architecture**: MVVM with `AppContainer` Dependency Injection
- **Local Database**: Room Database (with V1 to V2 schema migration)
- **Preferences**: DataStore Preferences
- **Authentication**: Firebase Auth & Android Credential Manager API (`com.google.android.libraries.identity.googleid`)
- **Cloud Database**: Cloud Firestore
- **Image Loading**: Coil Compose
- **Concurrency**: Kotlin Coroutines & Flow

---

## Project Setup & Firebase Configuration 🚀

### 1. Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35+ (target SDK 37)

### 2. Firebase & Google Sign-In Setup
To enable Google Sign-In and Cloud Sync:

1. Create a project on the [Firebase Console](https://console.firebase.google.com).
2. Add an Android app with package name `com.smish.wheresmymoney`.
3. Register your debug SHA-1 fingerprint (run `./gradlew signingReport` or keytool).
4. Download `google-services.json` and place it in the `app/` directory (`app/google-services.json`).
5. In **Authentication** → **Sign-in method**, enable **Google**.
6. In **Cloud Firestore**, create a database and paste these security rules:
   ```text
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

---

## Building the App ⚙️

```bash
# Clone repository
git clone https://github.com/smish-hash/wheres-my-money.git
cd wheres-my-money

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```
