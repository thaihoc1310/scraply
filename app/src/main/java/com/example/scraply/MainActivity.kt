package com.example.scraply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.scraply.ui.navigation.ScraplyNavHost
import com.example.scraply.ui.theme.ScraplyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScraplyTheme {
                ScraplyNavHost()
            }
        }
    }
}
