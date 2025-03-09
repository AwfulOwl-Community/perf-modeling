// WorkerProcessService.kt
package com.example.workerprocessmanager

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.workerprocessmanager.IWorkerProcess

class WorkerProcessService : Service() {
    private var isRunning = false
    private var isPaused = false
    private lateinit var workerThread: Thread
    private lateinit var logToFile: LogToFile

    companion object {
        private const val TAG = "WorkerProcessService"
        private const val CHANNEL_ID = "WorkerProcessChannel"
        private const val NOTIFICATION_ID = 1
        private const val AUTO_RESPAWN_INTERVAL = 60000L // 1 minute
    }

    private val binder = object : IWorkerProcess.Stub() {
        override fun pauseWorker() {
            Log.i(TAG, "Pausing worker thread")
            isPaused = true
        }

        override fun resumeWorker() {
            Log.i(TAG, "Resuming worker thread")
            isPaused = false
            synchronized(workerThread) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (workerThread as java.lang.Object).notify()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
        logToFile = LogToFile(this)
        logToFile.logToFile(TAG, "onCreate called")
        createNotificationChannel()
        setupAutoRespawn()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with startId: $startId")
        logToFile.logToFile(TAG, "onStartCommand called with startId: $startId")
        startForeground(NOTIFICATION_ID, createNotification())
        
        if (!isRunning) {
            Log.i(TAG, "Starting worker thread")
            logToFile.logToFile(TAG, "Starting worker thread")
            isRunning = true
            startWorkerProcess()
        } else {
            Log.d(TAG, "Worker thread already running")
            logToFile.logToFile(TAG, "Worker thread already running")
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called")
        return binder
    }

    private fun startWorkerProcess() {
        workerThread = Thread {
            Log.d(TAG, "Worker thread started")
            var shouldContinue = true
            while (isRunning && shouldContinue) {
                if (!isPaused) {
                    try {
                        Log.v(TAG, "Worker performing task")
                        logToFile.logToFile(TAG, "Worker performing task")
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        Log.w(TAG, "Worker thread interrupted", e)
                        logToFile.logToFile(TAG, "Worker thread interrupted: ${e.message}")
                        Thread.currentThread().interrupt()
                        shouldContinue = false
                    }
                } else {
                    synchronized(workerThread) {
                        try {
                            Log.d(TAG, "Worker thread entering wait state")
                            logToFile.logToFile(TAG, "Worker thread entering wait state")
                            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                            (workerThread as java.lang.Object).wait()
                        } catch (e: InterruptedException) {
                            Log.w(TAG, "Worker wait state interrupted", e)
                            logToFile.logToFile(TAG, "Worker wait state interrupted: ${e.message}")
                            Thread.currentThread().interrupt()
                            shouldContinue = false
                        }
                    }
                }
            }
            Log.d(TAG, "Worker thread finished")
            logToFile.logToFile(TAG, "Worker thread finished")
        }
        workerThread.start()
    }

    private fun setupAutoRespawn() {
        Log.d(TAG, "Setting up auto-respawn mechanism")
        logToFile.logToFile(TAG, "Setting up auto-respawn mechanism")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WorkerProcessService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + AUTO_RESPAWN_INTERVAL,
            AUTO_RESPAWN_INTERVAL,
            pendingIntent
        )
        Log.i(TAG, "Auto-respawn scheduled with interval: $AUTO_RESPAWN_INTERVAL ms")
        logToFile.logToFile(TAG, "Auto-respawn scheduled with interval: $AUTO_RESPAWN_INTERVAL ms")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        Log.d(TAG, "Creating notification channel")
        logToFile.logToFile(TAG, "Creating notification channel")
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Worker Process Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Worker Process Manager")
        .setContentText("Worker process is running")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        logToFile.logToFile(TAG, "onDestroy called")
        super.onDestroy()
    }
}