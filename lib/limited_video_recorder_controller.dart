import 'package:flutter/services.dart';
import 'camera_description.dart';
import 'limited_video_recorder_config.dart';

class LimitedVideoRecorderController {
  static const MethodChannel _channel = MethodChannel('limited_video_recorder');
  Function(String path)? _onComplete;

  bool _isRecording = false;

  LimitedVideoRecorderController() {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  Future<String?> start({RecordingConfig config = const RecordingConfig()}) async {
    if (_isRecording) return null;

    final result = await _channel.invokeMethod<String>('startRecording', config.toMap());
    _isRecording = true;
    return result;
  }

  Future<String?> stop() async {
    if (!_isRecording) return null;

    final result = await _channel.invokeMethod<String>('stopRecording');
    _isRecording = false;
    return result;
  }

  void onRecordingComplete(Function(String path) callback) {
    _onComplete = callback;
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    if (call.method == 'recordingComplete') {
      final path = call.arguments as String?;
      if (path != null && _onComplete != null) {
        _onComplete!(path);
      }
    }
  }

  void dispose() {
    _onComplete = null;
  }

  Future<List<CameraDescription>> listAvailableCameras() async {
    final List<dynamic> cameras = await _channel.invokeMethod('listCameras');
    return cameras.map((cam) => CameraDescription.fromMap(Map<String, dynamic>.from(cam))).toList();
  }

  bool get isRecording => _isRecording;
}
