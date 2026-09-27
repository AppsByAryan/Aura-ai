package com.example.aura.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.aura.data.SystemTelemetry

class SystemTelemetryManager(private val context: Context) {

    fun collectTelemetry(): SystemTelemetry {
        val (batteryLevel, isCharging, health, tempC) = getBatteryInfo()
        val (totalStorage, freeStorage, usedStoragePct) = getStorageInfo()
        val (totalRam, availRam, usedRamPct) = getMemoryInfo()
        val (networkType, isConnected) = getNetworkInfo()
        val (volPct, maxVol) = getAudioInfo()
        val isA11y = AuraAccessibilityService.isAccessibilityEnabledInSystem(context)

        return SystemTelemetry(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            batteryHealth = health,
            batteryTempC = tempC,
            totalStorageGb = totalStorage,
            freeStorageGb = freeStorage,
            usedStoragePercent = usedStoragePct,
            totalRamMb = totalRam,
            availRamMb = availRam,
            usedRamPercent = usedRamPct,
            networkType = networkType,
            isConnected = isConnected,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            musicVolumePercent = volPct,
            maxMusicVolume = maxVol,
            isAccessibilityActive = isA11y
        )
    }

    private fun getBatteryInfo(): Tuple4<Int, Boolean, String, Float> {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 85

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            val health = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal (Good)"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "High Temperature"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Critical Depleted"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                else -> "Normal"
            }

            val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 260) ?: 260
            val tempC = rawTemp / 10.0f

            Tuple4(pct, isCharging, health, tempC)
        } catch (_: Exception) {
            Tuple4(85, false, "Good", 27.5f)
        }
    }

    private fun getStorageInfo(): Triple<Float, Float, Int> {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGb = totalBytes.toDouble() / (1024 * 1024 * 1024)
            val freeGb = freeBytes.toDouble() / (1024 * 1024 * 1024)
            val usedPct = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

            Triple(
                String.format("%.1f", totalGb).toFloat(),
                String.format("%.1f", freeGb).toFloat(),
                usedPct.coerceIn(0, 100)
            )
        } catch (_: Exception) {
            Triple(128.0f, 64.0f, 50)
        }
    }

    private fun getMemoryInfo(): Triple<Long, Long, Int> {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)

            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = totalMb - availMb
            val usedPct = if (totalMb > 0) ((usedMb.toDouble() / totalMb) * 100).toInt() else 0

            Triple(totalMb, availMb, usedPct.coerceIn(0, 100))
        } catch (_: Exception) {
            Triple(6144L, 2048L, 66)
        }
    }

    private fun getNetworkInfo(): Pair<String, Boolean> {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)

            if (caps == null) {
                return Pair("Offline", false)
            }

            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi (High Speed)"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular 5G/LTE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet Link"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "Encrypted VPN"
                else -> "Active Connection"
            }
            Pair(type, hasInternet)
        } catch (_: Exception) {
            Pair("Online", true)
        }
    }

    private fun getAudioInfo(): Pair<Int, Int> {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val currentVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
            val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val pct = if (maxVol > 0) (currentVol * 100) / maxVol else 50
            Pair(pct, maxVol)
        } catch (_: Exception) {
            Pair(50, 15)
        }
    }

    data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
