package com.harshit.client

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LockScreenActivity : AppCompatActivity() {

    // यहाँ हम कस्टमर का IMEI स्टोर करेंगे
    private var imei = "8645XXXXXXXX321" // (अभी डमी है, बाद में मेन फाइल से डायनामिक आएगा)
    private val databaseRef = FirebaseDatabase.getInstance().getReference("Customers")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // यह कोड स्क्रीन को फुल-स्क्रीन (No Status Bar) करेगा
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // यह फोन को सोने नहीं देगा (Screen Always ON during lock)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_lock_screen)

        // फायरबेस को लगातार सुनते रहना कि मर्चेंट ने अनलॉक किया या नहीं
        databaseRef.child(imei).child("isDeviceLocked").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isLocked = snapshot.getValue(Boolean::class.java) ?: true
                
                // अगर मर्चेंट ने लॉक हटा दिया (false कर दिया), तो यह लॉक स्क्रीन खुद बंद हो जाएगी
                if (!isLocked) {
                    finish() // लॉक स्क्रीन गायब!
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 1. बैक बटन (Back Button) डिसेबल करने का कोड
    override fun onBackPressed() {
        // यहाँ कुछ नहीं लिखा है, इसका मतलब बैक बटन दबाने पर कुछ नहीं होगा!
        // (Do nothing)
    }

    // 2. रीसेंट ऐप्स (Recent Apps) और दूसरे बटन को ब्लॉक करने की कोशिश
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH || keyCode == KeyEvent.KEYCODE_HOME) {
            return true // सिस्टम को बोलेगा कि मैंने बटन हैंडल कर लिया है, तुम कुछ मत करो
        }
        return super.onKeyDown(keyCode, event)
    }

    // 3. अगर कोई चालाकी से स्क्रीन से बाहर निकलने की कोशिश करे, तो ऐप फिर से सामने आ जाएगा
    override fun onPause() {
        super.onPause()
        
        // एक सेकंड के अंदर दोबारा लॉक स्क्रीन सामने लाओ!
        /* (यह लॉजिक बैकग्राउंड सर्विस के साथ 100% काम करेगा, जो हम अगली फाइल में लिखेंगे) */
    }
}
