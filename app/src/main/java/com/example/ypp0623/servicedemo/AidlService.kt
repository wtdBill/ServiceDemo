package com.example.ypp0623.servicedemo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException

class AidlService : Service() {


    private var mBinder: MyAIDLService.Stub = object : MyAIDLService.Stub() {
        @Throws(RemoteException::class)
        override fun plus(a: Int, b: Int): Int {
            return a + b
        }

        @Throws(RemoteException::class)
        override fun toUpperCase(str: String?): String? {
            return str?.toUpperCase()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

}
