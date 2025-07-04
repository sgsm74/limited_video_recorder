class CameraDescription {
  final String id;
  final String lensFacing;
  final int sensorOrientation;

  CameraDescription({required this.id, required this.lensFacing, required this.sensorOrientation});

  factory CameraDescription.fromMap(Map<String, dynamic> map) {
    return CameraDescription(id: map['id'], lensFacing: map['lensFacing'], sensorOrientation: map['sensorOrientation']);
  }

  @override
  String toString() => 'Camera(id: $id, lensFacing: $lensFacing, orientation: $sensorOrientation)';
}
