package com.panov.timetable

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.Calendar
import android.os.Handler
import android.os.IBinder
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import com.panov.timetable.appwidget.ClockWidgetProvider
import com.panov.timetable.appwidget.LessonWidgetProvider
import com.panov.timetable.appwidget.TimetableWidgetProvider
import com.panov.util.Converter
import com.panov.util.SettingsData

class WidgetService : Service() {
    private lateinit var handler: Handler
    private var updateOnUnlock = false
    private var timetable = emptyArray<Int>()
    private var timer = 0L

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null && intent.action == Intent.ACTION_USER_PRESENT) {
                updateWidgets(context)
            }
        }
    }

    private val timetableUpdater = object : Runnable {
        override fun run() {
            updateWidgets(applicationContext)
            val calendar = AppUtils.getCalendar()
            val seconds = Converter.getSecondsInDay(calendar)

            for (index in timetable.indices) {
                if (timetable[index] > seconds) {
                    val delay = (timetable[index] - seconds) * 1000 - calendar.get(Calendar.MILLISECOND).toLong()
                    handler.postDelayed(this, delay)
                    return
                }
            }

            val delay = DateUtils.DAY_IN_MILLIS + (timetable[0] - seconds) * 1000 - calendar.get(Calendar.MILLISECOND)
            handler.postDelayed(this, delay)
        }
    }

    private val timerUpdater = object : Runnable {
        override fun run() {
            updateWidgets(applicationContext)
            val calendar = AppUtils.getCalendar()
            val minute = timer / 60 % 60
            val second = timer % 60

            var delay = timer
            if (timer >= 3600 && minute == 0L) delay -= calendar.get(Calendar.MINUTE)
            if (timer >= 60 && second == 0L) delay -= calendar.get(Calendar.SECOND)
            delay *= 1000
            delay -= calendar.get(Calendar.MILLISECOND)
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val settings = SettingsData(applicationContext)
        handler = Handler(mainLooper)
        updateOnUnlock = settings.getBoolean(Storage.Widgets.UPDATE_ON_UNLOCK)
        timetable = if (settings.getBoolean(Storage.Widgets.UPDATE_BY_TIMETABLE)) {
            val timetableData = AppUtils.getTimetableData(settings.getString(Storage.Timetable.JSON))
            if (timetableData != null) {
                val timetableList = arrayListOf<Int>()
                for (index in 0 until timetableData.getLessonsCount()) {
                    timetableList.add(timetableData.getLessonTimeStart(index))
                    timetableList.add(timetableData.getLessonTimeEnd(index))
                }
                timetableList.sort()
                timetableList.toTypedArray()
            } else {
                emptyArray<Int>()
            }
        } else {
            emptyArray<Int>()
        }
        timer = settings.getInt(Storage.Widgets.UPDATE_TIMER).toLong()

        if (updateOnUnlock) registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        if (timetable.isNotEmpty()) handler.post(timetableUpdater)
        if (timer > 0) handler.post(timerUpdater)

        val notificationChannel = NotificationChannel(
            "background_service", applicationContext.getString(R.string.title_notification_background), NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(startId, NotificationCompat.Builder(applicationContext, "background_service").build())
        return START_STICKY
    }

    override fun onDestroy() {
        if (updateOnUnlock) unregisterReceiver(unlockReceiver)
        if (timetable.isNotEmpty()) handler.removeCallbacks(timetableUpdater)
        if (timer > 0) handler.removeCallbacks(timerUpdater)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val lessonWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, LessonWidgetProvider::class.java))
        val clockWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
        val timetableWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TimetableWidgetProvider::class.java))

        LessonWidgetProvider().onUpdate(context, appWidgetManager, lessonWidgetIds)
        ClockWidgetProvider().onUpdate(context, appWidgetManager, clockWidgetIds)
        TimetableWidgetProvider().onUpdate(context, appWidgetManager, timetableWidgetIds)
    }
}