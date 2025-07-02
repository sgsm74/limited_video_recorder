import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'limited_video_recorder_platform_interface.dart';

/// An implementation of [LimitedVideoRecorderPlatform] that uses method channels.
class MethodChannelLimitedVideoRecorder extends LimitedVideoRecorderPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('limited_video_recorder');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
