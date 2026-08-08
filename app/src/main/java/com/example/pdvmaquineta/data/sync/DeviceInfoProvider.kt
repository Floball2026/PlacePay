package com.example.pdvmaquineta.data.sync

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.pdvmaquineta.data.sync.dto.DeviceInfoDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun androidId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    fun build(): DeviceInfoDto {
        val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkg.longVersionCode.toInt()
        } else {
            pkg.versionCode
        }
        return DeviceInfoDto(
            androidId = androidId(),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            appVersion = pkg.versionName,
            appVersionCode = versionCode
        )
    }
}
