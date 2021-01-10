package com.muumlover.antforestx

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder


class CaptureService : Service() {
    private var resultCode = 0
    private var resultData: Intent? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            resultCode = intent.getIntExtra("resultCode", -1)
            resultData = intent.getParcelableExtra("resultData")
            val mScreenWidth = intent.getIntExtra("mScreenWidth", 0)
            val mScreenHeight = intent.getIntExtra("mScreenHeight", 0)
            val mScreenDensity = intent.getIntExtra("mScreenDensity", 0)
            mediaProjection = createMediaProjection()
            mImageReader = ImageReader.newInstance(
                mScreenWidth,
                mScreenHeight,
                PixelFormat.RGBA_8888,
                1
            )
            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "ScreenShotDemo",
                mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader!!.surface, null, null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_NOT_STICKY
    }

    //createMediaProjection
    private fun createMediaProjection(): MediaProjection {
        return (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            .getMediaProjection(resultCode, resultData!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (image != null) {
            image!!.close()
            image = null
        }
        if (mImageReader != null) {
            mImageReader!!.close()
            mImageReader = null
        }
        if (virtualDisplay != null) {
            virtualDisplay!!.release()
            virtualDisplay = null
        }
        if (mediaProjection != null) {
            mediaProjection!!.stop()
            mediaProjection = null
        }
        //Toast.makeText(getApplicationContext(), "截屏service已注销", Toast.LENGTH_SHORT).show();
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        var mImageReader: ImageReader? = null
        var image: Image? = null
        fun getScreenShot(): Bitmap {
            mImageReader!!.surface
            image = mImageReader!!.acquireLatestImage()
            val width = image!!.width
            val height = image!!.height
            val planes = image!!.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width
            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image!!.close()
            return Bitmap.createBitmap(bitmap, 0, 0, width, height)
        }
    }
}