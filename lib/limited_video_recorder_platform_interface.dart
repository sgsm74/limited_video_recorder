import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'limited_video_recorder_method_channel.dart';

abstract class LimitedVideoRecorderPlatform extends PlatformInterface {
  /// Constructs a LimitedVideoRecorderPlatform.
  LimitedVideoRecorderPlatform() : super(token: _token);

  static final Object _token = Object();

  static LimitedVideoRecorderPlatform _instance = MethodChannelLimitedVideoRecorder();

  /// The default instance of [LimitedVideoRecorderPlatform] to use.
  ///
  /// Defaults to [MethodChannelLimitedVideoRecorder].
  static LimitedVideoRecorderPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [LimitedVideoRecorderPlatform] when
  /// they register themselves.
  static set instance(LimitedVideoRecorderPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
