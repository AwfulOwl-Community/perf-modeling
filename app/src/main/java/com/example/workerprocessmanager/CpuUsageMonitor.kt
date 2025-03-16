package com.example.workerprocessmanager

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.Executors

class CpuUsageMonitor {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    // 保存上一次的CPU时间数据
    private var prevTotalTime: Long = 0
    private var prevIdleTime: Long = 0

    // 监听回调接口
    interface CpuUsageListener {
        fun onUsageUpdated(usagePercent: Float)
    }

    /**
     * 开始监控CPU使用率
     * @param intervalMs 更新间隔（毫秒）
     * @param listener 回调监听器
     */
    fun startMonitoring(intervalMs: Long, listener: CpuUsageListener) {
        val runnable = object : Runnable {
            override fun run() {
                executor.execute {
                    val usage = getCpuUsagePercentage()
                    handler.post { listener.onUsageUpdated(usage) }
                    handler.postDelayed(this, intervalMs)
                }
            }
        }

        handler.postDelayed(runnable, intervalMs)
    }

    /**
     * 停止监控CPU使用率
     */
    fun stopMonitoring() {
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 获取CPU使用率百分比
     * @return CPU使用率 (0-100)
     */
    private fun getCpuUsagePercentage(): Float {
        try {
            // 读取 /proc/stat 文件获取CPU信息
            val reader = BufferedReader(FileReader("/proc/stat"))
            val cpuLine = reader.readLine()
            reader.close()

            // 解析CPU信息行，格式为:
            // cpu  user nice system idle iowait irq softirq steal guest guest_nice
            val cpuInfo = cpuLine.split("\\s+".toRegex()).dropWhile { it.isEmpty() }

            // 检查获取的数据是否有效
            if (cpuInfo.size < 5 || cpuInfo[0] != "cpu") {
                return 0f
            }

            // 计算总时间和空闲时间
            var totalTime: Long = 0
            for (i in 1 until cpuInfo.size) {
                totalTime += cpuInfo[i].toLongOrNull() ?: 0
            }

            val idleTime = cpuInfo[4].toLongOrNull() ?: 0

            // 计算时间差
            val totalTimeDelta = totalTime - prevTotalTime
            val idleTimeDelta = idleTime - prevIdleTime

            // 更新上一次的数据
            prevTotalTime = totalTime
            prevIdleTime = idleTime

            // 避免除以零
            if (totalTimeDelta == 0L) {
                return 0f
            }

            // 计算CPU使用率
            return 100f * (1f - idleTimeDelta.toFloat() / totalTimeDelta.toFloat())
        } catch (e: IOException) {
            e.printStackTrace()
            return 0f
        }
    }
}
