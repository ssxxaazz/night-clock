package com.ssxxaazz.nightclock

import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
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
    fun onPause_disablesDoNotDisturb_whenInNightMode() {
        val scenario = ActivityScenario.launch(FullscreenActivity::class.java)
        var scopedActivity: FullscreenActivity? = null

        scenario.onActivity { act ->
            scopedActivity = act
            val prefs = act.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("night_mode_brightness", 100).apply()

            val notificationManager = act.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Ensure DND is off initially
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

            // Enter night mode (enables DND)
            act.toggleNightMode()

            assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
                "Expected DND to be enabled after entering night mode"
            }
        }

        // Move to paused state
        scenario.moveToState(Lifecycle.State.STARTED)

        // Verify DND is disabled when paused
        val notificationManager = scopedActivity!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            "Expected DND to be disabled when activity is paused while in night mode"
        }

        scenario.close()
    }

    @Test
    fun onPause_doesNotDisableDoNotDisturb_ifUserHadItEnabled_beforeNightMode() {
        val scenario = ActivityScenario.launch(FullscreenActivity::class.java)
        var scopedActivity: FullscreenActivity? = null

        scenario.onActivity { act ->
            scopedActivity = act
            val prefs = act.getSharedPreferences("night_clock_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("night_mode_brightness", 100).apply()

            val notificationManager = act.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Simulate user had DND already enabled before
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)

            // Enter night mode
            act.toggleNightMode()
        }

        // Move to paused state
        scenario.moveToState(Lifecycle.State.STARTED)

        // Verify DND remains enabled since user had it on before
        val notificationManager = scopedActivity!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
            "Expected DND to remain enabled when paused (user had it on before night mode)"
        }

        scenario.close()
    }

    @Test
    fun onPause_doesNotDisableDoNotDisturb_ifNotInNightMode() {
        val scenario = ActivityScenario.launch(FullscreenActivity::class.java)
        var scopedActivity: FullscreenActivity? = null

        scenario.onActivity { act ->
            scopedActivity = act
            val notificationManager = act.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Ensure DND is on but not from the app
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        }

        // Move to paused state
        scenario.moveToState(Lifecycle.State.STARTED)

        // Verify DND state is unchanged
        val notificationManager = scopedActivity!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assert(notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
            "Expected DND state to remain unchanged when paused without night mode"
        }

        scenario.close()
    }
}
