package com.muumlover.antforestx

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()

    }

    private fun initView() {
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)
        val button5 = findViewById<Button>(R.id.button5)

        button3.setOnClickListener(this)
        button4.setOnClickListener(this)
        button5.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.button3 -> {
                if (!hasServicePermission(
                        applicationContext,
                        AntService::class.java
                    ) && !AntService.isStart()
                ) {
                    try {
                        Log.d(TAG, "尝试跳转到无障碍设置页面")
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) {
                        Log.d(TAG, "无法跳转到无障碍设置页面")
                        e.printStackTrace()
                        Log.d(TAG, "跳转到系统设置页面")
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            }
            R.id.button4 -> {
                Log.d(TAG, "请求屏幕截图权限")
                CaptureManager.fireScreenCaptureIntent(this@MainActivity)
            }
            R.id.button5 -> {
                Log.d(TAG, "请求屏幕截图权限")
                val bmp_image = CaptureManager.getCapture()
                if (bmp_image == null) {
                    Log.d(TAG, "没有获取到截屏")
                    return
                }
            }
        }
    }

    /**
     * 检测辅助功能是否开启
     *
     * @param ct
     * @param serviceClass
     * @return boolean
     */
    private fun hasServicePermission(ct: Context, serviceClass: Class<*>): Boolean {
        Log.d(TAG, "检测无障碍权限是否开启")
        var ok = 0
        try {
            ok = Settings.Secure.getInt(ct.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
        }

        val ms = TextUtils.SimpleStringSplitter(':')
        if (ok == 1) {
            val settingValue =
                Settings.Secure.getString(
                    ct.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
            if (settingValue != null) {
                ms.setString(settingValue)
                while (ms.hasNext()) {
                    val accessibilityService = ms.next()
                    if (accessibilityService.contains(serviceClass.simpleName)) {
                        Log.d(TAG, "无障碍权限已开启")
                        return true
                    }
                }
            }
        }
        Log.d(TAG, "无障碍权限未开启")
        return false
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CaptureManager.REQUEST_MEDIA_PROJECTION -> {
                if (CaptureManager.handleActivityResult(requestCode, resultCode, data)) {
                    Log.d(TAG, "已经获取到屏幕截图权限")
                }
            }
        }
    }
}
