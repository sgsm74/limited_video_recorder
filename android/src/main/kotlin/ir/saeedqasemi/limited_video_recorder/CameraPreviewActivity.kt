package ir.saeedqasemi.limited_video_recorder

import android.app.Activity
import android.content.Context
import android.hardware.camera2.*
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import android.graphics.SurfaceTexture
import android.view.ViewGroup.LayoutParams.MATCH_PARENT

/**
 * CameraPreviewActivity is a standalone Android Activity that opens the default camera
 * and displays a fullscreen live preview using TextureView.
 *
 * This class is useful for quickly testing camera preview functionality.
 */
class CameraPreviewActivity : Activity() {

    private lateinit var textureView: TextureView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String

    /**
     * Called when the activity is first created.
     * Initializes the TextureView and sets the camera preview once the surface is ready.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create and set up the TextureView to fill the screen
        textureView = TextureView(this)
        textureView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setContentView(textureView)

        // Start the camera once the surface is available
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    /**
     * Opens the device's default camera and starts the preview.
     */
    private fun openCamera() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]

        // Check permission before opening camera
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, null)
    }

    /**
     * Configures the preview surface and starts sending camera frames to it.
     */
    private fun startPreview() {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(1920, 1080)
        val surface = Surface(surfaceTexture)

        val previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(surface)

        cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                session.setRepeatingRequest(previewRequestBuilder.build(), null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                // Handle configuration failure (e.g. notify user)
                Log.e("CameraPreview", "Camera configuration failed")
            }
        }, null)
    }

    /**
     * Releases the camera and its resources when the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        captureSession?.close()
        cameraDevice?.close()
    }
}
