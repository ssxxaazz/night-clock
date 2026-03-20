package com.ssxxaazz.nightclock

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ssxxaazz.nightclock.ui.screen.FullscreenClock
import com.ssxxaazz.nightclock.ui.theme.NightClockTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FullscreenActivity : ComponentActivity() {

    var lowBrightness = false
        private set
    private var dndWasEnabledBeforeNightMode = false
    private var wasInNightMode = false

    private val systemUiVisibleForClock = mutableStateOf(false)

    fun toggleNightMode() {
        lowBrightness = !lowBrightness
        applyCustomBrightness()
    }

    override fun onResume() {
        super.onResume()
        setupWindowFlags()
        applyCustomBrightness()
    }

    override fun onPause() {
        super.onPause()
        if (lowBrightness) {
            lowBrightness = false
            val layout = window.attributes
            layout.screenBrightness = -1f
            window.attributes = layout

            if (wasInNightMode && !dndWasEnabledBeforeNightMode) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
            wasInNightMode = false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        setupWindowFlags()
    }

    private fun applyCustomBrightness() {
        val layout = window.attributes
        if (lowBrightness) {
            val sharedPreferences = getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
            layout.screenBrightness = sharedPreferences.getInt("night_mode_brightness", 0) / 500f

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                android.widget.Toast.makeText(this, "Enable Do Not Disturb permission in Settings", android.widget.Toast.LENGTH_LONG).show()
                startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            } else {
                dndWasEnabledBeforeNightMode = notificationManager.currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                if (!dndWasEnabledBeforeNightMode) {
                    notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS)
                }
            }

            wasInNightMode = true
            android.widget.Toast.makeText(applicationContext, "Night Mode", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            layout.screenBrightness = -1f

            if (wasInNightMode) {
                if (!dndWasEnabledBeforeNightMode) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                }
                android.widget.Toast.makeText(applicationContext, "Day Mode", android.widget.Toast.LENGTH_SHORT).show()
                wasInNightMode = false
            }
        }
        window.attributes = layout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindowFlags()

        setContent {
            NightClockTheme {
                FullscreenClock(
                    systemUiVisible = systemUiVisibleForClock,
                    onSettingsClick = {
                        val myIntent = Intent(this, SettingsActivity::class.java)
                        startActivity(myIntent)
                    },
                    onSystemUiShow = {
                        showSystemUI()
                    },
                    onSystemUiAutoHide = {
                        systemUiVisibleForClock.value = false
                        hideSystemUI()
                    },
                    onLongPress = {
                        toggleNightMode()
                    }
                )
            }
        }

        setupSystemUiVisibilityListener {
            systemUiVisibleForClock.value = false
            hideSystemUI()
        }
    }

    private fun showSystemUI() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.setAppearanceLightNavigationBars(false)
    }

    private fun hideSystemUI() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
    }

    private fun setupSystemUiVisibilityListener(onAutoHide: () -> Unit) {
        val decorView = window.decorView

        decorView.setOnApplyWindowInsetsListener { v, insets ->
            val isVisible = insets.isVisible(android.view.WindowInsets.Type.statusBars()) ||
                    insets.isVisible(android.view.WindowInsets.Type.navigationBars())

            if (isVisible) {
                lifecycleScope.launch {
                    delay(2000)
                    onAutoHide()
                }
            }

            v.onApplyWindowInsets(insets)
        }
    }

    private fun setupWindowFlags() {
        val window = window
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupWindowFlags()
        hideSystemUI()
    }
}
