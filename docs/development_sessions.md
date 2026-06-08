# Voice Notes Recorder - Development Sessions Plan

This document outlines a structured, step-by-step plan to develop the **Voice Notes Recorder** application starting from the empty-activity Jetpack Compose seed project.

The development process is broken down into **7 manageable sessions**. Each session is designed to be executable independently, ending in a stable, compileable state. After confirming each session compiles and functions as expected, changes must be committed to the `master` branch.

---

## Git Workflow
1. At the beginning of each session, the assistant will read the codebase to align context.
2. At the end of each session, the assistant will compile the project and verify it runs.
3. Upon your confirmation, the assistant will stage all changes and commit them directly to the `master` branch with a descriptive message (e.g. `git add . && git commit -m "Finish Session X: ..."`).
4. No separate branches are required in between.

---

## Summary of Sessions

| Session | Name | Core Deliverables |
| :--- | :--- | :--- |
| **Session 1** | [Dependency Setup & Architecture Core](#session-1-dependency-setup-architecture-core) | Version catalog modifications, Hilt injection, folder structure setup, base application, and Material 3 theme colors. |
| **Session 2** | [Audio Recording Interface & MediaRecorder](#session-2-audio-recording-interface--mediarecorder) | `AudioRecorder` abstraction, `MediaRecorderManager` data implementation, permissions utility for Microphone/Notifications. |
| **Session 3** | [MediaStore Repository & Storage Engine](#session-3-mediastore-repository--storage-engine) | MediaStore saving, querying and deleting recordings, automatic filename formatting, sharing intent, and domain use cases. |
| **Session 4** | [Foreground Recording Service](#session-4-foreground-recording-service) | `RecordingForegroundService`, notification channel with ongoing active status, running time tracker (1s updates), and background execution. |
| **Session 5** | [Audio Playback Engine (ExoPlayer)](#session-5-audio-playback-engine-exoplayer) | Media3 ExoPlayer integration, player states (Play/Pause/Seek/Stop), stop playback on new recording start, and playback use cases. |
| **Session 6** | [ViewModels & Jetpack Compose UI](#session-6-viewmodels--jetpack-compose-ui) | ViewModels, Bottom Navigation, Recorder Screen (pulsing animations), Recordings Screen (cards, seek bars, swipe-to-delete), Dark Theme. |
| **Session 7** | [Error Handling, E2E Testing & Release](#session-7-error-handling-e2e-testing--release) | Offline operation verification, permissions denial flows, storage failure recoveries, hardware state safety, and final release validation. |

---

## Session Details & Prompts

### Session 1: Dependency Setup & Architecture Core

#### Objective
Establish the project foundation by updating dependency configurations, creating the Hilt application environment, organizing packages, and configuring the design system tokens.

#### Code Modifications
*   Modify `gradle/libs.versions.toml` to add Hilt, Navigation, and Media3 ExoPlayer libraries/plugins.
*   Modify root `build.gradle.kts` and `app/build.gradle.kts` to apply plugins and implement libraries.
*   Change `minSdk` to `26` in `app/build.gradle.kts` to match the specs.
*   Create packages: `data/recorder`, `data/player`, `data/repository`, `domain/model`, `domain/usecase`, `presentation/recorder`, `presentation/recordings`, `presentation/navigation`, `service`, `di`.
*   Create `VoiceNotesApplication` extending `Application` with `@HiltAndroidApp` and register it in `AndroidManifest.xml`.
*   Modify `ui.theme` (Color, Type, Theme, Shape) with design specifications.

---

#### Copy-Paste Prompt for Session 1
```markdown
We are building the "Voice Notes Recorder" app using an MVVM architecture. The goal of this session is to configure all dependencies (Hilt, Jetpack Compose Navigation, Media3 ExoPlayer), organize the project packages, set up the Application class, and implement the custom color palette, shapes, and typography.

Please perform the following steps:

1. Update `gradle/libs.versions.toml`:
   - Add Hilt compiler and android dependencies (Version: "2.51.1" or similar stable version).
   - Add Hilt Navigation Compose dependency (Version: "1.2.0" or similar stable version).
   - Add Jetpack Compose Navigation dependency (Version: "2.8.2" or similar stable version).
   - Add Media3 ExoPlayer dependencies (`androidx.media3:media3-exoplayer` and `androidx.media3:media3-ui` Version: "1.4.1" or similar stable version).
   - Add Hilt Gradle Plugin dependency.

2. Update root `build.gradle.kts` and `app/build.gradle.kts`:
   - Apply the Dagger Hilt Android plugin.
   - Configure dependencies block in `app/build.gradle.kts` to implement Hilt, Navigation Compose, ExoPlayer, and Material 3 extended icons (if needed).
   - Change the `minSdk` in `app/build.gradle.kts` to 26 (as per the engineering spec). Make sure Java version compatibilities remain at Java 11 (or Java 17, depending on what works best with current Kotlin/Gradle setup).

3. Create the standard package directory layout under `com.svcj91.naradavoicerecorder`:
   - `data/recorder`
   - `data/player`
   - `data/repository`
   - `domain/model`
   - `domain/usecase`
   - `presentation/recorder`
   - `presentation/recordings`
   - `presentation/navigation`
   - `service`
   - `di`

4. Create `VoiceNotesApplication.kt` in the root package (`com.svcj91.naradavoicerecorder`), annotate it with `@HiltAndroidApp` to initialize Dagger Hilt, and register it in `app/src/main/AndroidManifest.xml` under `<application android:name=".VoiceNotesApplication" ...>`.

5. Update the UI Theme files in `ui.theme`:
   - Configure colors in `Color.kt` for Light and Dark modes:
     - Primary (Dark Navy): `#2B2D42` (Light) / `#EDF2F4` (Dark)
     - Secondary (Cool Gray Blue): `#8D99AE`
     - Main Background/Surface: `#EDF2F4` / `#1D1F2E` (Dark Background) / `#2B2D42` (Dark Surface)
     - Accent (Coral Red): `#EF233C`
     - Destructive (Deep Red): `#D90429`
   - Update `Type.kt` to define the typography scale (Display Large: 32sp, Headline: 24sp, Title: 20sp, Body: 16sp, Caption: 13sp). Set preferred font family to Inter (fallback to system default sans-serif if not packaged, or setup google fonts integration for Inter). Use medium and semi-bold weights instead of heavy bold.
   - Define custom shape tokens in `ui/theme/Shape.kt` with rounded corner radius: Small (12dp), Medium (16dp), Large (24dp).
   - Wire everything up in `Theme.kt` so that the custom theme applies light/dark colors and shape setups properly to Compose controls.

6. Build and compile the project to verify that the Gradle configuration syncs successfully and the app builds without errors. Do not proceed to change MainActivity's UI yet; just show a simple placeholder using the new theme.
```

---

### Session 2: Audio Recording Interface & MediaRecorder

#### Objective
Build the recording capabilities. Define core abstractions in the domain layer and implement the Android `MediaRecorder` manager in the data layer.

#### Code Modifications
*   Create `AudioRecorder.kt` interface in `domain/model` or `domain/repository`.
*   Create `MediaRecorderManager.kt` implementing `AudioRecorder` in `data/recorder`.
*   Create use cases: `StartRecordingUseCase.kt`, `StopRecordingUseCase.kt`, `GetRecordingStateUseCase.kt` in `domain/usecase`.
*   Create `RecorderModule.kt` in `di` to bind implementation to the interface.
*   Create a permission helper/utility in `presentation/` or `data/` to track RECORD_AUDIO and notification permissions.

---

#### Copy-Paste Prompt for Session 2
```markdown
The objective of this session is to implement the audio recording infrastructure in the data and domain layers.

Please perform the following steps:

1. Create a domain interface `AudioRecorder` in `com.svcj91.naradavoicerecorder.domain.model` or `com.svcj91.naradavoicerecorder.domain.repository` defining:
   - `fun start(outputFile: File)`
   - `fun stop()`
   - `val isRecording: Flow<Boolean>` (or state flow exposing current recorder state)

2. Create `MediaRecorderManager.kt` under `com.svcj91.naradavoicerecorder.data.recorder` implementing `AudioRecorder`:
   - Initialize and configure `MediaRecorder` (or use `MediaRecorder` constructor depending on Android version SDK compatibility).
   - Set Audio Source: `AudioSource.MIC`.
   - Set Output Format: `OutputFormat.MPEG_4`.
   - Set Audio Encoder: `AudioEncoder.AAC`.
   - Output file extension must be `.m4a` (excellent speech quality, small file size, native support).
   - Handle preparation and start errors safely inside `try-catch` blocks and expose failure states if initialization/recording start fails.

3. Create the recording Use Cases under `com.svcj91.naradavoicerecorder.domain.usecase`:
   - `StartRecordingUseCase` (takes a target file, triggers recording).
   - `StopRecordingUseCase` (stops the recording).
   - `GetRecordingStateUseCase` (exposes recording state flow).

4. Register and configure Hilt DI in `com.svcj91.naradavoicerecorder.di.RecorderModule` to bind `MediaRecorderManager` to `AudioRecorder` with `@Singleton` scope.

5. Create a utility class `PermissionHelper` to encapsulate runtime checks and details for permissions:
   - `RECORD_AUDIO` (microphone access).
   - `POST_NOTIFICATIONS` (required for Android 13+ to show foreground service notification).
   - `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MICROPHONE` (required for Android 14+ background recording).

6. Verify that the code compiles. Write a quick unit test or simple verification checks to ensure the MediaRecorder configuration meets our AAC/.m4a specifications.
```

---

### Session 3: MediaStore Repository & Storage Engine

#### Objective
Implement the local filesystem logic using Android MediaStore, ensuring files are saved to the designated directory, queried correctly, and deleted cleanly.

#### Code Modifications
*   Create model `Recording.kt` in `domain/model`.
*   Create `RecordingRepository.kt` interface in `domain/repository` or `domain/model`.
*   Create `MediaStoreRepository.kt` in `data/repository`.
*   Create use cases: `GetRecordingsUseCase.kt`, `DeleteRecordingUseCase.kt`, `ShareRecordingUseCase.kt` in `domain/usecase`.
*   Create `RepositoryModule.kt` in `di`.

---

#### Copy-Paste Prompt for Session 3
```markdown
The objective of this session is to build the file storage engine using Android MediaStore. We must save files locally, format filenames automatically, query files to populate the playlist, and handle file sharing and deletion.

Please perform the following steps:

1. Create a model class `Recording.kt` under `com.svcj91.naradavoicerecorder.domain.model` representing a recording:
   - `val id: Long`
   - `val name: String`
   - `val uri: Uri`
   - `val dateCreated: Long` (timestamp)
   - `val durationMs: Long`
   - `val sizeBytes: Long`

2. Create an interface `RecordingRepository` under `com.svcj91.naradavoicerecorder.domain.repository` exposing:
   - `fun getRecordings(): Flow<List<Recording>>`
   - `suspend fun deleteRecording(recording: Recording): Boolean`
   - `fun getShareIntent(recording: Recording): Intent`
   - `fun createTempFile(): File` (to record into before publishing to MediaStore, or manage directly via MediaStore pending status).

3. Create `MediaStoreRepository.kt` in `com.svcj91.naradavoicerecorder.data.repository`:
   - Store audio in `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
   - Save files in the directory `VoiceNotesRecorder` (configured via `RelativePath` under `Music/VoiceNotesRecorder` or `Recordings/VoiceNotesRecorder`).
   - Automatically format filenames using the pattern: `yyyy-MM-dd_HH-mm-ss.m4a` (e.g., `2026-06-08_10-30-15.m4a`).
   - Query all files in the `VoiceNotesRecorder` folder, mapping column values (DISPLAY_NAME, DATA/URI, DATE_ADDED, DURATION, SIZE) to the `Recording` model. Order the list by date created in descending order (newest first).
   - Implement `deleteRecording` which deletes the entry from `ContentResolver` and removes the actual file from disk. Ensure Android 10+ scoped storage restrictions are handled safely.

4. Create domain use cases:
   - `GetRecordingsUseCase`
   - `DeleteRecordingUseCase`
   - `ShareRecordingUseCase`

5. Create `di/RepositoryModule.kt` to bind the repository to `MediaStoreRepository` using Hilt.

6. Verify that the project compiles and repository queries compile without issues.
```

---

### Session 4: Foreground Recording Service

#### Objective
Ensure reliable background recording by building a Foreground Service that remains active when the screen is locked or the app is closed, showing a persistent notification with a ticking duration timer.

#### Code Modifications
*   Create `RecordingForegroundService.kt` in `service`.
*   Update `AndroidManifest.xml` to declare the service and request background permissions.
*   Bind the service to the UI and ensure proper lifecycle interaction (start/stop actions).

---

#### Copy-Paste Prompt for Session 4
```markdown
The goal of this session is to implement background recording reliability using an Android Foreground Service. The app must continue recording when minimized, when notifications arrive, or when the screen locks.

Please implement the following:

1. Create `RecordingForegroundService.kt` under `com.svcj91.naradavoicerecorder.service`:
   - The service must run in the foreground.
   - It should inject and use `AudioRecorder` and `RecordingRepository` (or the use cases) to manage recording operations.
   - When started with an action (e.g., `ACTION_START`), it must configure and show an ongoing notification.
   - Declare a notification channel (ID: "recording_channel", Name: "Recording Service").
   - The notification must display:
     - Title: "Recording in progress"
     - Subtext: Ticking timer updated every second showing elapsed time (e.g., `00:12:45`).
     - Clicking the notification must trigger a PendingIntent that opens `MainActivity`.
   - The service should manage a coroutine scope to update the notification timer tick every 1000ms.
   - When the service is stopped (via `ACTION_STOP` or service destruction), clean up resources, stop recording, publish the file to MediaStore, and remove the notification.

2. Update `AndroidManifest.xml` to configure the service:
   - Add permissions:
     - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />`
     - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />` (For API 34+ microphone permission rules)
   - Declare the service:
     ```xml
     <service
         android:name=".service.RecordingForegroundService"
         android:foregroundServiceType="microphone"
         android:exported="false" />
     ```

3. Ensure the service lifecycle is safe. Implement a broadcast receiver, binder interface, or static action intent helper to let the UI control and query service states easily.

4. Verify that the service compiles and conforms to Android SDK foreground limitations.
```

---

### Session 5: Audio Playback Engine (ExoPlayer)

#### Objective
Build the playback system using Media3 ExoPlayer, supporting Play, Pause, Seeking, and stopping playback immediately when another item starts playing.

#### Code Modifications
*   Create `AudioPlayer.kt` interface in `domain/model` or `domain/repository`.
*   Create `ExoPlayerManager.kt` implementing `AudioPlayer` in `data/player`.
*   Create use cases: `PlayAudioUseCase.kt`, `PauseAudioUseCase.kt`, `SeekAudioUseCase.kt`, `StopAudioUseCase.kt`, `GetPlaybackStateUseCase.kt`.
*   Create `PlayerModule.kt` in `di`.

---

#### Copy-Paste Prompt for Session 5
```markdown
The objective of this session is to implement audio playback functionality using Media3 ExoPlayer in the data and domain layers.

Please perform the following:

1. Create an interface `AudioPlayer` under `com.svcj91.naradavoicerecorder.domain.model` or `com.svcj91.naradavoicerecorder.domain.repository` containing:
   - `fun play(uri: Uri)`
   - `fun pause()`
   - `fun seekTo(positionMs: Long)`
   - `fun stop()`
   - `val playbackState: Flow<PlaybackState>` where `PlaybackState` exposes:
     - `val isPlaying: Boolean`
     - `val currentPositionMs: Long`
     - `val durationMs: Long`
     - `val activeUri: Uri?`
     - `val isCompleted: Boolean`

2. Create `ExoPlayerManager.kt` under `com.svcj91.naradavoicerecorder.data.player`:
   - Instantiate `ExoPlayer` using the application context.
   - Implement `play(uri: Uri)`, `pause()`, `seekTo(positionMs)`, and `stop()`.
   - If a file is already playing and the user plays a *different* file, the current playback must stop immediately, and the player must load and start the new recording.
   - Periodically emit progress updates (every 200ms-500ms when playing) and listen to player event callbacks to keep the `playbackState` flow up-to-date.
   - Release the player resources when the application is terminated or the manager is cleared.

3. Create the playback Use Cases under `com.svcj91.naradavoicerecorder.domain.usecase`:
   - `PlayAudioUseCase`
   - `PauseAudioUseCase`
   - `SeekAudioUseCase`
   - `StopAudioUseCase`
   - `GetPlaybackStateUseCase`

4. Set up `di/PlayerModule.kt` using Hilt to bind `ExoPlayerManager` as a singleton implementation of `AudioPlayer`.

5. Verify that the project compiles and dependency wiring functions correctly.
```

---

### Session 6: ViewModels & Jetpack Compose UI

#### Objective
Build the presentation layer. Configure ViewModels to wire business logic flows, set up bottom navigation, and design premium, animated Jetpack Compose screens for both Light and Dark modes.

#### Code Modifications
*   Create `presentation/recorder/RecorderViewModel.kt` and `RecorderScreen.kt`.
*   Create `presentation/recordings/RecordingsViewModel.kt` and `RecordingsScreen.kt`.
*   Create `presentation/navigation/AppNavigation.kt` (setting up NavHost and tabs).
*   Modify `MainActivity.kt` to load the UI and request permissions flow.

---

#### Copy-Paste Prompt for Session 6
```markdown
The goal of this session is to implement the ViewModels, Navigation, and Compose Screens following the Design System. The app should have a premium, clean, professional productivity aesthetic.

Please build the following:

1. Create `RecorderViewModel.kt` under `presentation/recorder`:
   - Manage the recorder screen states: Idle State vs Recording State.
   - Expose elapsed recording timer flow, recording status, and recent recording count.
   - Interact with the recording foreground service to start and stop recordings.

2. Create `RecordingsViewModel.kt` under `presentation/recordings`:
   - Load the list of files from the MediaStore.
   - Track active audio playback state, current position, and duration.
   - Interface with playback and storage use cases (Play, Pause, Seek, Delete, Share).

3. Setup Bottom Navigation in `MainActivity` / `presentation/navigation`:
   - Configured with two tabs:
     - Tab 1: "Recorder" (Icon: Mic)
     - Tab 2: "Recordings" (Icon: List/Folder)

4. Build the Compose Screens in their respective packages:
   - **Screen 1: Recorder UI (`RecorderScreen.kt`)**:
     - Modern aesthetic: large circular record button (96dp-120dp, Coral Red `#EF233C`) centered with generous spacing.
     - Add a subtle pulsing scale animation to the button and an accent dot when recording.
     - Display a prominent timer (format: `00:05` or `01:12:45`).
     - Display the recent recordings count indicator below the button in Idle State.
   - **Screen 2: Recordings UI (`RecordingsScreen.kt`)**:
     - Display recordings in a modern `LazyColumn` containing rounded cards (16dp radius).
     - Each card displays file name, date, duration, and file size.
     - Play/Pause icon button inside the card. When actively playing, show an expandable playback seekbar.
     - Implement swipe-to-delete with smooth item removal animations.
     - Provide a sharing action button.
     - **Empty State**: When no files are found, show a beautiful vector illustration or waveform graphic with text "No recordings yet" and subtext "Your recordings will appear here".

5. Apply Visual Styling Details:
   - Utilize typography scales and shape tokens from the design theme.
   - Style components with specific primary Dark Navy (`#2B2D42`), Cool Gray Blue (`#8D99AE`), Soft White (`#EDF2F4`), and Coral Red (`#EF233C`) colors.
   - Implement complete Dark Theme mapping: Background (`#1D1F2E`), Surface (`#2B2D42`), Primary Text (`#EDF2F4`).

6. Integrate Permissions request flow in `MainActivity.kt` using `rememberLauncherForActivityResult`. Request `RECORD_AUDIO` and `POST_NOTIFICATIONS` (for Android 13+) before letting the user trigger recording.

7. Verify that the application builds, runs on an emulator or device, and transitions smoothly between tabs.
```

---

### Session 7: Error Handling, E2E Testing & Release

#### Objective
Harden the codebase. Implement robust error handling, verify offline operational stability, test edge cases, and perform final production quality checks.

#### Code Modifications
*   Implement error boundaries, rationales, and fallback UIs across ViewModels and Screens.
*   Conduct end-to-end user tests.
*   Finalize code cleanups and ensure successful compilation.

---

#### Copy-Paste Prompt for Session 7
```markdown
The final session focuses on error handling, edge case testing, offline compatibility checks, and final production validation.

Please complete the following:

1. Add Robust Error Handling:
   - **Permission Denied**: If a user denies Microphone or Notification permissions, display a polite custom rationale dialog, disable corresponding buttons, and provide an action button directing them to Settings.
   - **Storage Issues**: Handle cases where storage is full, write-protected, or unmounted. Show a user-friendly Snackbar or custom error banner.
   - **Recorder Failures**: If `MediaRecorder` fails to initialize or crashes due to hardware being in use by another app, recover gracefully, reset the recording UI, and notify the user.
   - **Player Failures**: If ExoPlayer encounters a corrupt file or files deleted from disk outside the app, handle the player error, show a message, and refresh the playlist.

2. Validate Non-Functional Specs & Edge Cases:
   - **Background Reliability**: Verify background recording continues when screen is locked or another app is opened.
   - **Dynamic Notification**: Verify the notification timer updates accurately every second and doesn't leak memory.
   - **Offline Compatibility**: Turn off all network connectivity and verify the app operates perfectly (no ads, no analytics, no network operations).
   - **Playback Logic**: Test playing audio, pausing, seeking, and ensure playing a new file interrupts and stops the active track immediately.
   - **Memory Leak check**: Verify that the ExoPlayer and MediaRecorder are released properly.

3. Run final code reviews and clean up any unused imports or legacy boilerplate.

4. Run lint checks and build a production release APK using Gradle (`./gradlew assembleRelease` or `./gradlew build`) to ensure the build compiles without warnings or compilation errors.
```
