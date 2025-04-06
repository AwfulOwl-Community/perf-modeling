package com.example.workerprocessmanager

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException


data class BatteryStatus(val status: Int, val plug: Int, val level: Int, val scale: Int, val batteryPct: Int, val health: Int = -1, val voltage: Int = -1, val current: Int = -1, val power : Double = 0.0, val temperature: Int = -1) {
    override fun toString(): String {
        return "BatteryStatus(status=$status, plug=$plug, level=$level, scale=$scale, batteryPct=$batteryPct, health=$health, voltage=$voltage, current=$current, power=$power, temperature=$temperature)"
    }
}

data class NetworkStatus(val status: Int, val downloads: Int, val uploads: Int) {
    override fun toString(): String {
        return "NetworkStatus(status=$status, downloads=$downloads, uploads=$uploads)"
    }
}

data class MemoryStatus(val totalMemory: Long, val availableMemory: Long, val lowMemory: Boolean) {
    override fun toString(): String {
        return "MemoryStatus(totalMemory=$totalMemory, availableMemory=$availableMemory, lowMemory=$lowMemory)"
    }
}

enum class NetworkStatusEnum(val status: Int) {
    WIFI(1),
    MOBILE(2),
    NONE(0),
    BLUETOOTH(3),
    ETHERNET(4),
    VPN(5)
}

class CpuUsageTracer(private val context: Context) {
    private val appContext: Context = context.applicationContext

    companion object { // Singleton pattern
        private var instance: CpuUsageTracer? = null
        @Synchronized
        fun getInstance(context: Context): CpuUsageTracer? {
            if (instance == null) {
                instance = CpuUsageTracer(context)
            }
            return instance
        }
    }

    // Get CPU Frequency, Memory Usage, Battery Status, Network Status.
    private val batteryManager: BatteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    fun isCharging(context: Context): Boolean {
        return batteryManager.isCharging
    }



    private fun checkChargeBatteryStatus(): BatteryStatus {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = appContext.registerReceiver(null, filter)
        if (batteryStatus != null) {
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            var plug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            var level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            var scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            var health: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            var voltage: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            var current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW); // 单位：微安(μA)
            val temperature: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val batteryPct: Int
            val power: Int
            if (level == -1 || scale == -1) {
                batteryPct = -1
            }
            else {
                batteryPct = ((level / scale.toFloat()) * 100).toInt()
            }
            if (current == 0) {
                power = 0
            } else {
                power = (current * voltage) / 1000000 // Convert to mW
            }
            return BatteryStatus(status, plug, level, scale, batteryPct, health, voltage, current, power.toDouble(), temperature)
        }
        return BatteryStatus(-1, -1, -1, -1, -1, -1, -1, -1, 0.0, -1)
    }



    fun checkNetwork(context: Context): NetworkStatus {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
                ?: return NetworkStatus(NetworkStatusEnum.NONE.status, 0, 0)
            val capabilities = cm.getNetworkCapabilities(network)
            if(capabilities == null) {
                return NetworkStatus(NetworkStatusEnum.NONE.status, 0, 0)
            }
            if(!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) { // not connected
                return NetworkStatus(NetworkStatusEnum.NONE.status, 0, 0)
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return NetworkStatus(NetworkStatusEnum.WIFI.status, capabilities.linkDownstreamBandwidthKbps, capabilities.linkUpstreamBandwidthKbps)
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return NetworkStatus(NetworkStatusEnum.MOBILE.status, capabilities.linkDownstreamBandwidthKbps, capabilities.linkUpstreamBandwidthKbps)
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return NetworkStatus(NetworkStatusEnum.VPN.status, capabilities.linkDownstreamBandwidthKbps, capabilities.linkUpstreamBandwidthKbps)
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                return NetworkStatus(NetworkStatusEnum.BLUETOOTH.status, capabilities.linkDownstreamBandwidthKbps, capabilities.linkUpstreamBandwidthKbps)
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return NetworkStatus(NetworkStatusEnum.ETHERNET.status, capabilities.linkDownstreamBandwidthKbps, capabilities.linkUpstreamBandwidthKbps)
            } else {
                return NetworkStatus(NetworkStatusEnum.NONE.status, 0, 0)
            }

        } else {
            return NetworkStatus(NetworkStatusEnum.NONE.status, 0, 0)
        }
    }


    fun getCpuFreq(cpuCore: Int): String {
        val path = "/sys/devices/system/cpu/cpu$cpuCore/cpufreq/scaling_cur_freq"
        try {
            val reader = BufferedReader(FileReader(path))
            val line = reader.readLine()
            reader.close()
            return line?.trim { it <= ' ' } ?: "N/A"
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "N/A"
    }

    fun getFreqForAllCores(): List<Double> {
        val cpuFreqs = mutableListOf<Double>()
        val cpuCores = Runtime.getRuntime().availableProcessors()
        for (i in 0 until cpuCores) {
            val freq = getCpuFreq(i)
            if (freq != "N/A") {
                try {
                    cpuFreqs.add(freq.toDouble() / 1000) // Convert to MHz
                } catch (e: NumberFormatException) {
                    cpuFreqs.add(0.0)
                }
            } else {
                cpuFreqs.add(0.0)
            }
        }
        return cpuFreqs
    }

    fun getCpuInfo(): String {
        val cpuInfo = StringBuilder()
        try {
            BufferedReader(FileReader("/proc/cpuinfo")).use { br ->
                var line: String?
                while ((br.readLine().also { line = it }) != null) {
                    cpuInfo.append(line).append("\n")
                }
            }
        } catch (e: IOException) {
            return "CPU info not available"
        }
        return cpuInfo.toString()
    }

    fun getMemoryInfo(context: Context): MemoryStatus {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem
        val lowMemory = memoryInfo.lowMemory
        val memoryStatus = MemoryStatus(totalMemory, availableMemory, lowMemory)
        return memoryStatus
    }

    public fun logCPUInfo() {
        val cpuInfo = getCpuInfo()
        WorkerProcessService.logToFile.logToFile("CPUInfo", cpuInfo)
    }

    public fun logAllUsage() {
        val cpuFreqs = getFreqForAllCores()
        val memoryStatus = getMemoryInfo(appContext)
        val batteryStatus = checkChargeBatteryStatus()
        val networkStatus = checkNetwork(appContext)

        val logMessage = """
            Timestamp: ${System.currentTimeMillis()}
            CPU: $cpuFreqs
            Memory: $memoryStatus
            Battery: $batteryStatus
            Network: $networkStatus
        """.trimIndent()

        Log.d("CpuUsageTracer", "Logged")
        WorkerProcessService.logToFile.logToFile("AllUsage", logMessage)
    }


}

