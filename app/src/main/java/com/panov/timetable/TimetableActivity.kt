package com.panov.timetable

import android.annotation.SuppressLint
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.json.JSONObject

class TimetableActivity : AppCompatActivity() {
    private var date = Calendar.getInstance()
    private var jsonData = JSONObject()
    private var initialIndex = 1
    private var tempPosition = 0


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timetable)
        date.firstDayOfWeek = Calendar.MONDAY
        date.minimalDaysInFirstWeek = 4
        val timetableActivity: TimetableActivity = this

        val savedData      = getSharedPreferences("SavedData", 0)
        val jsonDataString = savedData.getString("Json", "")
        if (jsonDataString.isNullOrEmpty()) { this.finish() }
        jsonData           = JSONObject(jsonDataString!!)
        initialIndex       = savedData.getInt("InitialIndex", 1)

        findViewById<ImageButton>(R.id.buttonReturn).setOnClickListener { this.finish() }

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = TimetablePageAdapter(timetableActivity)
        viewPager.setCurrentItem(1, false)
        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tempPosition = position - 1
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE){
                    super.onPageScrollStateChanged(state)
                    date.add(Calendar.DAY_OF_YEAR, tempPosition)
                    tempPosition = 0
                    viewPager.adapter = null
                    viewPager.adapter = TimetablePageAdapter(timetableActivity)
                    viewPager.setCurrentItem(1, false)
                }
            }
        })
    }

    override fun onDestroy() {
        val editor = applicationContext.getSharedPreferences("SavedData", 0).edit()
        editor.putInt("LastPage", 0)
        editor.apply()
        super.onDestroy()
    }


    @SuppressLint("SetTextI18n", "InflateParams")
    private fun updateView(linearLayout: LinearLayout, position: Int) {
        if (linearLayout.childCount > 2) { linearLayout.removeViewsInLayout(2, linearLayout.childCount - 2) }
        val tempDate: Calendar = date.clone() as Calendar
        tempDate.add(Calendar.DAY_OF_YEAR, position - 1)

        val weekdays        = arrayOf( this.getString(R.string.weekday_monday), this.getString(R.string.weekday_tuesday), this.getString(R.string.weekday_wednesday), this.getString(R.string.weekday_thursday), this.getString(R.string.weekday_friday), this.getString(R.string.weekday_saturday), this.getString(R.string.weekday_sunday) )
        val dateDayOfWeek   = if (tempDate.get(Calendar.DAY_OF_WEEK) > 1) { tempDate.get(Calendar.DAY_OF_WEEK) - 2 } else { 6 }
        val dateWeekEvenOdd = if (tempDate.get(Calendar.WEEK_OF_YEAR) % 2 == 0) { "even" } else { "odd" }

        val times     = jsonData.getJSONArray("times")
        val lessons   = jsonData.getJSONArray("lessons")
        val timetable = jsonData.getJSONArray(dateWeekEvenOdd).getJSONArray(dateDayOfWeek)

        linearLayout.findViewById<TextView>(R.id.textDayOfWeek).text = weekdays[dateDayOfWeek]

        for (i: Int in 0 until times.length()) {
            val timetableLesson = layoutInflater.inflate(R.layout.timetable_lesson, null)

            val currentTime = times.getJSONObject(i)
            val startHour   = getTwoDigitNumber(currentTime.getInt("startHour"))
            val startMinute = getTwoDigitNumber(currentTime.getInt("startMinute"))
            val endHour     = getTwoDigitNumber(currentTime.getInt("endHour"))
            val endMinute   = getTwoDigitNumber(currentTime.getInt("endMinute"))

            timetableLesson.findViewById<TextView>(R.id.textIndex).text = "${i + initialIndex}"
            timetableLesson.findViewById<TextView>(R.id.textTime).text = "${startHour}:${startMinute} ${endHour}:${endMinute}"

            val textName    = timetableLesson.findViewById<TextView>(R.id.textName)
            val textTeacher = timetableLesson.findViewById<TextView>(R.id.textTeacher)
            val textRoom    = timetableLesson.findViewById<TextView>(R.id.textRoom)

            if (timetable.getInt(i) > 0) {
                val currentLesson = lessons.getJSONArray(timetable.getInt(i))
                textName.text    = currentLesson.getString(0)
                textTeacher.text = currentLesson.getString(2)
                textRoom.text    = "(${currentLesson.getString(1)})"
            } else {
                textName.visibility    = View.INVISIBLE
                textTeacher.visibility = View.INVISIBLE
                textRoom.visibility    = View.INVISIBLE
            }

            linearLayout.addView(timetableLesson)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun updateMainView() {
        val dateDay   = getTwoDigitNumber(date.get(Calendar.DAY_OF_MONTH))
        val dateMonth = getTwoDigitNumber(date.get(Calendar.MONTH) + 1)
        val dateYear  = date.get(Calendar.YEAR)
        val dateWeek  = getTwoDigitNumber(date.get(Calendar.WEEK_OF_YEAR))

        findViewById<TextView>(R.id.textDate).text = "$dateDay.$dateMonth.$dateYear ($dateWeek)"
    }

    private fun getTwoDigitNumber(number: Int): String {
        if (number < 10) { return "0$number" }
        return number.toString()
    }


    class TimetablePageAdapter( private val timetableActivity: TimetableActivity) : RecyclerView.Adapter<TimetablePageAdapter.TimetablePage>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetablePage {
            return TimetablePage(LayoutInflater.from(parent.context).inflate(R.layout.timetable_page, parent, false))
        }

        override fun getItemCount(): Int {
            return 3
        }

        override fun onBindViewHolder(holder: TimetablePage, position: Int) {
            timetableActivity.updateView(holder.itemView.findViewById(R.id.linearLayout), position)
            if (position == 1) { timetableActivity.updateMainView() }
        }

        class TimetablePage(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}