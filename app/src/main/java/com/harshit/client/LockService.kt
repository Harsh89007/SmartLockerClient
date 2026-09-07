package com.harshit.client

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
        
        // Android 8.0+ के लिए Notification Channel बनाना ज़रूरी है (वरना ऐप क्रैश होगा)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SmartClientChannel",
                "Device Security Service",
                NotificationManager.IMPORTANCE_LOW // Low रखने से यह बार-बार आवाज़ नहीं करेगा
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        // सर्विस को क्रैश होने से बचाने के लिए Notification दिखाना
        val notification: Notification = NotificationCompat.Builder(this, "SmartClientChannel")
            .setContentTitle("Security Active")
            .setContentText("This device is secured by Smart Locker.")
            .setSmallIcon(android.R.drawable.ic_secure) // डिफॉल्ट लॉक आइकॉन
            .setOngoing(true) // इसे कोई स्वाइप करके हटा नहीं पाएगा
            .build()
        
        startForeground(1, notification)

        // फोन का असली ID निकालना
        deviceIMEI = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // 24/7 फायरबेस को सुनते रहना
        databaseRef.child(deviceIMEI).child("isDeviceLocked").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isLocked = snapshot.getValue(Boolean::class.java) ?: false
                
                if (isLocked) {
                    // फायरबेस में Lock ON होते ही LockScreenActivity खोल दो
                    val lockIntent = Intent(this@LockService, LockScreenActivity::class.java)
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(lockIntent)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        return START_STICKY // सिस्टम इसे बंद करे तो खुद दोबारा चालू हो जाए
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // हमें बाइंडिंग नहीं चाहिए
    }
}
