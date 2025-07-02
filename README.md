[![pub version](https://img.shields.io/pub/v/limited_video_recorder.svg)](https://pub.dev/packages/limited_video_recorder)
[![GitHub stars](https://img.shields.io/github/stars/sgsm74/limited_video_recorder)](https://github.com/sgsm74/limited_video_recorder/stargazers)

# limited_video_recorder

🎥 A Flutter plugin to record videos with customizable limits on **file size**, **duration**, **resolution**, **bitrate**, and **frame rate**.

> ✅ Android supported  
> ⏳ iOS support planned  
> 🔧 Designed for control and simplicity

---

## 🚀 Features

- 📦 Limit **maximum file size** (e.g., 10 MB)
- ⏱️ Limit **recording duration** (e.g., 30 seconds)
- 🎞️ Set **video resolution** (width × height)
- ⚡ Control **bitrate** and **frame rate**
- 🔊 Records video **with audio**
- 🔄 Clean controller-based API
- 📱 Preview camera with `AndroidView`
- 📂 Access to recorded video file path
- ℹ️ Extract video file metadata (orientation, duration, size)

---

## 📦 Installation

Add to your `pubspec.yaml`:

```yaml
dependencies:
  limited_video_recorder:
    git:
      url: https://github.com/sgsm74/limited_video_recorder.git
      ref: main
```

Then run:

```bash
flutter pub get
```

---

## 🧪 Quick Example

```dart
final recorder = LimitedVideoRecorderController();

final config = RecordingConfig(
  videoWidth: 1280,
  videoHeight: 720,
  maxFileSize: 15 * 1024 * 1024, // 15 MB
  maxDuration: 10 * 1000, // 10 seconds
  videoBitRate: 5_000_000,
  frameRate: 30,
);

await recorder.start(config: config);
// ... wait or perform UI updates ...
final path = await recorder.stop();
print("Video saved at: $path");
```

Use `onRecordingComplete((path) { ... })` to get the final path automatically.

---

## 📱 Android Permissions

Add the following to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

If targeting Android 13+, add runtime permission handling.

---

## 🧩 Example App UI

- Preview camera using AndroidView
- Start / Stop buttons
- Show final video using `video_player`
- Display file size, duration, orientation after recording

---

## 🔥 Planned Features

- [ ] iOS support  
- [ ] Flash toggle  
- [ ] Pause/Resume recording  
- [ ] Manual focus / exposure controls  
- [ ] Thumbnail generation

---

## 🧑‍💻 Author

Developed by **[Saeed Ghasemi](https://saeedqasemi.ir)**  
Feel free to contribute, suggest features, or report bugs!
