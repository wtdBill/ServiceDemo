package com.example.ypp0623.servicedemo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.app.ActivityCompat
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val strings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else {
        TODO("VERSION.SDK_INT < JELLY_BEAN")
    }
    private lateinit var myBinder: DownloadService.MyBinder
    private lateinit var myAidlService:MyAIDLService
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            myBinder = service as DownloadService.MyBinder
            myBinder.startDownload()
        }

    }

    private val aidlConnection = object :ServiceConnection{
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            myAidlService = MyAIDLService.Stub.asInterface(service)
            try {
                val result = myAidlService.plus(3,5)
                val uppStr = myAidlService.toUpperCase("hello world")
                Log.d("TAG", "result is " + result)
                Log.d("TAG", "upperStr is " + uppStr)
            }catch (e:RemoteException){
                e.printStackTrace()
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        //如果没有权限,申请权限
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, strings, 11)
        } else {
            setClick()
        }

    }

    private fun setClick() {
        hello.setOnClickListener({
            val intent = Intent(this, DownloadService::class.java)
            startService(intent)
        })
        bind.setOnClickListener {
            val bindIntent = Intent(this, DownloadService::class.java)
            bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)
        }
        unbind.setOnClickListener {
            try {
                myBinder.cancelDownload()
                unbindService(connection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        aidl.setOnClickListener {
            val aidlIntent = Intent(this,AidlService::class.java)
            bindService(aidlIntent,aidlConnection,Context.BIND_AUTO_CREATE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 11 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setClick()
        }
    }
}
