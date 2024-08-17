package com.panov.timetable

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.panov.util.SettingsData
import java.util.Locale

class TimetableApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Storage.settings = SettingsData(applicationContext)
        Storage.setTimetable(Storage.settings.getString("timetable-json"))
        AppCompatDelegate.setDefaultNightMode(Storage.settings.getInt("app-theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(Storage.settings.getString("app-language", Locale.getDefault().language)))
    }
}