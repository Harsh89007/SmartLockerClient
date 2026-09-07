package com.harshit.client

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MyAdminReceiver : DeviceAdminReceiver() {

    // जब ऐप को एडमिन परमिशन मिल जाएगी
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Smart Client App Activated!", Toast.LENGTH_SHORT).show()
    }

    // जब कोई चालाक कस्टमर परमिशन हटाने की कोशिश करेगा (Uninstall करने के लिए)
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "चेतावनी: अगर आप इसे बंद करेंगे तो आपका फोन तुरंत लॉक हो जाएगा और आपको पेनाल्टी देनी पड़ेगी!"
    }

    // अगर कैसे भी करके परमिशन हट गई
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Admin Disabled", Toast.LENGTH_SHORT).show()
    }
}
