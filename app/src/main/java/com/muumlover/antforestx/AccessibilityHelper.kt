package com.muumlover.antforestx

import android.content.Context
import android.provider.Settings
import android.text.TextUtils

class AccessibilityHelper {
    companion object {
        /**
         * 检测辅助功能是否开启
         *
         * @param ct
         * @param serviceClass
         * @return boolean
         */
        fun hasServicePermission(ct: Context, serviceClass: Class<*>): Boolean {
            var ok = 0
            try {
                ok = Settings.Secure.getInt(
                    ct.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
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
                            return true
                        }
                    }
                }
            }
            return false
        }
    }
}