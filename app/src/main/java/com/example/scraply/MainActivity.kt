package com.example.scraply

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.scraply.data.preferences.AppLanguage
import com.example.scraply.data.preferences.AppThemeMode
import com.example.scraply.notifications.ScraplyNotifications
import com.example.scraply.ui.navigation.ScraplyNavHost
import com.example.scraply.ui.theme.ScraplyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ScraplyNotifications.ensureChannels(applicationContext)
        val appPrefRepository = ScraplyApp.instance.container.appPreferencesRepository

        // Synchronously read the preferred language on launch to set it in the configuration immediately
        val initialLanguage = runBlocking { appPrefRepository.appLanguage.first() }
        applyLocale(if (initialLanguage == AppLanguage.VIETNAMESE) "vi" else "en")

        setContent {
            val themeMode by appPrefRepository.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val appLanguage by appPrefRepository.appLanguage.collectAsState(initial = initialLanguage)
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            // Apply locale changes in-place without recreate() to preserve navigation stack
            val langCode = if (appLanguage == AppLanguage.VIETNAMESE) "vi" else "en"
            LaunchedEffect(langCode) {
                applyLocale(langCode)
            }

            // Wrap content with a locale-aware context so all stringResource() calls resolve correctly
            val localizedContext = createLocalizedContext(langCode)
            val localizedConfig = localizedContext.resources.configuration

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig,
                LocalActivityResultRegistryOwner provides this@MainActivity,
            ) {
                ScraplyTheme(darkTheme = darkTheme) {
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

    private fun applyLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun createLocalizedContext(langCode: String): Context {
        val locale = Locale(langCode)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        val localizedContext = createConfigurationContext(config)
        return LocalizedContextWrapper(this, localizedContext)
    }
}

class LocalizedContextWrapper(base: Context, private val localizedContext: Context) : ContextWrapper(base) {
    override fun getResources(): Resources {
        return localizedContext.resources
    }
    override fun getAssets(): AssetManager {
        return localizedContext.assets
    }
    override fun getTheme(): Resources.Theme {
        return localizedContext.theme
    }
}
