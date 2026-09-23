# Project Plan

A basic custom keyboard Android app written in Kotlin using InputMethodService, XML layouts / View Binding, displaying a functional custom keyboard with minimal boilerplate code.

## Project Brief

# Project Brief: Custom Keyboard App

## Overview
A minimum viable product (MVP) custom Android keyboard application developed in Kotlin. It implements an Android `InputMethodService` using classic Android XML layouts and View Binding to display and handle soft keyboard inputs with minimal boilerplate code.

## Features
1. **Custom Input Method Service (`InputMethodService`)**: Core IME service lifecycle management integrating with the Android system input framework.
2. **XML & View Binding Keyboard UI**: Soft keyboard layout rendered using standard XML view layouts and View Binding for type-safe view access.
3. **Key Press Handling & Text Commitment**: Captures user key taps (letters, numbers, spacebar, enter, and backspace) and commits or deletes text via `InputConnection`.
4. **System Activation & Setup Screen**: Basic launcher screen directing users to enable and select the custom keyboard in Android System Settings.

## High-Level Tech Stack
- **Language**: Kotlin
- **UI Framework**: Classic Android Views (XML Layouts + View Binding)
- **Core Framework APIs**: Android Input Method Editor (`InputMethodService`, `InputConnection`)
- **SDK Targets**: `minSdk` 26, `targetSdk` 35
- **Core Dependencies**:
  - `androidx.core:core-ktx`
  - `androidx.lifecycle:lifecycle-service`
  - `androidx.appcompat:appcompat`


## Implementation Steps

