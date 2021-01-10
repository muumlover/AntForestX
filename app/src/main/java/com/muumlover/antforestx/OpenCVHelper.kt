package com.muumlover.antforestx

import android.content.Context
import android.os.Looper
import android.support.annotation.Nullable
import android.util.Log
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint





class OpenCVHelper {
    companion object {
        fun init(AppContext: Context) {
            val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(AppContext) {
                override fun onManagerConnected(status: Int) {
                    when (status) {
                        LoaderCallbackInterface.SUCCESS -> {
                            Log.i("OpenCV", "OpenCV loaded successfully")
                            var imageMat = Mat()
                        }
                        else -> {
                            super.onManagerConnected(status)
                        }
                    }
                }
            }

            if (!OpenCVLoader.initDebug()) {
                Log.d(
                    "OpenCV",
                    "Internal OpenCV library not found. Using OpenCV Manager for initialization"
                )
                OpenCVLoader.initAsync(
                    OpenCVLoader.OPENCV_VERSION_3_0_0,
                    AppContext,
                    mLoaderCallback
                )
            } else {
                Log.d("OpenCV", "OpenCV library found inside package. Using it!")
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
            }
        }

        interface InitializeCallback {
            fun onInitFinish()
        }

        private const val LOG_TAG = "OpenCVHelper"
        private var sInitialized = false

        fun newMatOfPoint(mat: Mat?): MatOfPoint? {
            return MatOfPoint(mat)
        }

        fun release(@Nullable mat: MatOfPoint?) {
            if (mat == null) return
            mat.release()
        }

        fun release(@Nullable mat: Mat?) {
            if (mat == null) return
            mat.release()
        }

        @Synchronized
        fun isInitialized(): Boolean {
            return sInitialized
        }

        @Synchronized
        fun initIfNeeded(
            context: Context?,
            callback: InitializeCallback
        ) {
            if (sInitialized) {
                callback.onInitFinish()
                return
            }
            sInitialized = true
            if (Looper.getMainLooper() === Looper.myLooper()) {
                Thread(Runnable {
                    OpenCVLoader.initDebug()
                    callback.onInitFinish()
                }).start()
            } else {
                OpenCVLoader.initDebug()
                callback.onInitFinish()
            }
        }
    }
}
