// MainActivity.kt
package com.example.workerprocessmanager

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.workerprocessmanager.IWorkerProcess

class MainActivity : AppCompatActivity() {
    private var workerProcessService: IWorkerProcess? = null
    private var isServiceBound = false

    companion object {
        private const val TAG = "MainActivity"
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected: ${name?.className}")
            workerProcessService = IWorkerProcess.Stub.asInterface(service)
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected: ${name?.className}")
            workerProcessService = null
            isServiceBound = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.activity_main)

        val createDestroyButton = findViewById<Button>(R.id.btnCreateDestroy)
        val pauseResumeButton = findViewById<Button>(R.id.btnPauseResume)

        var isWorkerRunning = false
        var isWorkerPaused = false

        createDestroyButton.setOnClickListener {
            if (!isWorkerRunning) {
                Log.i(TAG, "Starting worker process")
                startWorkerProcess()
                isWorkerRunning = true
                createDestroyButton.text = getString(R.string.destroy_worker)
            } else {
                Log.i(TAG, "Stopping worker process")
                stopWorkerProcess()
                isWorkerRunning = false
                createDestroyButton.text = getString(R.string.create_worker)
            }
        }

        pauseResumeButton.setOnClickListener {
            if (isServiceBound) {
                if (!isWorkerPaused) {
                    Log.i(TAG, "Pausing worker")
                    workerProcessService?.pauseWorker()
                    isWorkerPaused = true
                    pauseResumeButton.text = getString(R.string.resume_worker)
                } else {
                    Log.i(TAG, "Resuming worker")
                    workerProcessService?.resumeWorker()
                    isWorkerPaused = false
                    pauseResumeButton.text = getString(R.string.pause_worker)
                }
            } else {
                Log.w(TAG, "Service not bound, cannot pause/resume worker")
                Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startWorkerProcess() {
        Log.d(TAG, "Binding and starting worker service")
        val intent = Intent(this, WorkerProcessService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "Starting worker service")
        startForegroundService(intent)
    }

    private fun stopWorkerProcess() {
        if (isServiceBound) {
            Log.d(TAG, "Unbinding worker service")
            unbindService(serviceConnection)
            isServiceBound = false
        }
        Log.d(TAG, "Stopping worker service")
        stopService(Intent(this, WorkerProcessService::class.java))
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        if (isServiceBound) {
            Log.d(TAG, "Unbinding service in onDestroy")
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }
}