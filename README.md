# Exchange Tracker

A modern Android application for tracking cryptocurrency exchange rates in real-time using WebSockets.

## Project Overview

Exchange Tracker is a Jetpack Compose application that allows users to select and monitor cryptocurrency assets from various exchanges. The app provides real-time price updates through WebSocket connections and maintains a clean, responsive UI.

## Key Features

- **Real-time Price Updates**: Subscribe to WebSocket feeds for instant price changes
- **Asset Selection**: Browse and select from a comprehensive list of cryptocurrency assets
- **Local State Management**: Changes in the asset selection bottom sheet are only committed when explicitly saved
- **Persistent Storage**: Selected assets are saved locally for quick access between sessions
- **Asset Removal**: Easily remove assets with a confirmation dialog to prevent accidental deletions

## Architecture

The application follows Clean Architecture principles with an MVI (Model-View-Intent) pattern for the UI layer:

- **Domain Layer**: Contains business models and repository interfaces
- **Data Layer**: Implements data sources (local and remote) and repositories
- **UI Layer**: Uses Jetpack Compose with MVI pattern for reactive UI updates

## Technologies

- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern toolkit for building native UI
- **Coroutines & Flow**: For asynchronous operations and reactive programming
- **Room**: For local database storage
- **Ktor Client**: For network requests and WebSocket connections
- **Hilt**: For dependency injection
- **Material 3**: For modern UI components and theming

## Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the application on an emulator or physical device

## Future Enhancements

- Support for additional exchanges
- Price alerts and notifications
- Historical price charts
- Portfolio tracking
- Theme customization
