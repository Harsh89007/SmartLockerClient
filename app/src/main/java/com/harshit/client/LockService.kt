package com.harshit.client

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LockService : Service() {

    private val databaseRef = FirebaseDatabase.getInstance().getReference("Customers")
    private var deviceIMEI = ""

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SmartClientChannel",
                "Security Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("HardwareIds")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, "SmartClientChannel")
            .setContentTitle("Security Active")
            .setContentText("Protected by Smart Client Locker")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .build()
        
        startForeground(1, notification)

        deviceIMEI = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        databaseRef.child(deviceIMEI).child("isDeviceLocked").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isLocked = snapshot.getValue(Boolean::class.java) ?: false
                if (isLocked) {
                    val lockIntent = Intent(this@LockService, LockScreenActivity::class.java)
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(lockIntent)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
