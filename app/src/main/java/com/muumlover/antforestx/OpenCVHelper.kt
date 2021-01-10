package com.muumlover.antforestx

import android.content.Context
import android.util.Log
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat


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
    }
}
