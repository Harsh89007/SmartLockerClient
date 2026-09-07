package com.harshit.client

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
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
                    val scannedData = it.text
                    barcodeScanner.pause()
                    tvStatus.text = "Syncing with Merchant..."
                    syncWithFirebase(scannedData)
                }
            }
            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
        })
    }

    @SuppressLint("HardwareIds")
    private fun syncWithFirebase(scannedData: String) {
        val deviceIMEI = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        
        databaseRef.child(deviceIMEI).child("isDeviceLocked").setValue(false)
        databaseRef.child(deviceIMEI).child("status").setValue("Active / Synced")
        databaseRef.child(deviceIMEI).child("loanId").setValue(scannedData)
        databaseRef.child(deviceIMEI).child("imei").setValue(deviceIMEI)

        Toast.makeText(this, "Device Synced Successfully!", Toast.LENGTH_LONG).show()

        val serviceIntent = Intent(this, LockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        hideAppIcon()
    }

    private fun hideAppIcon() {
        val componentName = ComponentName(this, MainActivity::class.java)
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanner()
        } else {
            Toast.makeText(this, "Camera Permission is required!", Toast.LENGTH_SHORT).show()
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
