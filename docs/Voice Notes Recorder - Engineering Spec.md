# Voice Notes Recorder - Product Specification

## Overview

Develop a simple Android application named **Voice Notes Recorder**.

The primary purpose of the application is to allow a user to manually record audio during meetings, phone conversations on speakerphone, online calls, discussions, brainstorming sessions, and other situations where note-taking is difficult.

The application is intended for personal use only and is not intended to integrate with Android's call recording APIs.

The app should simply record audio from the device microphone and save it locally as an audio file.

---

# Objectives

The application must:

* Start recording when the user presses a button.
* Stop recording when the user presses a button.
* Save recordings locally on the device.
* Display a list of previously recorded files.
* Allow playback of recordings.
* Allow deletion of recordings.
* Continue recording while the app is in the background.
* Display an ongoing foreground notification while recording.

The application should be extremely lightweight and reliable.

---

# Technical Requirements

## Platform

* Android
* Minimum SDK: 26
* Target SDK: Latest stable Android SDK
* Kotlin
* Jetpack Compose
* Material 3
* MVVM Architecture
* Coroutines + Flow
* Hilt Dependency Injection

---

# Audio Recording Requirements

## Recording Method

Use:

* MediaRecorder

Configuration:

* Audio Source: MIC
* Output Format: MPEG_4
* Audio Encoder: AAC

Output file extension:

* .m4a

Reason:

* Excellent speech quality
* Small file size
* Native Android support
* Minimal complexity

---

# Storage Requirements

Store recordings in:

Android MediaStore

Suggested folder:

VoiceNotesRecorder

Example filenames:

2026-06-08_10-30-15.m4a

2026-06-08_11-45-32.m4a

File names should be generated automatically.

---

# Permissions

Required:

* RECORD_AUDIO
* FOREGROUND_SERVICE

Request microphone permission on first use.

---

# Foreground Service

Recording must continue when:

* Screen is locked
* User switches apps
* User receives notifications

Requirements:

* Foreground service
* Persistent notification while recording

Notification example:

Recording in progress
00:12:45 elapsed

Tapping the notification should reopen the app.

---

# User Interface

## Screen 1 - Recorder

### Idle State

Display:

* App title
* Large Start Recording button
* Recent recordings count

### Recording State

Display:

* Recording indicator
* Elapsed timer
* Stop Recording button

Visual indicator:

* Pulsing recording dot
* Timer updates every second

---

## Screen 2 - Recordings

Display:

* List of recordings

Each item shows:

* File name
* Date created
* Duration
* File size

Actions:

* Play
* Pause
* Delete
* Share

Use LazyColumn.

---

# Navigation

Bottom Navigation

Tab 1:
Recorder

Tab 2:
Recordings

---

# Audio Playback

Implement using ExoPlayer.

Features:

* Play
* Pause
* Seek
* Stop when another recording starts playing

---

# Error Handling

Handle:

* Permission denied
* Storage unavailable
* Recording initialization failure
* Unexpected recorder crash

Display user-friendly messages.

---

# Architecture

## Layers

### Presentation

* Compose UI
* ViewModels
* UI State

### Domain

* Recording Use Cases
* Playback Use Cases

### Data

* MediaRecorder Manager
* MediaStore Repository
* Audio Player Repository

---

# Folder Structure

com.example.voicenotes

* data

  * recorder
  * player
  * repository

* domain

  * model
  * usecase

* presentation

  * recorder
  * recordings
  * navigation

* service

  * RecordingForegroundService

* di

---

# Non-Functional Requirements

* Fast startup
* Minimal battery consumption
* Reliable long-duration recording
* Support recordings longer than one hour
* No advertisements
* No analytics
* No internet access required

---

# Future Enhancements (Do Not Implement Yet)

Potential future features:

* Speech-to-text transcription
* AI-generated summaries
* Action item extraction
* Recording search
* Recording tags
* Recording folders
* Export transcript as PDF
* Cloud backup

These features should not be implemented in version 1.

---

# Acceptance Criteria

The application is considered complete when:

1. User can start recording with one tap.
2. User can stop recording with one tap.
3. Recording continues in background.
4. Audio is saved successfully.
5. User can play recordings.
6. User can delete recordings.
7. User can share recordings.
8. App remains stable during long recordings.
9. All functionality works without internet access.
10. UI follows Material 3 guidelines.
