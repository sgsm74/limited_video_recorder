package ir.saeedqasemi.limited_video_recorder

import android.view.Surface

interface PreviewSurfaceProvider {
    fun onSurfaceAvailable(surface: Surface)
    fun onSurfaceDestroyed()
}
