class RecordingConfig {
  final int videoWidth;
  final int videoHeight;
  final int videoBitRate;
  final int frameRate;
  final int maxDuration;
  final int maxFileSize;

  const RecordingConfig({
    this.videoWidth = 1920,
    this.videoHeight = 1080,
    this.videoBitRate = 5 * 1000 * 1000,
    this.frameRate = 30,
    this.maxDuration = 0,
    this.maxFileSize = 10 * 1024 * 1024,
  });

  Map<String, dynamic> toMap() => {
    'videoWidth': videoWidth,
    'videoHeight': videoHeight,
    'videoBitRate': videoBitRate,
    'frameRate': frameRate,
    'maxDuration': maxDuration,
    'maxFileSize': maxFileSize,
  };
}
