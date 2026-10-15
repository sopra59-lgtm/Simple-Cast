# Simple-Cast — Android screen capture prototype

Native Android application (Java, Android 10+) that captures the **entire device screen** via MediaProjection after explicit user approval. It exposes JPEG still frames on a local HTTP server for a browser-based prototype. No root required.

## Current scope (v0.1.0)

- Screen capture via `MediaProjection` foreground service, user approval required every session.
- Network preview on a **trusted** LAN: `http://PHONE_IP:8765`, refreshed approximately 2–3 times/second.
- Start / Stop controls; foreground notification; secure screen content may be blacked out by Android.
- Debug APK built by GitHub Actions (Actions → Android APK → artifact).
- **Chromecast direct mirroring is NOT implemented yet.** The browser preview is not equivalent to Chromecast casting.

## Roadmap for real Chromecast

1. Build a Google Cast *Custom Web Receiver*, host it on an HTTPS URL and register it in the [Google Cast SDK Developer Console](https://cast.google.com/publish/); note the receiver application ID.
2. Add the Android Cast Sender SDK with device discovery and session management.
3. Replace the LAN HTTP JPEG preview with a supported transport for Cast (e.g. video encoder + supported streaming format and transport, or a TLS-secured signaling/media infrastructure plus a compatible custom receiver). Validate real device codec support, audio, latency, orientation and rotation.
4. Make sessions access-controlled. Do not publicly expose the experimental local HTTP endpoint.

Important: an HTTPS Chromecast receiver cannot generally connect to the current `http://PHONE_IP:8765` preview because mixed-content restrictions apply. Merely changing the receiver URL or declaring the default Cast receiver is **not** a working end-to-end mirror.

## Build

- Android Studio with Android SDK Platform 35 and JDK 17, Gradle 8.11.1 (Android Gradle Plugin 8.9.2).
- Import project and run `gradle assembleDebug` or use the Actions artifact.
- Install APK, connect phone and second device to the same trusted Wi-Fi, launch app, approve screen capture, open `http://PHONE_IP:8765` in a browser while screen capture is active.

## Limitations and security

The HTTP server is unauthenticated and reachable on the local network; do not use on school/public Wi-Fi or for sensitive content. No sound, no Chromecast discovery or Cast receiver support yet. Capture is throttled to roughly 3 JPEG frames per second and is **not suitable for fluid video**. A foreground capture service is required and Android may prevent protected content capture. On modern Android, user permission must be obtained for each fresh capture session.

This repository uses a Java project to avoid external libraries for the prototype. Test compilation and the APK on the target hardware; generated APK is debug-signed and not Play Store ready.
