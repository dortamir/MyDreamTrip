# MyDreamTrip

A modern Android application for travel enthusiasts to discover, share, and discuss dream travel destinations. Users can post about their favorite locations, view posts from other travelers, leave comments, and explore destinations with integrated Wikipedia information.

## Project Description

MyDreamTrip is a social travel-sharing platform built with modern Android development practices. The app allows users to:
- Create and share their travel experiences and favorite destinations
- Browse posts from other users with infinite scrolling
- View detailed information about destinations, enriched with Wikipedia data
- Engage with the community through comments
- Manage their user profile and travel posts
- Access content offline with local Room database caching

The app combines remote data persistence (Firebase) with local caching (Room) to provide a seamless, efficient user experience while demonstrating best practices in Android architecture and data management.

## Main Features

- **User Authentication**: Secure signup and login using Firebase Authentication
- **Post Management**: Create, edit, and delete travel posts with optional images
- **Browse Destinations**: Explore travel posts from all users with infinite scrolling and gradual loading
- **Post Details**: View detailed post information including Wikipedia summaries and user comments
- **Comments System**: Add and delete comments on travel posts to engage with other users
- **User Profiles**: View and edit personal profile information with profile pictures
- **Image Upload**: Upload images to Firebase Storage when creating or editing posts
- **Wikipedia Integration**: Fetch and display destination information from Wikipedia API
- **Offline Support**: All posts and user data cached locally using Room database
- **Responsive UI**: Material Design components with bottom navigation for easy navigation

## Technologies Used

### Core Android & UI
- **Kotlin**: Modern programming language for Android development
- **AndroidX Libraries**: Latest Android support libraries for compatibility and features
- **Material Design**: Google Material Design components for modern UI
- **Navigation Component**: Fragment-based navigation with SafeArgs for type-safe argument passing

### Authentication & Remote Data
- **Firebase Authentication**: Secure user authentication with email/password
- **Firebase Firestore**: Real-time NoSQL database for storing posts, users, and comments
- **Firebase Storage**: Cloud storage for user-uploaded images

### Local Data & Caching
- **Room (SQLite)**: Local database for caching posts and user data, enabling offline access
- **Shared Preferences**: Lightweight storage for caching profile photo references

### Data Loading & Networking
- **Android Paging 3 Library**: Efficient gradual loading of large post lists with automatic pagination
- **Retrofit**: Type-safe REST client for consuming the Wikipedia API
- **OkHttp**: HTTP client with logging interceptor for debugging network requests
- **Gson**: JSON serialization/deserialization for API responses

### Image Loading
- **Picasso**: Image loading and caching library for efficiently loading and displaying images from URLs and local storage

## Architecture Overview

### Architecture Pattern
The project follows the **MVVM (Model-View-ViewModel)** architecture combined with a **Repository pattern** to provide a clean separation of concerns and improve testability and maintainability.

### Key Components

**Fragments & UI Layer**
- All UI screens are implemented as Fragments for flexibility and reusability
- Fragments: WelcomeFragment, LoginFragment, SignupFragment, AddFragment, EditPostFragment, PostDetailsFragment, ProfileFragment, EditProfileFragment, ExploreFragment
- Activities: AuthActivity (authentication flow), MainActivity (main app container)

**Navigation Graph**
- `nav_graph.xml`: Handles authentication flow (Welcome → Login/Signup → MainActivity)
- `main_graph.xml`: Handles in-app navigation between Explore, Add, and Profile sections
- Uses SafeArgs for type-safe argument passing between fragments

**MVVM Components**
- **AddViewModel**: Manages state for fetching Wikipedia destination information during post creation
- Uses Kotlin Flow for reactive state management

**Repository Layer**
- **PostsRepository**: Manages post data from both Firestore (remote) and Room (local)
  - Syncs Firestore posts to local Room database in real-time
  - Provides Paging flow for gradual loading of posts
  - Handles real-time synchronization of post changes
- **UsersRepository**: Manages user data caching
  - Caches user profiles from Firestore to Room
  - Provides Flow-based user observation for reactive updates
- **WikiRepository**: Manages Wikipedia API integration
  - Fetches destination summaries via Retrofit

