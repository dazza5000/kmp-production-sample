[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# KMP RSS Reader

<img src="/media/Android+iOS+Desktop.png"/>

This is an open-source cross-platform RSS reader application built with [Kotlin Multiplatform](https://kotlinlang.org/multiplatform/) and [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/).

## Project Structure

This repository contains a common Kotlin Multiplatform module (`shared`), an Android app (`androidApp`), a desktop app (`desktopApp`), and an iOS app (`iosApp`):

- `shared`: Common multiplatform code shared across all targets:
  - `core`: Network client (`HttpClient`) and `RssReader` entry point coordinator.
  - `datasource/network`: Ktor-based RSS feed fetching (`FeedLoader`).
  - `datasource/storage`: Local persistence with Room KMP (`RssDatabase`, `RssDao`, `FeedStorage`, SQLite entities).
  - `domain`: Shared domain models (`RssFeed`, `Channel`, `Item`) with XML serialization annotations.
  - `presentation`: MVVM state management (`FeedViewModel`, `FeedUiState`, `FeedUiEvent`).
  - `ui`: Shared Compose Multiplatform UI screens and navigation (`RssReaderApp`, `MainScreen`, `FeedListScreen`, `AppTheme`).
- `androidApp`: Android application entry point.
- `desktopApp`: Desktop application entry point using Compose for Desktop.
- `iosApp`: iOS application built in SwiftUI, connecting to the shared `RssReader` framework via Xcode.

You can achieve a similar structure by creating a project with the [KMP project wizard](https://kmp.jetbrains.com/?android=true&ios=true&iosui=compose&desktop=true&includeTests=true).

<img src="/media/basic-structure.png"/>

## Architecture

This sample demonstrates sharing data, domain, state management, and UI across platforms.

### Shared Data & Storage

- **Networking**: [Ktor HTTP Client](https://ktor.io/docs/client.html) fetches RSS feeds asynchronously across all platforms.
- **XML Parsing**: Multiplatform XML parsing is handled in common code using [xmlutil](https://github.com/pdvrieze/xmlutil) and `kotlinx.serialization` (`@XmlSerialName`, `@XmlElement`).
- **Local Storage**: [Room KMP](https://developer.android.com/kotlin/multiplatform/room) with `BundledSQLiteDriver` persists feeds and items in a local SQLite database for offline reading.

### Shared Application State (MVVM)

State management follows the MVVM pattern with `FeedViewModel` in `com.github.jetbrains.rssreader.presentation`:
- **UiState**: Emits immutable `FeedUiState` snapshots via Kotlin `StateFlow`.
- **UiEvents**: One-off events (such as error alerts) are dispatched through a buffered `Channel<FeedUiEvent>` exposed as a `Flow`.
- **Dependency Injection**: [Koin](https://insert-koin.io/) provides dependency injection across common and platform modules.

### UI Layer

- **Android & Desktop**: Share Compose Multiplatform UI components directly from `shared/src/commonMain/kotlin/.../ui`.
- **iOS**: Built natively with SwiftUI. The shared `FeedViewModel` is wrapped in an `ObservableFeedStore` (`ObservableObject`) that observes `uiState` updates via `@Published` properties using `IosViewModelUtils` / `CFlow`.

## Build & Run

> **Prerequisite**: JDK 17 or JDK 21 (JDK 25 is currently not supported by AGP).

- **Android**: Run `./gradlew assembleDebug` or launch from Android Studio.
- **Desktop**: Run `./gradlew :desktopApp:run`.
- **iOS**: Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

## Testing

Shared business logic and ViewModels are tested in `shared/src/commonTest` (e.g. `FeedViewModelTest`):

```bash
./gradlew :shared:jvmTest
```
