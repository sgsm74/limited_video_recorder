package ir.saeedqasemi.limited_video_recorder

import android.view.Surface

/**
 * Interface used for providing a [Surface] to consumers such as the MediaRecorder or camera session.
 *
 * This interface allows you to receive callbacks when a surface becomes available or is destroyed.
 * Typically used in combination with [CameraPreviewFactory] and [CameraPreview] to manage
 * the preview surface lifecycle.
 */
interface PreviewSurfaceProvider {

    /**
     * Called when the [Surface] is available and ready to be used for preview or recording.
     *
     * @param surface The available [Surface] object.
     */
    fun onSurfaceAvailable(surface: Surface)

    /**
     * Called when the previously provided [Surface] is no longer valid or has been destroyed.
     * Use this to clean up resources or notify dependent components.
     */
    fun onSurfaceDestroyed()
}
