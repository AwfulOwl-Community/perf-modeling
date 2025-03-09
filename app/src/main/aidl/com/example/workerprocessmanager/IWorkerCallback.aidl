// IWorkerCallback.aidl
package com.example.workerprocessmanager;

interface IWorkerCallback {
    void onWorkerStatusChanged(boolean isRunning);
    void onWorkerError(String error);
}