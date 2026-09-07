package com.harshit.client

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class MainActivity : AppCompatActivity() {

    private lateinit var barcodeScanner: DecoratedBarcodeView
    private lateinit var tvStatus: TextView
    private val databaseRef = FirebaseDatabase.getInstance().getReference("Customers")
    private val CAMERA_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        barcodeScanner = findViewById(R.id.barcode_scanner)
        tvStatus = findViewById(R.id.tvStatus)

        // 1. कैमरा परमिशन चेक करो
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE), CAMERA_PERMISSION_CODE)
        } else {
            startScanner()
        }
    }

    private fun startScanner() {
        barcodeScanner.setStatusText("Point camera at Merchant QR")
        barcodeScanner.resume()

        barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    val scannedData = it.text // मर्चेंट के QR से मिला डेटा (जैसे Loan ID)
                    barcodeScanner.pause() // एक बार स्कैन होने के बाद स्कैनर रोक दो
                    
                    tvStatus.text = "Syncing with Merchant..."
                    syncWithFirebase(scannedData)
                }
            }
            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
        })
    }

    @SuppressLint("HardwareIds")
    private fun syncWithFirebase(scannedData: String) {
        // फोन का असली ID/IMEI निकालना
        var deviceIMEI = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        
        // (अगर Device Owner है, तो असली IMEI भी निकल सकता है, अभी सुरक्षित तरीके से Android ID ले रहे हैं)
        
        // फायरबेस में डेटा भेजना (मर्चेंट को सिग्नल देना कि डिवाइस जुड़ गया है)
        databaseRef.child(deviceIMEI).child("isDeviceLocked").setValue(false)
        databaseRef.child(deviceIMEI).child("status").setValue("Active / Synced")
        databaseRef.child(deviceIMEI).child("loanId").setValue(scannedData) // QR से मिली Loan ID

        Toast.makeText(this, "Device Synced Successfully!", Toast.LENGTH_LONG).show()

        // बैकग्राउंड सर्विस स्टार्ट करो
        val serviceIntent = Intent(this, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // अपना काम होने के बाद ऐप का आइकॉन फोन के मेन्यू से छुपा दो (Hide App)
        hideAppIcon()
    }

    private fun hideAppIcon() {
        val componentName = ComponentName(this, MainActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        finish() // ऐप स्क्रीन से बंद हो जाएगा लेकिन बैकग्राउंड में चलता रहेगा
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanner()
        } else {
            Toast.makeText(this, "Camera Permission is required to scan QR!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeScanner.isInitialized) barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeScanner.isInitialized) barcodeScanner.pause()
    }
}
