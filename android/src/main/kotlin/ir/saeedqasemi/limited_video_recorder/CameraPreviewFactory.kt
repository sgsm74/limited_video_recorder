package ir.saeedqasemi.limited_video_recorder

import android.content.Context
import android.view.View
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.plugin.common.StandardMessageCodec

/**
 * Factory class responsible for creating instances of [CameraPreview],
 * which are used to display the native Android camera preview inside Flutter.
 *
 * This class bridges the Flutter side and the Android side using Platform Views.
 *
 * @param messenger The BinaryMessenger used for communicating with Flutter.
 */
class CameraPreviewFactory(
    private val messenger: BinaryMessenger
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    /**
     * Holds the current preview [Surface] provided by [CameraPreview].
     * This surface can be used by the MediaRecorder or Camera2 API.
     */
    var currentPreviewSurface: android.view.Surface? = null

    /**
     * Called by the Flutter engine to create a native [PlatformView] for the camera preview.
     *
     * @param context The Android [Context].
     * @param viewId The unique identifier for this view.
     * @param args Any additional arguments passed from Flutter (not used here).
     *
     * @return A new instance of [CameraPreview] configured with a [PreviewSurfaceProvider].
     */
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        return CameraPreview(context, object : PreviewSurfaceProvider {

            // Called when the surface is created and ready to be used
            override fun onSurfaceAvailable(surface: android.view.Surface) {
                currentPreviewSurface = surface
            }

            // Called when the surface is destroyed or no longer valid
            override fun onSurfaceDestroyed() {
                currentPreviewSurface = null
            }
        })
    }
}
