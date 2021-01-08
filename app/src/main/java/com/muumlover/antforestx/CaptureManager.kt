package com.muumlover.antforestx

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build

internal class CaptureManager private constructor() {

    init {
        throw AssertionError("No instances.")
    }

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001
        private lateinit var intentData: Intent

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun fireScreenCaptureIntent(activity: Activity) {
            val manager =
                activity.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = manager.createScreenCaptureIntent()
            activity.startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)

        }

        fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            if (requestCode != REQUEST_MEDIA_PROJECTION) {
                return false
            }
            if (resultCode == Activity.RESULT_OK && data != null) {
                intentData = data
                return true
            }
            return false
        }
    }
}