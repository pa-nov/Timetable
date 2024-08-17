package com.panov.timetable.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.panov.timetable.R
import com.panov.timetable.Storage
import com.panov.util.Converter
import com.panov.util.UiUtils
import java.util.Locale
import kotlin.math.abs

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragment = inflater.inflate(R.layout.fragment_settings, container, false)

        fragment.findViewById<Button>(R.id.button_theme_system).setOnClickListener { setTheme(fragment, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        fragment.findViewById<Button>(R.id.button_theme_light).setOnClickListener { setTheme(fragment, AppCompatDelegate.MODE_NIGHT_NO) }
        fragment.findViewById<Button>(R.id.button_theme_dark).setOnClickListener { setTheme(fragment, AppCompatDelegate.MODE_NIGHT_YES) }
        setTheme(fragment, Storage.settings.getInt("app-theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM), true)


        fragment.findViewById<Button>(R.id.button_language_english).setOnClickListener { setLanguage(fragment, "en") }
        fragment.findViewById<Button>(R.id.button_language_russian).setOnClickListener { setLanguage(fragment, "ru") }
        setLanguage(fragment, Storage.settings.getString("app-language", Locale.getDefault().language), true)

        fragment.findViewById<Button>(R.id.button_initial_index_zero).setOnClickListener { setInitialIndex(fragment, 0) }
        fragment.findViewById<Button>(R.id.button_initial_index_one).setOnClickListener { setInitialIndex(fragment, 1) }
        setInitialIndex(fragment, Storage.settings.getInt("app-initial_index", 1), true)


        fragment.findViewById<SwitchMaterial>(R.id.switch_modifiers).setOnCheckedChangeListener { _, isChecked ->
            UiUtils.setViewVisibility(fragment.findViewById(R.id.layout_modifiers), if (isChecked) View.VISIBLE else View.GONE)
        }
        UiUtils.setupButtonGroup(arrayOf(fragment.findViewById(R.id.button_modifier_hour_round), fragment.findViewById(R.id.button_modifier_hour_set)))
        UiUtils.setupButtonGroup(arrayOf(fragment.findViewById(R.id.button_modifier_minute_round), fragment.findViewById(R.id.button_modifier_minute_set)))
        UiUtils.setupButtonGroup(arrayOf(fragment.findViewById(R.id.button_modifier_second_round), fragment.findViewById(R.id.button_modifier_second_set)))


        fragment.findViewById<View>(R.id.button_source_code).setOnClickListener {
            UiUtils.openURL(requireContext(), "https://github.com/pa-nov/Timetable")
        }
        fragment.findViewById<View>(R.id.button_timetable_editor).setOnClickListener {
            UiUtils.openURL(requireContext(), "https://github.com/pa-nov/TimetableEditor")
        }
        fragment.findViewById<TextView>(R.id.text_app_version).text = getString(
            R.string.app_version, requireContext().packageManager.getPackageInfo(requireContext().packageName, PackageManager.GET_ACTIVITIES).versionName
        )


        fragment.findViewById<Button>(R.id.button_action).setOnClickListener { saveSettings(fragment) }
        readSettings(fragment)

        return fragment
    }

    private fun setTheme(view: View, theme: Int, onlyRead: Boolean = false) {
        if (!onlyRead) {
            AppCompatDelegate.setDefaultNightMode(theme)
            Storage.settings.saveInt("app-theme", theme)
        }
        view.findViewById<Button>(R.id.button_theme_system).isEnabled = theme != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        view.findViewById<Button>(R.id.button_theme_light).isEnabled = theme != AppCompatDelegate.MODE_NIGHT_NO
        view.findViewById<Button>(R.id.button_theme_dark).isEnabled = theme != AppCompatDelegate.MODE_NIGHT_YES
    }

    private fun setLanguage(view: View, language: String, onlyRead: Boolean = false) {
        if (!onlyRead) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            Storage.settings.saveString("app-language", language)
        }
        view.findViewById<Button>(R.id.button_language_english).isEnabled = language != "en"
        view.findViewById<Button>(R.id.button_language_russian).isEnabled = language != "ru"
    }

    private fun setInitialIndex(view: View, index: Int, onlyRead: Boolean = false) {
        if (!onlyRead) {
            Storage.settings.saveInt("app-initial_index", index)
        }
        view.findViewById<Button>(R.id.button_initial_index_zero).isEnabled = index != 0
        view.findViewById<Button>(R.id.button_initial_index_one).isEnabled = index != 1
    }

    private fun readSettings(view: View) {
        view.findViewById<TextInputEditText>(R.id.input_timetable_json).setText(Storage.settings.getString("timetable-json"))

        val modifierHour = Storage.settings.getInt("widget-modifier_hour", 1)
        val modifierMinute = Storage.settings.getInt("widget-modifier_minute", 1)
        val modifierSecond = Storage.settings.getInt("widget-modifier_second", 1)

        view.findViewById<SwitchMaterial>(R.id.switch_modifiers).isChecked = !(modifierHour == 1 && modifierMinute == 1 && modifierSecond == 1)

        view.findViewById<TextInputEditText>(R.id.input_modifier_hour).setText(abs(modifierHour).toString())
        view.findViewById<TextInputEditText>(R.id.input_modifier_minute).setText(abs(modifierMinute).toString())
        view.findViewById<TextInputEditText>(R.id.input_modifier_second).setText(abs(modifierSecond).toString())

        view.findViewById<Button>(if (modifierHour > 0) R.id.button_modifier_hour_round else R.id.button_modifier_hour_set).performClick()
        view.findViewById<Button>(if (modifierMinute > 0) R.id.button_modifier_minute_round else R.id.button_modifier_minute_set).performClick()
        view.findViewById<Button>(if (modifierSecond > 0) R.id.button_modifier_second_round else R.id.button_modifier_second_set).performClick()
    }

    private fun saveSettings(view: View) {
        val timetableString = view.findViewById<TextInputEditText>(R.id.input_timetable_json).text.toString()
        Storage.settings.setString("timetable-json", timetableString)
        Storage.setTimetable(timetableString)

        if (view.findViewById<SwitchMaterial>(R.id.switch_modifiers).isChecked) {
            val modifierHour = Converter.getIntFromInput(view.findViewById(R.id.input_modifier_hour), 1)
            val modifierMinute = Converter.getIntFromInput(view.findViewById(R.id.input_modifier_minute), 1)
            val modifierSecond = Converter.getIntFromInput(view.findViewById(R.id.input_modifier_second), 1)
            Storage.settings.setInt(
                "widget-modifier_hour", if (view.findViewById<Button>(R.id.button_modifier_hour_round).isEnabled) -modifierHour else modifierHour
            )
            Storage.settings.setInt(
                "widget-modifier_minute", if (view.findViewById<Button>(R.id.button_modifier_minute_round).isEnabled) -modifierMinute else modifierMinute
            )
            Storage.settings.setInt(
                "widget-modifier_second", if (view.findViewById<Button>(R.id.button_modifier_second_round).isEnabled) -modifierSecond else modifierSecond
            )
        } else {
            Storage.settings.setInt("widget-modifier_hour", 1)
            Storage.settings.setInt("widget-modifier_minute", 1)
            Storage.settings.setInt("widget-modifier_second", 1)
        }

        Storage.settings.save()
        UiUtils.showToast(requireContext(), R.string.message_applied)
        UiUtils.clearFocus(requireView())
        readSettings(view)
    }
}