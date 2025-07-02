import 'package:flutter_test/flutter_test.dart';
import 'package:limited_video_recorder/limited_video_recorder_platform_interface.dart';
import 'package:limited_video_recorder/limited_video_recorder_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockLimitedVideoRecorderPlatform with MockPlatformInterfaceMixin implements LimitedVideoRecorderPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final LimitedVideoRecorderPlatform initialPlatform = LimitedVideoRecorderPlatform.instance;

  test('$MethodChannelLimitedVideoRecorder is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelLimitedVideoRecorder>());
  });
}
