package com.muumlover.antforestx

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import java.nio.ByteBuffer

internal class CaptureManager private constructor() {

    init {
        throw AssertionError("No instances.")
    }

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001
        private lateinit var intentData: Intent
        private lateinit var mMediaProjectionManager: MediaProjectionManager
        private var mMediaProjection: MediaProjection? = null
        private lateinit var mVirtualDisplay: VirtualDisplay
        private val metrics = DisplayMetrics()

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun fireScreenCaptureIntent(activity: Activity) {
            mMediaProjectionManager =
                activity.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            val intent = mMediaProjectionManager.createScreenCaptureIntent()
            activity.startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
        }

        fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            if (requestCode != REQUEST_MEDIA_PROJECTION) {
                return false
            }
            if (resultCode == Activity.RESULT_OK && data != null) {
                intentData = data
                mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data)
                return true
            }
            return false
        }

        fun getCapture(): Bitmap? {
            if (mMediaProjection == null) {
                Log.e("CaptureManager", "mMediaProjection is null.")
                return null
            }
            val mImageReader: ImageReader = ImageReader.newInstance(
                metrics.widthPixels, metrics.heightPixels, 0x1, 2
            )
            mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
                "ScreenCapture",
                metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.surface, null, null
            );

            val image: Image? = mImageReader.acquireLatestImage()

            if (image == null) {
                Log.e("CaptureManager", "image is null.")
                return null
            }
            val width: Int = image.width
            val height: Int = image.height
            val planes: Array<Image.Plane> = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride: Int = planes[0].pixelStride
            val rowStride: Int = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width
            var mBitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            mBitmap.copyPixelsFromBuffer(buffer)
            mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height)
            image.close()
            return mBitmap
        }
    }
}