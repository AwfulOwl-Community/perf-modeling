package com.example.workerprocessmanager
/*
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LogToFile(private val context: Context) {

    private val logFileName = "app_log.txt"

    fun logToFile(tag: String, message: String) {
        val logMessage = "$tag: $message\n"
        try {
            val file = File(context.filesDir, logFileName)
            FileOutputStream(file, true).use { outputStream ->
                outputStream.write(logMessage.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}*/


import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogToFile(private val context: Context) {

    private val logFileName = "app_log.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun logToFile(tag: String, message: String) {
        val timestamp = System.currentTimeMillis()
        val logMessage = "$timestamp $tag: $message\n"
        try {
            val externalStorageDir = context.getExternalFilesDir(null)
            val file = File(externalStorageDir, logFileName)
            FileOutputStream(file, true).use { outputStream ->
                outputStream.write(logMessage.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
// When foreground die, not continue.