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
import ir.saeedqasemi.limited_video_recorder.RecordingConfig

class LimitedVideoRecorderPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    // Communication channel between Dart and Android
    private lateinit var channel: MethodChannel

    // Flutter Activity & Context references
    private var activity: Activity? = null
    private var context: Context? = null

    // Camera and recording components
    private var cameraDevice: CameraDevice? = null
    private var mediaRecorder: MediaRecorder? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var previewFactory: CameraPreviewFactory

    private var videoFilePath: String? = null
    private var isRecording = false

    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var config = RecordingConfig()
    private var cameraId: String = ""
    private val TAG = "LimitedVideoRecorder"

    // Called when the plugin is attached to the Flutter engine
    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "limited_video_recorder")
        channel.setMethodCallHandler(this)

        // Register camera preview platform view
        previewFactory = CameraPreviewFactory(binding.binaryMessenger)
        binding.platformViewRegistry.registerViewFactory("camera_preview", previewFactory)
    }

    // Handles incoming method calls from Dart
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startRecording" -> {
                config = RecordingConfig.fromCall(call)
                val previewSurface = previewFactory.currentPreviewSurface
                if (previewSurface == null) {
                    result.error("NO_SURFACE", "Preview surface is not ready", null)
                    return
                }
                startCamera(result)
            }
            "stopRecording" -> stopRecording(result)
            "previewCamera" -> launchCameraPreview(result)
            "listCameras" -> listAvailableCameras(result)
            else -> {
                Log.w(TAG, "Method not implemented: ${call.method}")
                result.notImplemented()
            }
        }
    }

    /**
     * Starts the camera and prepares for recording.
     */
    private fun startCamera(result: MethodChannel.Result) {
        if (!requestCameraPermissions(result)) return

        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = config.cameraId.toString()



        if (handlerThread == null || !handlerThread!!.isAlive) {
            handlerThread = HandlerThread("CameraBackground").apply { start() }
            handler = Handler(handlerThread!!.looper)
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

    /**
     * Starts video recording using MediaRecorder with given config.
     */
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
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                ) {
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
                builder.addTarget(previewSurface!!)
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

    /**
     * Stops recording and releases all camera/recorder resources.
     */
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
    /**
    * Retrieves a list of available camera devices on the device.
    *
    * This method queries the system camera manager and builds a list of
    * [CameraDescription] objects representing each available camera.
    *
    * You can use the camera ID returned here to start a recording with a specific camera.
    *
    * @return A list of [CameraDescription]s with ID, lens facing direction, and sensor orientation.
    * @throws CameraAccessException if the camera access fails.
    */
    private fun listAvailableCameras(result: MethodChannel.Result) {
        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            result.error("NO_CAMERA_MANAGER", "CameraManager not available", null)
            return
        }

        val cameraList = mutableListOf<Map<String, Any>>()

        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

                val cameraInfo = mapOf(
                    "id" to id,
                    "lensFacing" to when (lensFacing) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "front"
                        CameraCharacteristics.LENS_FACING_BACK -> "back"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
                        else -> "unknown"
                    },
                    "sensorOrientation" to (orientation ?: 0)
                )

                cameraList.add(cameraInfo)
            }

            result.success(cameraList)
        } catch (e: Exception) {
            result.error("CAMERA_LIST_ERROR", e.message, null)
        }
    }


    /**
     * Launches a full-screen native Android preview activity (optional).
     */
    private fun launchCameraPreview(result: MethodChannel.Result) {
        val intent = Intent(activity, CameraPreviewActivity::class.java)
        activity?.startActivity(intent)
        result.success("Preview launched")
    }

    /**
     * Checks and requests camera/audio permissions.
     */
    private fun requestCameraPermissions(result: MethodChannel.Result): Boolean {
        if (activity == null) {
            result.error("NO_ACTIVITY", "Activity is not attached", null)
            return false
        }
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context!!, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            result.error("PERMISSION_DENIED", "Camera or audio permission not granted", null)
            return false
        }
        return true
    }

    // Flutter activity lifecycle handlers
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

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }
}
