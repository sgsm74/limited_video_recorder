package ir.saeedqasemi.limited_video_recorder

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import io.flutter.plugin.platform.PlatformView

/**
 * CameraPreview is a custom PlatformView that displays the live camera feed
 * inside a TextureView and prepares the surface for recording or previewing video.
 *
 * This class bridges Android's native camera system to Flutter via PlatformView.
 */
class CameraPreview(
    private val context: Context,
    private val surfaceProvider: PreviewSurfaceProvider
) : PlatformView, TextureView.SurfaceTextureListener {

    private val textureView: TextureView = TextureView(context)
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private lateinit var cameraId: String

    init {
        textureView.surfaceTextureListener = this
        startBackgroundThread()
        cameraId = cameraManager.cameraIdList[0]
    }

    /**
     * Returns the TextureView that displays the camera preview.
     */
    override fun getView(): TextureView = textureView

    /**
     * Disposes the camera and stops the background thread when the PlatformView is destroyed.
     */
    override fun dispose() {
        closeCamera()
        stopBackgroundThread()
    }

    /**
     * Starts a background thread for camera operations.
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    /**
     * Stops the background thread gracefully.
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Opens the camera and begins the preview session once permissions are granted.
     */
    private fun openCamera() {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, should request from Flutter side
                return
            }
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Starts the camera preview using the configured resolution and surface.
     */
    private fun startPreview() {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(1920, 1080) // Set preview resolution

        val surface = Surface(texture)

        // Notify that the surface is ready for recording/preview
        surfaceProvider.onSurfaceAvailable(surface)

        try {
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surface)

            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    previewRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    val previewRequest = previewRequestBuilder?.build()
                    captureSession?.setRepeatingRequest(previewRequest!!, null, backgroundHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    println("Camera configuration failed")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Closes the camera and releases all preview resources.
     */
    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null

        // Notify the provider that the surface is no longer available
        surfaceProvider.onSurfaceDestroyed()
    }

    /**
     * Called when the TextureView becomes available and the camera can be opened.
     */
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        openCamera()
    }

    /**
     * Called when the TextureView size changes. You can use this to adjust preview scaling.
     */
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // No-op
    }

    /**
     * Called when the TextureView is destroyed. Releases the camera and stops the background thread.
     */
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        closeCamera()
        stopBackgroundThread()
        return true
    }

    /**
     * Called when the content of the TextureView is updated.
     */
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // No-op
    }
}
