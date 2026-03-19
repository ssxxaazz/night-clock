package com.ssxxaazz.nightclock

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ssxxaazz.nightclock.ui.screen.SettingsScreen
import com.ssxxaazz.nightclock.ui.theme.NightClockTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow rotation for settings
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Show system UI in settings
        showSystemUI()

        setContent {
            NightClockTheme {
                SettingsScreen(
                    onBackClick = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showSystemUI()
    }

    private fun showSystemUI() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.setAppearanceLightNavigationBars(false)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
