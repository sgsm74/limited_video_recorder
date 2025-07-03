/**
 * Configuration class for video recording parameters.
 *
 * Used to pass recording preferences from Flutter to the Android native plugin.
 *
 * @property videoWidth Width of the recorded video.
 * @property videoHeight Height of the recorded video.
 * @property videoBitRate Bitrate in bits per second (e.g. 5_000_000 = 5 Mbps).
 * @property frameRate Number of video frames per second.
 * @property maxFileSize Optional limit on output file size in bytes.
 * @property maxDuration Optional maximum recording duration in milliseconds.
 */
data class RecordingConfig(
    val videoWidth: Int = 1920,
    val videoHeight: Int = 1080,
    val videoBitRate: Int = 5_000_000,
    val frameRate: Int = 30,
    val maxFileSize: Long = 10 * 1024 * 1024,
    val maxDuration: Int = 0
) {
    companion object {
        /**
         * Creates a [RecordingConfig] instance from a [MethodCall].
         *
         * Used to receive settings from the Dart side.
         */
        fun fromCall(call: MethodCall): RecordingConfig = RecordingConfig(
            videoWidth = call.argument<Int>("videoWidth") ?: 1920,
            videoHeight = call.argument<Int>("videoHeight") ?: 1080,
            videoBitRate = call.argument<Int>("videoBitRate") ?: 5_000_000,
            frameRate = call.argument<Int>("frameRate") ?: 30,
            maxFileSize = (call.argument<Int>("maxFileSize") ?: 10_000_000).toLong(),
            maxDuration = call.argument<Int>("maxDuration") ?: 0
        )
    }
}
