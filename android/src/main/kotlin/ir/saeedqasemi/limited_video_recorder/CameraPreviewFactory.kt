package ir.saeedqasemi.limited_video_recorder

import android.content.Context
import android.view.View
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.plugin.common.StandardMessageCodec

class CameraPreviewFactory(
    private val messenger: BinaryMessenger
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    var currentPreviewSurface: android.view.Surface? = null

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        return CameraPreview(context, object : PreviewSurfaceProvider {
            override fun onSurfaceAvailable(surface: android.view.Surface) {
                currentPreviewSurface = surface
            }

            override fun onSurfaceDestroyed() {
                currentPreviewSurface = null
            }
        })
    }
}
