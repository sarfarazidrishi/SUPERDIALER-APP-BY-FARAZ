package com.example.superdialer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.example.superdialer.ui.theme.SuperDialerTheme
import com.example.superdialer.ui.theme.DialerScreen
import com.example.superdialer.ui.theme.ProfileScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Permissions to request
        val permissionsToRequest = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )

        // ✅ Launcher for requesting multiple permissions
        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                val denied = results.filterValues { !it }.keys
                if (denied.isNotEmpty()) {
                    // Optional: notify the user some permissions were denied
                    // e.g., Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
                }
            }

        // ✅ Request all at once
        permissionLauncher.launch(permissionsToRequest)

        setContent {
            var selectedNumber by remember { mutableStateOf<String?>(null) }

            SuperDialerTheme {
                if (selectedNumber == null) {
                    DialerScreen(
                        onProfileClick = { number ->
                            selectedNumber = number
                        }
                    )
                } else {
                    ProfileScreen(
                        number = selectedNumber!!,
                        onBack = { selectedNumber = null }
                    )
                }
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
