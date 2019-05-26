package com.muumlover.antforestx

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasServicePermission(applicationContext, AntService::class.java) && !AntService.isStart()) {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
                e.printStackTrace()
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
        var ok = 0
        try {
            ok = Settings.Secure.getInt(ct.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
        }

        val ms = TextUtils.SimpleStringSplitter(':')
        if (ok == 1) {
            val settingValue =
                Settings.Secure.getString(ct.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                ms.setString(settingValue)
                while (ms.hasNext()) {
                    val accessibilityService = ms.next()
                    if (accessibilityService.contains(serviceClass.simpleName)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
