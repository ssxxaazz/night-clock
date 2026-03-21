package com.ssxxaazz.nightclock

import android.app.AlarmManager
import android.content.Context

object AlarmFilter {

    fun isClockAlarm(alarmInfo: AlarmManager.AlarmClockInfo?): Boolean {
        if (alarmInfo == null) return false
        val packageName = alarmInfo.showIntent?.creatorPackage ?: return true
        return packageName.contains("clock", ignoreCase = true)
    }

    fun getNextAlarm(context: Context): AlarmManager.AlarmClockInfo? {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.nextAlarmClock
    }
}
