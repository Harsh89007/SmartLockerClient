package com.harshit.client

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LockService : Service() {

    // (डमी IMEI - बाद में MainActivity से असली IMEI सेट करेंगे)
    private val imei = "8645XXXXXXXX321" 
    private val databaseRef = FirebaseDatabase.getInstance().getReference("Customers")

    override fun onBind(intent: Intent?): IBinder? {
        return null // हमें बाइंडिंग की ज़रूरत नहीं है, यह बैकग्राउंड में आज़ाद चलेगा
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LockService", "Service Started! Listening to Firebase...")

        // 24 घंटे फायरबेस को सुनते रहना
        databaseRef.child(imei).child("isDeviceLocked").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isLocked = snapshot.getValue(Boolean::class.java) ?: false
                
                if (isLocked) {
                    // अगर फायरबेस में Lock ON हुआ, तो तुरंत LockScreen खोल दो!
                    val lockIntent = Intent(this@LockService, LockScreenActivity::class.java)
                    // सर्विस से कोई भी स्क्रीन खोलने के लिए NEW_TASK का झंडा लगाना ज़रूरी है
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(lockIntent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LockService", "Firebase Error: ${error.message}")
            }
        })

        // START_STICKY का मतलब है कि अगर सिस्टम इसे बंद भी कर दे, तो यह दोबारा खुद चालू हो जाएगी!
        return START_STICKY 
    }
}

