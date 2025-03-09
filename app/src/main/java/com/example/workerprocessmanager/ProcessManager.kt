// ProcessManager.kt
package com.example.workerprocessmanager

import android.content.Context
import android.app.ActivityManager

class ProcessManager(private val context: Context) {
    fun isProcessRunning(serviceName: String): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceName }
    }

    fun getProcessId(serviceName: String): Int {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .find { it.service.className == serviceName }
            ?.pid ?: -1
    }
}