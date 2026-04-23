package com.example.scraply

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.example.scraply.notifications.ScraplyNotifications
import com.example.scraply.ui.navigation.ScraplyNavHost
import com.example.scraply.ui.theme.ScraplyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ScraplyNotifications.ensureChannels(applicationContext)
        setContent {
            ScraplyTheme {
                // Android 13+ requires the POST_NOTIFICATIONS runtime permission before the
                // system will actually display pushes. Asking on first composition keeps the
                // prompt off the splash/camera flows and lets the user decline without
                // crashing the app – notifications simply silently stop being shown.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { /* result logged by the system; no-op here */ }
                    LaunchedEffect(Unit) {
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                ScraplyNavHost()
            }
        }
    }
}