**Data Access Layer**
- **Room DAOs** (PostsDao, UsersDao):
  - PostsDao provides PagingSource for Paging support
  - Both DAOs provide Flow-based queries for reactive updates
- **Firestore Integration**: Direct Firestore calls with proper listener management

**Data Models**
- **PostEntity/UserEntity**: Room entities for local caching
- **Destination/Comment**: Domain models used throughout the app

**Image Loading**
- Picasso handles all image loading with built-in memory caching
- Images are stored in Firebase Storage with URLs saved in Firestore
- Local image URIs are preserved for offline access

### Data Flow
1. **Remote Source**: Firebase Firestore stores posts, users, and comments in the cloud
2. **Sync Mechanism**: PostsRepository listens to Firestore changes and syncs to Room in real-time
3. **Local Cache**: Room database stores all posts and user data for offline access
4. **UI Binding**: Fragments observe Room data via Flow, automatically updating when data changes
5. **Pagination**: Explore screen uses Paging library to load posts gradually (10 posts per page)

## Requirement Compliance

This project fully satisfies all nine course requirements:

### 1. Remote Data Storage ✅
- **Implementation**: Firebase Firestore serves as the remote data storage solution
- **Details**: Posts are stored in the `posts` collection with subcollections for comments. User data stored in `users` collection
- **Multi-user Sharing**: When a user creates a post, it's immediately synced to Firestore and becomes visible to all other users in the Explore feed
- **Real-time Sync**: PostsRepository uses Firestore snapshot listeners to keep local cache synchronized with remote changes
- **Files**: PostsRepository.kt, AddFragment.kt

### 2. Local Storage Using Room (SQLite) ✅
- **Implementation**: Room database caches all posts and user data locally
- **Database**: `AppDatabase` defines the Room database with two entities:
  - `PostEntity`: Caches travel posts locally (id, title, location, rating, author, images, Wikipedia info, timestamps)
  - `UserEntity`: Caches user profiles locally (uid, name, email, photo URL)
- **Cache Synchronization**: PostsRepository syncs Firestore data to Room in real-time
- **Offline Access**: All cached posts and user data remain accessible when the app is offline
- **Files**: AppDatabase.kt, PostEntity.kt, UserEntity.kt

### 3. Object Caching ✅
- **Post Caching**: Posts are cached in Room via PostEntity. When users browse the Explore feed or their profile, data loads from the local Room cache
- **User Caching**: User profiles are cached in Room. When posts are displayed, author information is retrieved from the cached UserEntity
- **Cache Strategy**: PostsRepository maintains a real-time listener on Firestore that automatically updates the Room cache when posts change
- **Memory Caching**: In-memory caches in adapters (DestinationAdapter, DestinationPagingAdapter) store user names and photos to reduce repeated Room queries
- **Files**: PostsRepository.kt, UsersRepository.kt

### 4. Image Caching ✅
- **Image Library**: Picasso is used for all image loading and caching
- **Memory Caching**: Picasso provides built-in image loading with automatic memory and disk caching
- **Image Upload**: When users upload images, they're stored in Firebase Storage at `post_images/` and download URLs are saved to Firestore
- **Image Display**: Images are loaded via Picasso from:
  - Firebase Storage URLs (for newly uploaded images)
  - Local content URIs (for gallery-selected images)
- **Efficient Loading**: Picasso handles placeholder images, error states, and fit/crop operations
- **Files**: AddFragment.kt, DestinationPagingAdapter.kt

### 5. Paging / Gradual Loading ✅
- **Implementation**: Android Paging 3 Library enables gradual loading of large post lists
- **Configuration**: Posts load 10 items at a time (`pageSize = 10`) as users scroll
- **PagingSource**: PostsDao.pagingAll() returns a PagingSource that queries the Room database in pages
- **UI Integration**: DestinationPagingAdapter is a PagingDataAdapter that handles the paging logic automatically
- **Explore Screen**: ExploreFragment uses `repo.explorePaging()` to load posts gradually without loading the entire list at once
- **Benefits**: Improves app performance and provides smooth scrolling experience for large datasets
- **Files**: PostsDao.kt, DestinationPagingAdapter.kt, ExploreFragment.kt

