package com.example.superdialer

import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.superdialer.ui.theme.SuperDialerTheme
import com.example.superdialer.ui.theme.DialerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permission launcher for CALL_PHONE
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // You can handle "permission denied" here if needed
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CALL_LOG),
            1
        )


        setContent {
            SuperDialerTheme {
                DialerScreen(
                    onCallClick = { number ->
                        // Ask for permission before making call
                        requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:$number")
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
