# FluidLauncher

FluidLauncher is a lightweight, fluid home-screen replacement for Android. It discovers launchable apps, supports multiple animated home pages and a searchable curved app drawer, keeps the system wallpaper visible, and requests the highest available 120 Hz display mode.

## Build

```bash
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also builds and uploads an installable debug APK on every push; version tags (`v*`) additionally create a GitHub release.

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The project intentionally does not commit Gradle's binary wrapper JAR. Install
Gradle 8.10.2 locally, or let the included GitHub Actions workflow provision
that exact version automatically.

Press Home and select **FluidLauncher**. Long-press an empty area to open live motion tuning. Swipe horizontally between pages and swipe upward (or tap the dock button) to open the app drawer.

## Requirements

- Android 8.0+ (optimized for Android 14+ and high-refresh displays)
- OpenGL ES 3.0-capable device
