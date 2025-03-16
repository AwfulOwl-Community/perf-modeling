package com.example.workerprocessmanager

class DummyOperands {
    // matrix 100x100 A,B,C Dst:D
    val matrixSize = 100
    public var matrixA = Array(matrixSize) { DoubleArray(matrixSize) }
    public var matrixB = Array(matrixSize) { DoubleArray(matrixSize) }
    public var matrixC = Array(matrixSize) { DoubleArray(matrixSize) }
    public var matrixD = Array(matrixSize) { DoubleArray(matrixSize) }

    init {
        for (i in 0 until matrixSize) {
            for (j in 0 until matrixSize) {
                matrixA[i][j] = i.toDouble()
                matrixB[i][j] = j.toDouble()
                matrixC[i][j] = 0.0
                matrixD[i][j] = 0.0
            }
        }
    }
}