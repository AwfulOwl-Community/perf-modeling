package com.example.workerprocessmanager

import android.util.Log

class WorkloadFactory {
    private val TAG = "WorkerThread"
    private val dummyOperands = DummyOperands()
    public fun makeDummyThread(): Thread {
        var dummyThread = Thread {
            Log.d(TAG, "Dummy started")
            // Matrix multiplication for 100x100 matrices
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
        return dummyThread
    }
}