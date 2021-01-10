package com.muumlover.antforestx

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        OpenCVHelper.init(this)
        refreshView()
        screenBaseInfo
    }

    override fun onResume() {
        super.onResume()
        refreshView()
    }

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0

    /**
     * 获取屏幕基本信息
     */
    private val screenBaseInfo: Unit
        get() {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            mScreenWidth = metrics.widthPixels
            mScreenHeight = metrics.heightPixels
            mScreenDensity = metrics.densityDpi
        }


    private fun refreshView() {
        swScreenShot.setOnClickListener(this)
        swAccessibilityService.setOnClickListener(this)
        btnOpenAntForest.setOnClickListener(this)

        if (AntForestService.isStart()) {
            txtAntForestService.text = "蚂蚁森林服务正在运行"
        } else {
            txtAntForestService.text = "蚂蚁森林服务已经停止"
        }

        swAccessibilityService.isChecked =
            AccessibilityHelper.hasServicePermission(
                applicationContext,
                AntForestService::class.java
            )

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            swAccessibilityService.id -> {
                if (swAccessibilityService.isChecked) {
                    try {
                        Log.d(TAG, "尝试跳转到无障碍设置页面")
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) {
                        Log.d(TAG, "无法跳转到无障碍设置页面")
                        e.printStackTrace()
                        Log.d(TAG, "跳转到系统设置页面")
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                } else {
                    val service = Intent(this, AntForestService::class.java)

                    stopService(service)
                }
            }
            swScreenShot.id -> {
                if (swScreenShot.isChecked) {
                    startScreenRecord()
                } else {
                    stopScreenRecord()
                }
            }
            R.id.btnOpenAntForest -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("alipayqr://platformapi/startapp?saId=60000002")
                    )
                )
            }
        }
        refreshView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == RESULT_OK) {
                    //获得录屏权限，启动Service进行录制
                    val intent = Intent(this, CaptureService::class.java)
                    intent.putExtra("resultCode", resultCode)
                    intent.putExtra("resultData", data)
                    intent.putExtra("mScreenWidth", mScreenWidth)
                    intent.putExtra("mScreenHeight", mScreenHeight)
                    intent.putExtra("mScreenDensity", mScreenDensity)
                    startService(intent)
                    Log.d(TAG, "已经获取到屏幕截图权限")
                    //Toast.makeText(this, "成功开启服务", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "服务开启失败,无法截屏，退出", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    //start screen record
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun startScreenRecord() {
        //Manages the retrieval of certain types of MediaProjection tokens.
        val mediaProjectionManager =
            this.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        //Returns an Intent that must passed to startActivityForResult() in order to start screen capture.
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        this.startActivityForResult(permissionIntent, REQUEST_MEDIA_PROJECTION)
    }

    //stop screen record.
    private fun stopScreenRecord() {
        val service = Intent(this, CaptureService::class.java)
        this.stopService(service)
    }

    companion object {
        private var REQUEST_MEDIA_PROJECTION = 1001
    }
}