### 6. Architecture ✅
- **Fragments**: All screens (authentication, main app) are implemented as Fragments for modularity and reusability
- **Navigation Graph**: Two navigation graphs handle app flow:
  - `nav_graph.xml`: Authentication flow
  - `main_graph.xml`: Main app navigation with bottom navigation between Explore, Add, and Profile
- **SafeArgs**: Type-safe navigation arguments between fragments using SafeArgs (e.g., PostDetailsFragmentArgs)
- **Repository Pattern**: Data access is abstracted in repository classes (PostsRepository, UsersRepository, WikiRepository)
- **ViewModels**: AddViewModel manages async operations and state for Wikipedia info fetching
- **Reactive Data**: Fragments observe Room data through Flow streams for reactive UI updates
- **Modular Code**: Clear separation between UI (Fragments), data access (Repository), and data models
- **Files**: MainActivity.kt, AddViewModel.kt, Repository classes

### 7. User System ✅
- **Signup**: SignupFragment enables users to create accounts with email, password, and optional profile picture
  - Uploads profile images to Firebase Storage
  - Stores user data in Firestore and Room
- **Login**: LoginFragment authenticates users with Firebase Authentication
- **Logout**: ProfileFragment includes a sign-out button that logs out the user and returns to authentication
- **User Profile**: ProfileFragment displays the current user's:
  - Email, name, and profile picture
  - All posts created by the user
- **Profile Editing**: EditProfileFragment allows users to update:
  - Display name and email
  - Profile picture with image upload to Firebase Storage
  - Changes are synchronized to both Firebase Authentication and Firestore
- **User Cache**: User data is cached locally in Room for offline access
- **Files**: SignupFragment.kt, LoginFragment.kt, ProfileFragment.kt, EditProfileFragment.kt

### 8. Content Management ✅
- **Create Posts**: AddFragment allows users to create posts with:
  - Title, location, and description (about trip)
  - Star rating (1-5 stars)
  - Optional image upload to Firebase Storage
  - Automatic Wikipedia destination info fetching
- **Edit Posts**: EditPostFragment enables users to modify existing posts:
  - Update title, location, description, and rating
  - Change the post image
  - Updates are saved to Firestore and Room cache
- **Delete Posts**: PostDetailsFragment includes a delete button (visible only to post author)
  - Removes post from Firestore and local Room cache
- **View Posts**: ExploreFragment displays all posts with pagination
  - PostDetailsFragment shows full post details, comments, and Wikipedia info
  - ProfileFragment displays only the current user's posts
- **Comments**: PostDetailsFragment enables users to:
  - Add comments to posts in real-time
  - Delete their own comments
- **Files**: AddFragment.kt, EditPostFragment.kt, PostDetailsFragment.kt

### 9. External API Usage ✅
- **Wikipedia API Integration**: The app integrates with Wikipedia's REST API to enrich destination information
- **Implementation Details**:
  - Uses Retrofit to make HTTP requests to `https://en.wikipedia.org/api/rest_v1/page/summary/{title}`
  - WikiApi interface defines the REST endpoint
  - WikiClient configures the Retrofit instance with OkHttp for logging and custom headers
  - WikiRepository handles API calls and response parsing
- **Data Retrieved**: For each destination, the API provides:
  - Title and extract (summary text)
  - Desktop page URL
  - Thumbnail image
- **Integration Points**:
  - AddViewModel fetches Wikipedia info when creating a post
  - EditPostFragment fetches updated Wikipedia info when location changes
  - PostDetailsFragment displays Wikipedia content alongside post details
- **Error Handling**: Gracefully handles 404 responses and network errors
- **Files**: WikiApi.kt, WikiClient.kt, WikiRepository.kt, WikiSummaryResponse.kt

## How to Run the Project

### Prerequisites
- Android Studio (Arctic Fox or later recommended)
- JDK 11 or later
- Android SDK API 36 or later
- Minimum device API level 26

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd MyDreamTrip
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the MyDreamTrip directory

3. **Configure Firebase**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use existing one
   - Enable Authentication (Email/Password method)
   - Enable Cloud Firestore
   - Enable Cloud Storage
   - Download the `google-services.json` file
   - Place `google-services.json` in the `app/` directory

4. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```
   
   Or use Android Studio's "Run" button (Shift+F10)

5. **Create Test Account**
   - On first launch, sign up with an email and password
   - Add a profile picture (optional)
   - Start creating travel posts!

### Project Structure

```
app/src/main/
├── java/com/example/mydreamtrip/
│   ├── data/
│   │   ├── local/                 # Room database entities and DAOs
│   │   │   ├── AppDatabase.kt
│   │   │   ├── PostEntity.kt
│   │   │   ├── UserEntity.kt
│   │   │   ├── PostsDao.kt
│   │   │   └── UsersDao.kt
│   │   ├── remote/                # API services
│   │   │   └── wiki/              # Wikipedia API integration
│   │   │       ├── WikiApi.kt
│   │   │       ├── WikiClient.kt
│   │   │       ├── WikiRepository.kt
│   │   │       └── WikiSummaryResponse.kt
│   │   └── repo/                  # Repository classes
│   │       ├── PostsRepository.kt
│   │       └── UsersRepository.kt
│   ├── model/                     # Data models
│   │   ├── Destination.kt
│   │   └── Comment.kt
│   ├── ui/                        # UI components
│   │   ├── add/
│   │   │   └── AddViewModel.kt
│   │   └── explore/
│   │       ├── ExploreFragment.kt
│   │       ├── DestinationAdapter.kt
│   │       ├── DestinationPagingAdapter.kt
│   │       └── GridSpacingItemDecoration.kt
│   ├── AddFragment.kt
│   ├── EditPostFragment.kt
│   ├── PostDetailsFragment.kt
│   ├── ProfileFragment.kt
│   ├── EditProfileFragment.kt
│   ├── LoginFragment.kt
│   ├── SignupFragment.kt
│   ├── WelcomeFragment.kt
│   ├── AuthActivity.kt
│   ├── MainActivity.kt
│   └── CommentAdapter.kt
├── res/
│   ├── navigation/                # Navigation graphs
│   │   ├── nav_graph.xml         # Auth flow
│   │   └── main_graph.xml        # Main app flow
│   ├── layout/                    # Fragment and activity layouts
│   ├── drawable/                  # App icons and assets
│   └── values/                    # Strings, colors, dimensions
└── AndroidManifest.xml

build/
└── (generated build files)
```

## Key Implementation Details

### Real-time Data Synchronization
- PostsRepository uses Firestore snapshot listeners (`addSnapshotListener`) to monitor changes in real-time
- When posts are created, edited, or deleted, the local Room cache is automatically updated
- Fragments observe Room data through Flow, automatically updating the UI when data changes

### Image Upload Flow
1. User selects image from device gallery
2. Image is uploaded to Firebase Storage (`post_images/` path)
3. Download URL is retrieved and saved in Firestore and Room
4. Picasso loads and caches the image for display

### Error Handling
- All Firestore operations include failure callbacks with user-friendly error messages via Toast
- Network errors are handled gracefully with fallback behavior
- Image upload failures don't prevent post creation (fallback to local URI)

### Performance Optimizations
- Paging library prevents loading entire post list at once
- In-memory caches reduce repeated database queries in adapters
- Firestore queries are filtered by author for profile view
- Picasso handles efficient image loading and caching

## Future Improvements

- Push notifications for new comments on user's posts
- Location-based destination recommendations
- Social features (following users, liking posts, user search)
- Offline mode with sync queue for posts created offline
- Photo gallery/carousel for posts with multiple images
- Map view for browsing destinations by location
- Post search and filtering
- User profiles with followers/following system
- Advanced filtering (by rating, date, location)
- Multi-language support

## Development Notes

- **Database Migrations**: Room uses fallbackToDestructiveMigration() during development to simplify schema changes
- **Firebase Permissions**: Firestore rules should be configured for production deployment
- **Image Permissions**: App requests READ_EXTERNAL_STORAGE permission for image selection
- **Coroutines**: All long-running operations use Kotlin coroutines with lifecycleScope
- **Memory Management**: Firestore listeners are properly removed in `onDestroyView()` to prevent memory leaks

---

**Last Updated**: March 2026  
**Built for**: Android API 26+ (Kotlin, Jetpack)</content>
<parameter name="filePath">c:\Users\user\AndroidStudioProjects\MyDreamTrip\README.md