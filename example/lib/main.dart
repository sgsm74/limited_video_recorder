import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:limited_video_recorder/camera_description.dart';
import 'package:limited_video_recorder/camera_preview.dart';
import 'package:limited_video_recorder/limited_video_recorder_config.dart';
import 'package:limited_video_recorder/limited_video_recorder_controller.dart';
import 'package:video_player/video_player.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(title: 'Limited Video Recorder', home: const VideoRecorderPage());
  }
}

class VideoRecorderPage extends StatefulWidget {
  const VideoRecorderPage({super.key});

  @override
  State<VideoRecorderPage> createState() => _VideoRecorderPageState();
}

class _VideoRecorderPageState extends State<VideoRecorderPage> {
  bool isRecording = false;
  String? videoPath;
  String statusText = "Ready to record";
  VideoPlayerController? _controller;
  final recorder = LimitedVideoRecorderController();
  List<CameraDescription> cameras = [];
  Duration? videoDuration;
  int? videoWidth;
  int? videoHeight;
  int? videoFileSize;
  DateTime? videoModifiedDate;

  Future<void> startRecording() async {
    try {
      final config = RecordingConfig(videoWidth: 1280, videoHeight: 720, maxFileSize: 10 * 1024 * 1024, maxDuration: 5 * 1000, cameraId: 1);
      await recorder.start(config: config);

      setState(() {
        isRecording = true;
        statusText = "Recording...";
        videoPath = null;
        videoDuration = null;
        videoWidth = null;
        videoHeight = null;
        videoFileSize = null;
        videoModifiedDate = null;
        _controller?.dispose();
        _controller = null;
      });
    } catch (e) {
      setState(() {
        statusText = "Error starting recording: $e";
      });
    }
  }

  Future<void> stopRecording() async {
    try {
      final p = await recorder.stop();
      if (p != null) {
        await _loadVideo(p);
      }
      setState(() {
        isRecording = false;
        statusText = "Recording stopped.";
      });
    } catch (e) {
      setState(() {
        statusText = "Error stopping recording: $e";
      });
    }
  }

  Future<void> _loadVideo(String path) async {
    final file = File(path);
    if (!await file.exists()) return;

    final stat = await file.stat();

    final controller = VideoPlayerController.file(file);
    await controller.initialize();

    setState(() {
      videoPath = path;
      _controller = controller;
      videoDuration = controller.value.duration;
      videoWidth = controller.value.size.width.toInt();
      videoHeight = controller.value.size.height.toInt();
      videoFileSize = stat.size;
      videoModifiedDate = stat.modified;
      statusText = "Recording complete at: $path";
    });
  }

  Future<void> listCameras() async {
    try {
      cameras = await recorder.listAvailableCameras();
    } catch (e) {
      setState(() {
        statusText = "Error stopping recording: $e";
      });
    }
  }

  @override
  void initState() {
    super.initState();
    listCameras();
    recorder.onRecordingComplete((String path) {
      _loadVideo(path);
    });
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  String _formatBytes(int bytes) {
    const suffixes = ["B", "KB", "MB", "GB"];
    var i = 0;
    double size = bytes.toDouble();
    while (size >= 1024 && i < suffixes.length - 1) {
      size /= 1024;
      i++;
    }
    return "${size.toStringAsFixed(2)} ${suffixes[i]}";
  }

  String _formatDuration(Duration duration) {
    String twoDigits(int n) => n.toString().padLeft(2, "0");
    final minutes = twoDigits(duration.inMinutes.remainder(60));
    final seconds = twoDigits(duration.inSeconds.remainder(60));
    return "${duration.inHours > 0 ? '${twoDigits(duration.inHours)}:' : ''}$minutes:$seconds";
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Limited Video Recorder')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Text(statusText, style: const TextStyle(fontSize: 16)),
            const SizedBox(height: 20),
            if (videoPath != null && _controller != null)
              Expanded(
                child: Column(
                  children: [
                    AspectRatio(aspectRatio: _controller!.value.aspectRatio, child: VideoPlayer(_controller!)),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        IconButton(
                          icon: Icon(_controller!.value.isPlaying ? Icons.pause : Icons.play_arrow),
                          onPressed: () {
                            setState(() {
                              if (_controller!.value.isPlaying) {
                                _controller!.pause();
                              } else {
                                _controller!.play();
                              }
                            });
                          },
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text("Path: $videoPath", style: const TextStyle(fontSize: 12)),
                    Text(
                      "Duration: ${videoDuration != null ? _formatDuration(videoDuration!) : '-'}",
                      style: const TextStyle(fontSize: 12),
                    ),
                    Text("Resolution: ${videoWidth ?? '-'} x ${videoHeight ?? '-'}", style: const TextStyle(fontSize: 12)),
                    Text("File Size: ${videoFileSize != null ? _formatBytes(videoFileSize!) : '-'}", style: const TextStyle(fontSize: 12)),
                    Text("Last Modified: ${videoModifiedDate ?? '-'}", style: const TextStyle(fontSize: 12)),
                  ],
                ),
              ),
            if (videoPath == null) Expanded(child: CameraPreview(cameraId: 1)),
            const SizedBox(height: 10),
            if (!isRecording) ElevatedButton(onPressed: startRecording, child: const Text('Start Recording')),
            if (isRecording && videoPath == null) ElevatedButton(onPressed: stopRecording, child: const Text('Stop Recording')),
          ],
        ),
      ),
    );
  }
}
