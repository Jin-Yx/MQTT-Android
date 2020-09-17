package com.jinyx.mqtt

import android.Manifest.permission
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.RequiresPermission
import java.lang.ref.WeakReference

class MqttReconnectHandler(imqtt: IMqtt, private val context: Context, private val interval: Long) : Handler(Looper.getMainLooper()) {

    companion object {
        const val RECONNECT = 0x01
    }

    private val mWeakReference = WeakReference(imqtt)

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        if (msg.what == RECONNECT) {
            if (isNetworkAvailable(context)) {
                mWeakReference.get()?.connect()
            }
            sendEmptyMessageDelayed(RECONNECT, interval)
        }
    }

    @RequiresPermission(permission.ACCESS_NETWORK_STATE)
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val info = cm?.activeNetworkInfo
        return info != null && info.isConnected && info.state == NetworkInfo.State.CONNECTED
    }

}