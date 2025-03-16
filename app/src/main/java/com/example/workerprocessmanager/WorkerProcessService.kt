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
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import android.os.Process


class WorkerProcessService : Service() {
    private var isRunning = false
    private var isPaused = false
    private lateinit var logToFile: LogToFile
    /*
    private var periodicCheckUtilizationHandler: Handler = Handler(Looper.getMainLooper())
    private val periodicCheckUtilization: Runnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Dummy Check")
            periodicCheckUtilizationHandler.postDelayed(this, 10000) // schedule itself
        }
    }*/

    private lateinit var worker: WorkerThread_

    companion object {
        private const val TAG = "WorkerProcessService"
        private const val CHANNEL_ID = "WorkerProcessChannel"
        private const val NOTIFICATION_ID = 1
        private const val AUTO_RESPAWN_INTERVAL = 60000L // 1 minute
        public const val WORKER_INFORM_PID = 2
        public val mainHandler: Handler = Handler(Looper.getMainLooper())
    }

    private val binder = object : IWorkerProcess.Stub() {
        override fun pauseWorker() {
            Log.i(TAG, "Pausing worker thread")
            isPaused = true
        }

        override fun resumeWorker() {
            Log.i(TAG, "Resuming worker thread")
            isPaused = false
            /*
            synchronized(workerThread) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (workerThread as java.lang.Object).notify()
            }
            */
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
        worker = WorkerThread_()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with startId: $startId")
        logToFile.logToFile(TAG, "onStartCommand called with startId: $startId")
        startForeground(NOTIFICATION_ID, createNotification())
        if(!worker.isAlive){
            worker.start()
        }
        else{
            Log.d(TAG, "Worker thread is already running")
            logToFile.logToFile(TAG, "Worker thread is already running")
        }
        //periodicCheckUtilizationHandler.postDelayed(periodicCheckUtilization, 10000)
        return START_STICKY
    }




    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called")
        return binder
    }

    /*
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
    }*/

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


private class WorkerThread_ : HandlerThread("WorkerThread") {

    companion object{
        private val TAG = "WorkerThread"
    }
    private lateinit var handler: Handler

    private val dummyOperands = DummyOperands()
    private lateinit var workRunnable: Runnable

    // init
    init {
        workRunnable = Runnable {
            work()
            Log.d(TAG, "post new")
            handler.post(workRunnable)
        }
    }

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        handler = WorkerHandler(looper)
        Log.d(TAG, "Worker thread looper prepared")
        //sendTidToMainThread()
        handler.post(workRunnable)
    }


    fun work() {
        Log.d(TAG, "Worker thread started")
        dummyWorkload()
        Log.d(TAG, "Worker thread finished")
    }

    fun dummyWorkload() {
        Log.d(TAG, "Dummy started")
        val matrixSize = 1000
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                for (k in 0 until matrixSize) {
                    // Multiply matrices A and B and store the result in matrix C
                    dummyOperands.matrixD[i][j] = dummyOperands.matrixC[i][j] + dummyOperands.matrixA[i][k] * dummyOperands.matrixB[k][j]
                }
            }
        }
        Log.d(TAG, "Dummy finished")
    }

    fun sendTidToMainThread() {
        val tid: Int = Process.myTid()
        Log.d(TAG, "Worker thread sending its TID: $tid")
        val tidMsg: Message = Message.obtain()
        tidMsg.what = WorkerProcessService.WORKER_INFORM_PID
        tidMsg.arg1 = tid
        WorkerProcessService.mainHandler.sendMessage(tidMsg)
    }


}


// A thread extends normal thread equivalent to WorkerThread_

private class WorkerThread__ : Thread() {

    companion object{
        private val TAG = "WorkerThread"
    }

    private val dummyOperands = DummyOperands()

    override fun run() {
        Log.d(TAG, "Worker thread started")
        dummyWorkload()
        Log.d(TAG, "Worker thread finished")
    }

    fun dummyWorkload() {
        Log.d(TAG, "Dummy started")
        val matrixSize = 100
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                for (k in 0 until matrixSize) {
                    // Multiply matrices A and B and store the result in matrix C
                    dummyOperands.matrixD[i][j] = dummyOperands.matrixC[i][j] + dummyOperands.matrixA[i][k] * dummyOperands.matrixB[k][j]
                }
            }
        }
        Log.d(TAG, "Dummy finished")
    }

}


private class WorkerHandler internal constructor(
    looper: Looper?
) :
    Handler(looper!!) {

    private final val MSG_LOWER_PRIORITY = 0
    private final val MSG_RAISE_PRIORITY = 1
    private final val TAG = "A4A_Worker"

    private val dummyOperands = DummyOperands()

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_LOWER_PRIORITY -> {
                Log.d(TAG, "Lowering worker thread priority")
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            }
            MSG_RAISE_PRIORITY -> {
                Log.d(TAG, "Raising worker thread priority")
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            }
            else -> super.handleMessage(msg)
        }
    }

}

