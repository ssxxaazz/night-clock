package com.ssxxaazz.nightclock

import android.app.NotificationManager
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowNotificationManager as BaseShadow

@Implements(NotificationManager::class)
class TestShadowNotificationManager : BaseShadow() {
    
    @RealObject
    private lateinit var notificationManager: NotificationManager
    
    override fun isNotificationPolicyAccessGranted(): Boolean {
        return true
    }
}
