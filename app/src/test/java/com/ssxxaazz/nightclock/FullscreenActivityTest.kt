package com.ssxxaazz.nightclock

import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.widget.Toast
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import org.robolectric.shadows.ShadowToast
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [TestShadowNotificationManager::class], sdk = [35])
class FullscreenActivityTest {

    private lateinit var activity: FullscreenActivity

    @Before
    fun setUp() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        // Grant notification policy permission using shadow
        val notificationManager = RuntimeEnvironment.getApplication()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadow = Shadows.shadowOf(notificationManager)
        shadow.setNotificationPolicyAccessGranted(true)
        
        activity = Robolectric.buildActivity(FullscreenActivity::class.java)
            .create()
            .resume()
            .get()
    }

    @Test
    fun activity_launches_successfully() {
        assert(!activity.isFinishing)
    }

    @Test
    fun longPress_togglesLowBrightness() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness", 100).apply()

        assert(!activity.lowBrightness)

        activity.toggleSleepMode()

        val attrs = activity.window.attributes
        assert(attrs.screenBrightness < 1.0f)
        assert(activity.lowBrightness)
    }

    @Test
    fun longPress_showsSleepModeToast_whenEnteringSleepMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness", 100).apply()

        ShadowToast.reset()
        activity.toggleSleepMode()

        val latestToast = ShadowToast.getLatestToast()
        assert(latestToast != null) { "Expected a toast to be shown" }
        assert(ShadowToast.getTextOfLatestToast() == "Sleep Mode") {
            "Expected toast message 'Sleep Mode' but got '${ShadowToast.getTextOfLatestToast()}'"
        }
    }

    @Test
    fun longPress_showsAwakeModeToast_whenExitingSleepMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness", 100).apply()

        // Enter sleep mode first
        activity.toggleSleepMode()
        ShadowToast.reset()

        // Exit sleep mode (back to awake mode)
        activity.toggleSleepMode()

        val latestToast = ShadowToast.getLatestToast()
        assert(latestToast != null) { "Expected a toast to be shown" }
        assert(ShadowToast.getTextOfLatestToast() == "Awake Mode") {
            "Expected toast message 'Awake Mode' but got '${ShadowToast.getTextOfLatestToast()}'"
        }
    }

    @Test
    fun longPress_enablesDoNotDisturb_whenEnteringSleepMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness", 100).apply()

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure DND is off initially
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

        activity.toggleSleepMode()

        // Verify DND is enabled (alarms only - not all interruptions)
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
            "Expected DND to be enabled when entering sleep mode"
        }
    }

    @Test
    fun longPress_disablesDoNotDisturb_whenExitingSleepMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness", 100).apply()

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure DND is off initially (not enabled by app before entering sleep mode)
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

        // Enter sleep mode (enables DND)
        activity.toggleSleepMode()

        // Exit sleep mode (should disable DND since app enabled it)
        activity.toggleSleepMode()

        // Verify DND is disabled (all - normal)
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            "Expected DND to be disabled when exiting sleep mode"
        }
    }

    @Test
    fun longPress_doesNotDisableDoNotDisturb_ifItWasAlreadyEnabled_beforeSleepMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness", 100).apply()

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Simulate user had DND already enabled (alarms only) before app enters sleep mode
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)

        // Enter sleep mode (app will see DND is already not all)
        activity.toggleSleepMode()

        // Exit sleep mode (should NOT disable DND since it was already enabled by user)
        activity.toggleSleepMode()

        // Verify DND is still enabled (alarms only - user had it on, app should not turn it off)
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
            "Expected DND to remain enabled when exiting sleep mode (it was already enabled before)"
        }
    }

    @Test
    fun longPress_doesNotDisableDoNotDisturb_ifItWasTotalDnd_beforeSleepMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness", 100).apply()

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Simulate user had total DND (none) before app enters sleep mode
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)

        // Enter sleep mode (app will see DND is already none)
        activity.toggleSleepMode()

        // Exit sleep mode (should NOT disable DND since it was already enabled by user)
        activity.toggleSleepMode()

        // Verify DND is still enabled (none - user had total DND, app should not turn it off)
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
            "Expected DND to remain enabled when exiting sleep mode (it was total DND before)"
        }
    }

    @Test
    fun onStop_finishesActivity_whenGoingToBackground() {
        val activityController = Robolectric.buildActivity(FullscreenActivity::class.java)
            .create()
            .resume()
        val runningActivity = activityController.get()

        assert(!runningActivity.isFinishing) { "Activity should be running" }

        activityController.stop()

        assert(runningActivity.isFinishing) { "Activity should finish when going to background" }
    }

    @Test
    fun onStop_doesNotFinishActivity_whenCoveredBySettings() {
        val activityController = Robolectric.buildActivity(FullscreenActivity::class.java)
            .create()
            .resume()
        val runningActivity = activityController.get()

        runningActivity.isCoveredByOtherActivity = true
        activityController.stop()

        assert(!runningActivity.isFinishing) { "Activity should not finish when covered by another activity" }
    }

    // --- Auto-brightness tests ---

    @Test
    fun computeAutoFactor_handlesNegativeLux_asZero() {
        val factor = FullscreenActivity.computeAutoFactor(-5f)
        assert(factor == 0f) { "Expected 0 for negative lux but was $factor" }
    }

    @Test
    fun computeAutoFactor_clampsLargeLux_toOne() {
        val factor = FullscreenActivity.computeAutoFactor(100000f)
        assert(factor == 1f) { "Expected 1 for very large lux but was $factor" }
    }

    @Test
    fun computeAutoFactor_zeroLux_returnsZero() {
        val factor = FullscreenActivity.computeAutoFactor(0f)
        assert(factor == 0f) { "Expected 0 for zero lux but was $factor" }
    }

    @Test
    fun computeAutoFactor_twoHundredLux_returnsOne() {
        val factor = FullscreenActivity.computeAutoFactor(200f)
        assert(factor == 1f) { "Expected 1 for 200 lux but was $factor" }
    }

    @Test
    fun sleepModeBrightness_isNotChangedByBurnInProtection_whenBurnInProtectionIsEnabled() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("burn_in", true).commit()
        prefs.edit().putInt("sleep_mode_brightness", 100).commit()

        activity.toggleSleepMode()

        val brightness = activity.window.attributes.screenBrightness
        assert(brightness == 0.5f) {
            "Expected brightness 0.5 regardless of burn-in protection, got $brightness"
        }
    }

    @Test
    fun sleepModeBrightness_mapsOneHundredPercentToHalfBrightness() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("burn_in", false).commit()
        prefs.edit().putInt("sleep_mode_brightness", 100).commit()

        activity.toggleSleepMode()

        val brightness = activity.window.attributes.screenBrightness
        assert(brightness == 0.5f) {
            "Expected brightness 0.5 for 100% sleep brightness, got $brightness"
        }
    }

    @Test
    fun autoBrightness_registersListener_whenLightSensorIsAvailable() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness_min", 50).commit()
        prefs.edit().putInt("sleep_mode_brightness_max", 50).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleSleepMode()

        assert(shadowManager.getListeners().isNotEmpty()) {
            "Expected sensor listener to be registered when a light sensor is available"
        }
    }

    @Test
    fun autoBrightness_doesNotRegisterListener_whenLightSensorIsUnavailable() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness_min", 50).commit()
        prefs.edit().putInt("sleep_mode_brightness_max", 50).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager

        activity.toggleSleepMode()

        assert(shadowManager.getListeners().isEmpty()) {
            "Expected no sensor listener when no light sensor is available"
        }
    }

    @Test
    fun autoBrightness_unregistersListener_whenSleepModeExits() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness_min", 50).commit()
        prefs.edit().putInt("sleep_mode_brightness_max", 50).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleSleepMode()
        assert(shadowManager.getListeners().isNotEmpty()) {
            "Expected listener to be registered after entering sleep mode"
        }

        activity.toggleSleepMode()
        assert(shadowManager.getListeners().isEmpty()) {
            "Expected sensor listener to be unregistered when sleep mode is exited"
        }
    }

    @Test
    fun autoBrightness_usesMaxRangeValue_atMaxLux() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness_min", 30).commit()
        prefs.edit().putInt("sleep_mode_brightness_max", 80).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleSleepMode()

        val event = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(200f))
            .build()
        shadowManager.sendSensorEventToListeners(event, lightSensor)

        // 200 lux → max range value (80/200 = 0.4)
        val brightness = activity.window.attributes.screenBrightness
        assert(brightness == 0.4f) {
            "Expected brightness 0.4 from max range value. Got $brightness"
        }
    }

    @Test
    fun autoBrightness_isNotChangedByBurnInProtection_whenBurnInProtectionIsEnabled() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("burn_in", true).commit()
        prefs.edit().putInt("sleep_mode_brightness_min", 100).commit()
        prefs.edit().putInt("sleep_mode_brightness_max", 100).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleSleepMode()

        val event = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(200f))
            .build()
        shadowManager.sendSensorEventToListeners(event, lightSensor)

        val brightness = activity.window.attributes.screenBrightness
        assert(brightness == 0.5f) {
            "Expected ranged brightness 0.5 regardless of burn-in protection. Got $brightness"
        }
    }

    @Test
    fun autoBrightness_scalesBrightnessBasedOnLux() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness_min", 20).commit()
        prefs.edit().putInt("sleep_mode_brightness_max", 80).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleSleepMode()

        // 0 lux → min range value (20/200 = 0.1)
        val event0Lux = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(0f))
            .build()
        shadowManager.sendSensorEventToListeners(event0Lux, lightSensor)
        val after0Lux = activity.window.attributes.screenBrightness
        assert(after0Lux == 0.1f) {
            "Expected brightness 0.1 at 0 lux but was $after0Lux"
        }

        // 10 lux: ln(11)/ln(201) ≈ 0.452 → brightness = 0.1 + (0.4 - 0.1) × factor
        val event10Lux = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(10f))
            .build()
        shadowManager.sendSensorEventToListeners(event10Lux, lightSensor)
        val after10Lux = activity.window.attributes.screenBrightness
        val expected10Lux = 0.1f + (0.3f * (Math.log(11.0) / Math.log(201.0)).toFloat())
        assert(Math.abs(after10Lux - expected10Lux) < 0.01f) {
            "Expected brightness ~$expected10Lux at 10 lux but was $after10Lux"
        }

        // 200 lux → max range value (80/200 = 0.4)
        val event200Lux = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(200f))
            .build()
        shadowManager.sendSensorEventToListeners(event200Lux, lightSensor)
        val after200Lux = activity.window.attributes.screenBrightness
        assert(after200Lux == 0.4f) {
            "Expected brightness 0.4 at 200 lux but was $after200Lux"
        }
    }

    @Test
    fun autoBrightness_unregistersListener_onPause() {
        val controller = Robolectric.buildActivity(FullscreenActivity::class.java)
            .create()
            .resume()
        val testActivity = controller.get()

        val prefs = testActivity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("sleep_mode_brightness_min", 50).commit()
        prefs.edit().putInt("sleep_mode_brightness_max", 50).commit()

        val sensorManager = testActivity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        testActivity.toggleSleepMode()
        assert(shadowManager.getListeners().isNotEmpty()) {
            "Expected listener to be registered before pause"
        }

        controller.pause()

        assert(shadowManager.getListeners().isEmpty()) {
            "Expected sensor listener to be unregistered on pause"
        }
    }

}
