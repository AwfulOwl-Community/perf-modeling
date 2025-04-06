package com.example.workerprocessmanager

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Executors

class CpuUsageMonitor {

    public fun getCpuUsage(): Double {
        try {
            // 执行top命令
            val process = Runtime.getRuntime().exec("top -n 1")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            // 解析输出
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    System.out.println(it)
                    WorkerProcessService.logToFile.logToFile("CpuUsageMonitor", it)
                    if (it.contains("CPU")) {
                        // 找到包含CPU信息的一行
                        val usageLine = it.trim()
                        val cpuInfo = usageLine.split(",").map { info -> info.trim() }
                        // 提取用户和系统CPU占用的百分比
                        var totalCpuUsage = 0.0
                        for (info in cpuInfo) {
                            if (info.contains("%")) {
                                totalCpuUsage += info.substringBefore("%").toDoubleOrNull() ?: 0.0
                            }
                        }
                        return totalCpuUsage
                    }
                }
            }
            reader.close()
            process.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0.0 // 默认返回0表示读取失败
    }


}
