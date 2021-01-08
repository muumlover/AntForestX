package com.muumlover.antforestx

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log


class MainActivity : AppCompatActivity() {
    private val TAG = javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        Log.d(TAG, "请求屏幕截图权限")
        CaptureManager.fireScreenCaptureIntent(this@MainActivity)
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
