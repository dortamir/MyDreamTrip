# MyDreamTrip

MyDreamTrip is an Android application designed for travel enthusiasts to share and discover dream destinations. Users can post about their favorite travel spots, read about others' experiences, leave comments, and explore destinations with integrated Wikipedia information.

## Features

- **User Authentication**: Secure login and signup using Firebase Authentication
- **Explore Destinations**: Browse travel posts with infinite scrolling using Android Paging Library
- **Post Management**: Create, edit, and delete your own travel posts
- **Comments System**: Engage with other users by commenting on posts
- **Profile View**: View your own posts and manage your profile
- **Offline Support**: Local storage using Room database for offline access
- **Wikipedia Integration**: Get detailed information about destinations from Wikipedia
- **Image Support**: Upload and display images for posts using Picasso
- **Material Design**: Modern UI following Material Design principles

## Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **UI**: Android Fragments with Navigation Component
- **Database**: Room for local storage, Firebase Firestore for remote
- **Authentication**: Firebase Auth
- **Networking**: Retrofit with OkHttp for API calls
- **Image Loading**: Picasso
- **Dependency Injection**: Manual (no DI framework used)
- **Build System**: Gradle with Kotlin DSL

## Dependencies

- AndroidX Libraries (Core, AppCompat, Activity, ConstraintLayout)
- Material Components
- Navigation Component
- Firebase (Auth, Firestore)
- Room Database
- Paging Library
- Picasso
- Retrofit with Gson converter
- OkHttp Logging Interceptor
- JUnit and Espresso for testing

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11
- Android SDK API 36
- Minimum SDK API 26

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd MyDreamTrip
   ```

2. Open the project in Android Studio

3. Add your Firebase configuration:
   - Create a Firebase project at https://console.firebase.google.com/
   - Enable Authentication and Firestore
   - Download `google-services.json` and place it in `app/` directory

4. Build the project:
   ```bash
   ./gradlew build
   ```

5. Run on device or emulator

## Project Structure

```
app/src/main/java/com/example/mydreamtrip/
├── data/
│   ├── local/          # Room database entities and DAOs
│   ├── remote/         # API services (Wikipedia)
│   └── repo/           # Repository classes
├── model/              # Data models (Destination, Comment)
├── ui/                 # UI packages (add, explore)
├── AddFragment.kt      # Fragment for adding posts
├── AuthActivity.kt     # Authentication activity
├── CommentAdapter.kt   # RecyclerView adapter for comments
├── EditPostFragment.kt # Fragment for editing posts
├── LoginFragment.kt    # Login UI
├── MainActivity.kt     # Main app activity
├── MainFragment.kt     # Container fragment with bottom nav
├── PlaceholderFragment.kt
├── PostDetailsFragment.kt # Post details with comments
├── ProfileFragment.kt  # User profile
├── SignupFragment.kt   # Signup UI
└── WelcomeFragment.kt  # Welcome screen
```

## API Usage

The app integrates with Wikipedia API to fetch destination information:
- Uses Retrofit to make HTTP requests
- Parses JSON responses with Gson
- Displays Wikipedia extracts and images

## Database Schema

### Local Database (Room)
- **PostEntity**: Stores travel posts locally
  - id, title, location, rating, author, image data, Wikipedia info

### Remote Database (Firestore)
- **posts** collection: Stores user posts
- **comments** subcollection: Comments on each post

## Build and Run

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Run Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Screenshots

(Add screenshots here when available)

## Future Enhancements

- Push notifications for new comments
- Location-based destination suggestions
- Social features (following users, liking posts)
- Offline map integration
- Multi-language support</content>
<parameter name="filePath">c:\Users\user\AndroidStudioProjects\MyDreamTrip\README.md