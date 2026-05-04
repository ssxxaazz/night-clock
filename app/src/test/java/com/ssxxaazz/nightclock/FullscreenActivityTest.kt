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
        prefs.edit().putInt("night_mode_brightness", 100).apply()

        assert(!activity.lowBrightness)

        activity.toggleNightMode()

        val attrs = activity.window.attributes
        assert(attrs.screenBrightness < 1.0f)
        assert(activity.lowBrightness)
    }

    @Test
    fun longPress_showsNightModeToast_whenEnteringNightMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("night_mode_brightness", 100).apply()

        ShadowToast.reset()
        activity.toggleNightMode()

        val latestToast = ShadowToast.getLatestToast()
        assert(latestToast != null) { "Expected a toast to be shown" }
        assert(ShadowToast.getTextOfLatestToast() == "Night Mode") { 
            "Expected toast message 'Night Mode' but got '${ShadowToast.getTextOfLatestToast()}'" 
        }
    }

    @Test
    fun longPress_showsDayModeToast_whenExitingNightMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("night_mode_brightness", 100).apply()

        // Enter night mode first
        activity.toggleNightMode()
        ShadowToast.reset()
        
        // Exit night mode (back to day mode)
        activity.toggleNightMode()

        val latestToast = ShadowToast.getLatestToast()
        assert(latestToast != null) { "Expected a toast to be shown" }
        assert(ShadowToast.getTextOfLatestToast() == "Day Mode") { 
            "Expected toast message 'Day Mode' but got '${ShadowToast.getTextOfLatestToast()}'" 
        }
    }

    @Test
    fun longPress_enablesDoNotDisturb_whenEnteringNightMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("night_mode_brightness", 100).apply()

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Ensure DND is off initially
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

        activity.toggleNightMode()

        // Verify DND is enabled (alarms only - not all interruptions)
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
            "Expected DND to be enabled when entering night mode"
        }
    }

    @Test
    fun longPress_disablesDoNotDisturb_whenExitingNightMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("night_mode_brightness", 100).apply()

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Ensure DND is off initially (not enabled by app before entering night mode)
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

        // Enter night mode (enables DND)
        activity.toggleNightMode()
        
        // Exit night mode (should disable DND since app enabled it)
        activity.toggleNightMode()

        // Verify DND is disabled (all - normal)
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            "Expected DND to be disabled when exiting night mode"
        }
    }

    @Test
    fun longPress_doesNotDisableDoNotDisturb_ifItWasAlreadyEnabled_beforeNightMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("night_mode_brightness", 100).apply()

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Simulate user had DND already enabled (alarms only) before app enters night mode
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)

        // Enter night mode (app will see DND is already not all)
        activity.toggleNightMode()
        
        // Exit night mode (should NOT disable DND since it was already enabled by user)
        activity.toggleNightMode()

        // Verify DND is still enabled (alarms only - user had it on, app should not turn it off)
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
            "Expected DND to remain enabled when exiting night mode (it was already enabled before)"
        }
    }

    @Test
    fun longPress_doesNotDisableDoNotDisturb_ifItWasTotalDnd_beforeNightMode() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("night_mode_brightness", 100).apply()

        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Simulate user had total DND (none) before app enters night mode
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)

        // Enter night mode (app will see DND is already none)
        activity.toggleNightMode()
        
        // Exit night mode (should NOT disable DND since it was already enabled by user)
        activity.toggleNightMode()

        // Verify DND is still enabled (none - user had total DND, app should not turn it off)
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
            "Expected DND to remain enabled when exiting night mode (it was total DND before)"
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
    fun autoBrightness_doesNotRegisterListener_whenPrefIsFalse() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_brightness", false).commit()
        prefs.edit().putInt("night_mode_brightness", 50).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleNightMode()

        assert(shadowManager.getListeners().isEmpty()) {
            "Expected no sensor listeners when auto-brightness is disabled"
        }
    }

    @Test
    fun autoBrightness_registersListener_whenEnabledAndNightModeOn() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_brightness", true).commit()
        prefs.edit().putInt("night_mode_brightness", 50).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleNightMode()

        assert(shadowManager.getListeners().isNotEmpty()) {
            "Expected sensor listener to be registered when auto-brightness is enabled and night mode is on"
        }
    }

    @Test
    fun autoBrightness_unregistersListener_whenNightModeExits() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_brightness", true).commit()
        prefs.edit().putInt("night_mode_brightness", 50).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleNightMode()
        assert(shadowManager.getListeners().isNotEmpty()) {
            "Expected listener to be registered after entering night mode"
        }

        activity.toggleNightMode()
        assert(shadowManager.getListeners().isEmpty()) {
            "Expected sensor listener to be unregistered when night mode is exited"
        }
    }

    @Test
    fun autoBrightness_ignoresManualSetting_whenAutoIsOn() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_brightness", true).commit()
        prefs.edit().putInt("night_mode_brightness", 30).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleNightMode()

        val event = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(200f))
            .build()
        shadowManager.sendSensorEventToListeners(event, lightSensor)

        // 200 lux → 100% equivalent (0.2), manual setting of 30% must be ignored
        val brightness = activity.window.attributes.screenBrightness
        assert(brightness == 0.2f) {
            "Expected brightness 0.2 (100% equivalent) ignoring manual 30% setting. Got $brightness"
        }
    }

    @Test
    fun autoBrightness_scalesBrightnessBasedOnLux() {
        val prefs = activity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_brightness", true).commit()
        prefs.edit().putInt("night_mode_brightness", 50).commit()

        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        activity.toggleNightMode()

        // 0 lux → 0% equivalent → brightness 0
        val event0Lux = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(0f))
            .build()
        shadowManager.sendSensorEventToListeners(event0Lux, lightSensor)
        val after0Lux = activity.window.attributes.screenBrightness
        assert(after0Lux == 0f) {
            "Expected brightness 0 at 0 lux (0% manual equivalent) but was $after0Lux"
        }

        // 10 lux: ln(11)/ln(201) ≈ 0.452 → brightness = 0.452 × 0.2 ≈ 0.0904
        val event10Lux = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(10f))
            .build()
        shadowManager.sendSensorEventToListeners(event10Lux, lightSensor)
        val after10Lux = activity.window.attributes.screenBrightness
        val expected10Lux = 0.2f * (Math.log(11.0) / Math.log(201.0)).toFloat()
        assert(Math.abs(after10Lux - expected10Lux) < 0.01f) {
            "Expected brightness ~$expected10Lux at 10 lux but was $after10Lux"
        }

        // 200 lux → factor 1.0 → brightness = 0.2 (100% equivalent)
        val event200Lux = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(200f))
            .build()
        shadowManager.sendSensorEventToListeners(event200Lux, lightSensor)
        val after200Lux = activity.window.attributes.screenBrightness
        assert(after200Lux == 0.2f) {
            "Expected brightness 0.2 at 200 lux (100% manual equivalent) but was $after200Lux"
        }
    }

    @Test
    fun autoBrightness_unregistersListener_onPause() {
        val controller = Robolectric.buildActivity(FullscreenActivity::class.java)
            .create()
            .resume()
        val testActivity = controller.get()

        val prefs = testActivity.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_brightness", true).commit()
        prefs.edit().putInt("night_mode_brightness", 50).commit()

        val sensorManager = testActivity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadowManager = Shadows.shadowOf(sensorManager) as ShadowSensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        shadowManager.addSensor(lightSensor)

        testActivity.toggleNightMode()
        assert(shadowManager.getListeners().isNotEmpty()) {
            "Expected listener to be registered before pause"
        }

        controller.pause()

        assert(shadowManager.getListeners().isEmpty()) {
            "Expected sensor listener to be unregistered on pause"
        }
    }

}
