import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class CameraPreview extends StatelessWidget {
  final int cameraId;

  const CameraPreview({super.key, required this.cameraId});

  @override
  Widget build(BuildContext context) {
    return AndroidView(
      viewType: 'camera_preview',
      layoutDirection: TextDirection.ltr,
      creationParams: {'cameraId': '$cameraId'},
      creationParamsCodec: const StandardMessageCodec(),
    );
  }
}
