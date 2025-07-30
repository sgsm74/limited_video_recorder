package ir.saeedqasemi.limited_video_recorder

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.core.app.ActivityCompat
import java.io.File
import android.content.Intent
import android.os.Looper
import androidx.core.content.ContextCompat

class LimitedVideoRecorderPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var context: Context? = null
    private var cameraDevice: CameraDevice? = null
    private var mediaRecorder: MediaRecorder? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraId: String = ""
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var config = RecordingConfig()
    lateinit var previewFactory: CameraPreviewFactory
    private var videoFilePath: String? = null
    private val TAG = "LimitedVideoRecorder"
    private var isRecording = false
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    private val PERMISSION_REQUEST_CODE = 1001
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "limited_video_recorder")
        channel.setMethodCallHandler(this)
        previewFactory = CameraPreviewFactory(binding.binaryMessenger)
        binding.platformViewRegistry.registerViewFactory("camera_preview", previewFactory)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startRecording" -> {
                config = RecordingConfig.fromCall(call)
                if (!hasPermissions(context!!)) {
                    requestPermissions()
                    result.error("PERMISSION_DENIED", "Camera and audio permissions are required", null)
                    return
                }
                val previewSurface = previewFactory.currentPreviewSurface
                if (previewSurface == null) {
                    result.error("NO_SURFACE", "Preview surface is not ready", null)
                    return
                }
                startCamera(result)
            }
            "stopRecording" -> {
                stopRecording(result)
            }
            "previewCamera" -> {
                launchCameraPreview(result)
            }
            else -> {
                Log.w(TAG, "Method not implemented: ${call.method}")
                result.notImplemented()
            }
        }
    }

    private fun startCamera(result: MethodChannel.Result) {
        if (!requestCameraPermissions(result)) return

        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0] // Assuming the first camera is the back camera, change if needed

        if (handlerThread == null || !handlerThread!!.isAlive) {
            handlerThread = HandlerThread("CameraBackground")
            handlerThread?.start()
            handler = Handler(handlerThread!!.looper)
            Log.d(TAG, "HandlerThread started")
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startRecording(result)
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                result.error("CAMERA_ERROR", "Camera open error: $error", null)
            }
        }, handler)
    }

    private fun startRecording(result: MethodChannel.Result) {
        if (isRecording) {
            result.error("ALREADY_RECORDING", "Recording is already in progress", null)
            return
        }
        isRecording = true
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        val file = context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let {
            File(it, fileName)
        } ?: run {
            result.error("FILE_ERROR", "Unable to get output file", null)
            return
        }
        if (mediaRecorder != null) {
            result.error("ALREADY_RECORDING", "Recording is already in progress", null)
            return
        }

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file.absolutePath)
            setVideoEncodingBitRate(config.videoBitRate)
            setVideoFrameRate(config.frameRate)
            setVideoSize(config.videoWidth, config.videoHeight)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setMaxFileSize(config.maxFileSize)
            if (config.maxDuration > 0) setMaxDuration(config.maxDuration)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    activity?.runOnUiThread {
                        stopRecording(null)
                        channel.invokeMethod("recordingComplete", file.absolutePath)
                    }
                }
            }
            try {
                prepare()
                Log.d(TAG, "MediaRecorder prepared")
            } catch (e: Exception) {
                Log.e(TAG, "MediaRecorder prepare failed: ${e.message}")
                result.error("PREPARE_FAILED", e.message, null)
                return
            }
        }
        videoFilePath = file.absolutePath
        val previewSurface = previewFactory.currentPreviewSurface
        if (previewSurface == null) {
            result.error("NO_SURFACE", "Preview surface is not ready", null)
            return
        }
        val recordingSurface = mediaRecorder!!.surface

        val surfaces = listOf(previewSurface, recordingSurface)

        cameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session

                val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                builder.addTarget(previewSurface)
                builder.addTarget(recordingSurface)

                session.setRepeatingRequest(builder.build(), null, handler)
                mediaRecorder?.start()

                result.success("Recording started")
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                result.error("SESSION_ERROR", "Failed to configure camera", null)
            }
        }, handler)
    }

    private fun stopRecording(result: MethodChannel.Result?) {
        if (mediaRecorder == null) {
            Log.d(TAG, "MediaRecorder is null, already stopped or not started")
            result?.success("Already stopped or not started")
            return
        }

        try {
            isRecording = false
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            Log.d(TAG, "MediaRecorder stopped and released")
        } catch (e: Exception) {
            videoFilePath?.let {
                Log.d(TAG, "Deleting video file: $it")
                File(it).delete()
            }
            Log.e(TAG, "Failed to stop media recorder: ${e.message}")
            result?.error("STOP_FAILED", "Failed to stop media recorder: ${e.message}", null)
            // Try to release other resources even if MediaRecorder stop failed
        } finally {
            try {
                captureSession?.apply {
                    stopRepeating()
                    abortCaptures()
                    close()
                }
                captureSession = null
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Failed to stop/close capture session: ${e.message}")
            }

            try {
                cameraDevice?.close()
                cameraDevice = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close camera device: ${e.message}")
            }

            handlerThread?.quitSafely()
            handlerThread = null
            handler = null

            Log.d(TAG, "All camera/recording resources released")
            result?.success(videoFilePath)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }
    private fun launchCameraPreview(result: MethodChannel.Result) {
        val intent = Intent(activity, CameraPreviewActivity::class.java)
        activity?.startActivity(intent)
        result.success("Preview launched")
    }

    private fun onVideoRecorded(result: MethodChannel.Result?, path: String?) {
        channel.invokeMethod("recordingComplete", path)
    }

    private fun requestCameraPermissions(result: MethodChannel.Result): Boolean {
        if (activity == null) {
            result.error("NO_ACTIVITY", "Activity is not attached", null)
            return false
        }

        val missingPermissions = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.CAMERA)
        }

        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.RECORD_AUDIO)
        }

        return if (missingPermissions.isEmpty()) {
            true
        } else {
            ActivityCompat.requestPermissions(activity!!, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            result.error("PERMISSION_REQUEST", "Requesting permissions", null)
            false
        }
    }

    private fun hasPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(activity!!, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }
}

// config class
data class RecordingConfig(
    val videoWidth: Int = 1920,
    val videoHeight: Int = 1080,
    val videoBitRate: Int = 5_000_000,
    val frameRate: Int = 30,
    val maxFileSize: Long = 10 * 1024 * 1024,
    val maxDuration: Int = 0
) {
    companion object {
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