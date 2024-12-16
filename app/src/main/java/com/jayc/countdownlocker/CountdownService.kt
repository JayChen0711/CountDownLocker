package com.jayc.countdownlocker

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build.VERSION_CODES
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CountdownService : Service() {

    override fun onBind(intent: Intent): IBinder {
        Log.d("Jay", "onBind: ")
        throw UnsupportedOperationException("Not yet implemented")
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val minutes = intent?.getIntExtra(EXTRA_MINUTES, 1) ?: 1
        startCountDownNotification(minutes)

        return super.onStartCommand(intent, flags, startId)
    }


    @RequiresApi(VERSION_CODES.Q)
    private fun startCountDownNotification(minutes: Int) {

        startForeground(COUNTDOWN_NOTIFICATION_ID, getNotification(minutes * 60000L))


        val deviceManager = DeviceManager(this)
        object : CountDownTimer(minutes * 60000L, 1000L) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            override fun onTick(millisUntilFinished: Long) {
                manager.notify(COUNTDOWN_NOTIFICATION_ID, getNotification(millisUntilFinished))
            }

            override fun onFinish() {
                manager.cancel(COUNTDOWN_NOTIFICATION_ID)
                deviceManager.lockNow()
                stopSelf()
            }

        }.start()
    }

    private fun getNotification(millisUntilFinished: Long): Notification {
        val expireTimeInMillisecond = Date().time + millisUntilFinished
        val expired =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(expireTimeInMillisecond))
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_notification)
            .setOnlyAlertOnce(true)
            .setContentTitle(
                "Lock after ${
                    String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        millisUntilFinished / 60000,
                        millisUntilFinished / 1000 % 60
                    )
                }"
            )
            .setContentText("Screen will be locked at $expired")
            .build()
    }

    companion object {
        const val EXTRA_MINUTES = "MIN"
        const val CHANNEL_ID = "CountdownChannel"
        const val COUNTDOWN_NOTIFICATION_ID = 78249
    }
}