package com.ssxxaazz.nightclock

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
    @Suppress("VisibilityProperty")
    internal var isCoveredByOtherActivity = false

    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var lightSensorListener: SensorEventListener? = null

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
        stopLightSensor()
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

    override fun onStop() {
        super.onStop()
        if (!isCoveredByOtherActivity) {
            finish()
        }
        isCoveredByOtherActivity = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        setupWindowFlags()
    }

    private fun applyCustomBrightness() {
        val sharedPreferences = getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        val layout = window.attributes
        if (lowBrightness) {
            val autoBrightnessEnabled = sharedPreferences.getBoolean("auto_brightness", false)

            if (autoBrightnessEnabled && hasLightSensor()) {
                layout.screenBrightness = DEFAULT_AUTO_FACTOR * MAX_AUTO_BRIGHTNESS
                startLightSensor()
            } else {
                stopLightSensor()
                layout.screenBrightness = computeBaseBrightness(sharedPreferences)
            }

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
            stopLightSensor()
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

    private fun computeBaseBrightness(prefs: android.content.SharedPreferences): Float {
        return prefs.getInt("night_mode_brightness", 0) / 500f
    }

    private fun hasLightSensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
    }

    private fun startLightSensor() {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) ?: return
        if (lightSensorListener != null) return

        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                val lux = event.values[0]
                val autoFactor = computeAutoFactor(lux)
                runOnUiThread {
                    val layout = window.attributes
                    layout.screenBrightness = autoFactor * MAX_AUTO_BRIGHTNESS
                    window.attributes = layout
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            lightSensorListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun stopLightSensor() {
        lightSensorListener?.let { listener ->
            sensorManager.unregisterListener(listener)
        }
        lightSensorListener = null
    }

    companion object {
        /** Initial auto factor applied before the first sensor reading arrives (mid-range). */
        private const val DEFAULT_AUTO_FACTOR = 0.5f

        /** Maximum screen brightness when auto-brightness is at 100% (100/500 = 0.2). */
        private const val MAX_AUTO_BRIGHTNESS = 0.2f

        /** Map ambient lux to a brightness scale factor in [0.0, 1.0].
         *  0 lux → 0% equivalent, 200 lux → 100% equivalent.
         *  Uses a natural-log mapping clamped at 200 lux.
         *  Negative lux values are treated as 0 to prevent NaN. */
        fun computeAutoFactor(lux: Float): Float {
            val safeLux = lux.coerceAtLeast(0f)
            return (kotlin.math.ln((safeLux + 1f).toDouble()) / kotlin.math.ln(201.0))
                .toFloat()
                .coerceIn(0f, 1f)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindowFlags()

        setContent {
            NightClockTheme {
                FullscreenClock(
                    systemUiVisible = systemUiVisibleForClock,
                    onSettingsClick = {
                        isCoveredByOtherActivity = true
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
