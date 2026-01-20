package com.celdy.groufr.data.device

import android.content.Context
import android.os.Build
import com.celdy.groufr.data.auth.DeviceInfo
import com.celdy.groufr.data.storage.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenStore
) {
    fun buildDeviceInfo(): DeviceInfo {
        val versionName = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            ?: "0.0.0"
        return DeviceInfo(
            uuid = tokenStore.getOrCreateDeviceId(),
            platform = "android",
            name = Build.MODEL ?: "Android",
            appVersion = versionName,
            osVersion = Build.VERSION.RELEASE ?: "unknown"
        )
    }
}
