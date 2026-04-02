package com.example.myapplication

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var chartView: StatsView
    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button
    private lateinit var btnYear: Button
    private lateinit var tvTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation)
        setContentView(R.layout.activity_statistics)

        chartView = findViewById(R.id.stats_chart_view)
        btnWeek = findViewById(R.id.btn_range_week)
        btnMonth = findViewById(R.id.btn_range_month)
        btnYear = findViewById(R.id.btn_range_year)
        tvTitle = findViewById(R.id.tv_chart_title)

        val tvStreak = findViewById<TextView>(R.id.tv_stat_streak)
        val tvTotal = findViewById<TextView>(R.id.tv_stat_total)
        val tvXp = findViewById<TextView>(R.id.tv_stat_xp)
        val tvLevel = findViewById<TextView>(R.id.tv_stat_level)
        val tvBestDay = findViewById<TextView>(R.id.tv_stat_best_day)
        val tvAvgXp = findViewById<TextView>(R.id.tv_stat_avg_xp)
        val btnBack = findViewById<ImageButton>(R.id.btn_back_stats)

        tvStreak.text = GameManager.currentStreak.toString()
        tvTotal.text = GameManager.totalTasksDone.toString()
        tvXp.text = GameManager.totalLifetimeXp.toString()
        tvLevel.text = GameManager.currentLevel.toString()

        tvBestDay.text = calculateBusiestDay()
        tvAvgXp.text = calculateAverageXp().toString()

        setupChartButtons()

        // Wywołujemy to sztucznie, by zasymulować kliknięcie na "Week" przy starcie
        btnWeek.performClick()

        btnBack.setOnClickListener { finish() }
    }

    private fun setupChartButtons() {
        // === MAGIA KOLORÓW DARK PREMIUM ===
        val activeColor = ColorStateList.valueOf(GameManager.appThemeColor)
        val inactiveColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bg_dark_surface_elevated))

        val textColorActive = ContextCompat.getColor(this, R.color.bg_dark_base)
        val textColorInactive = ContextCompat.getColor(this, R.color.text_primary)
        // ==================================

        btnWeek.setOnClickListener {
            loadChartData(7)

            btnWeek.backgroundTintList = activeColor
            btnWeek.setTextColor(textColorActive)

            btnMonth.backgroundTintList = inactiveColor
            btnMonth.setTextColor(textColorInactive)

            btnYear.backgroundTintList = inactiveColor
            btnYear.setTextColor(textColorInactive)
        }

        btnMonth.setOnClickListener {
            loadChartData(30)

            btnWeek.backgroundTintList = inactiveColor
            btnWeek.setTextColor(textColorInactive)

            btnMonth.backgroundTintList = activeColor
            btnMonth.setTextColor(textColorActive)

            btnYear.backgroundTintList = inactiveColor
            btnYear.setTextColor(textColorInactive)
        }

        btnYear.setOnClickListener {
            loadChartYearData()

            btnWeek.backgroundTintList = inactiveColor
            btnWeek.setTextColor(textColorInactive)

            btnMonth.backgroundTintList = inactiveColor
            btnMonth.setTextColor(textColorInactive)

            btnYear.backgroundTintList = activeColor
            btnYear.setTextColor(textColorActive)
        }
    }

    private fun loadChartData(days: Int) {
        tvTitle.text = "Aktywność ($days dni)"

        val result = mutableListOf<Pair<String, Int>>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val labelFormat = if (days == 7) SimpleDateFormat("EEE", Locale.getDefault()) else SimpleDateFormat("dd", Locale.getDefault())

        for (i in (days - 1) downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(calendar.time)
            val label = labelFormat.format(calendar.time)

            val count = GameManager.journalEntries.count { it.date == dateStr && it.type == 1 }
            result.add(Pair(label, count))
        }
        chartView.setData(result)
    }

    private fun loadChartYearData() {
        tvTitle.text = "Aktywność (12 miesięcy)"

        val result = mutableListOf<Pair<String, Int>>()

        val monthLabelFormat = SimpleDateFormat("MMM", Locale.getDefault())

        for (i in 11 downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -i)
            val monthLabel = monthLabelFormat.format(calendar.time)

            val targetMonth = calendar.get(Calendar.MONTH)
            val targetYear = calendar.get(Calendar.YEAR)

            var count = 0
            GameManager.journalEntries.forEach { entry ->
                if (entry.type == 1) {
                    try {
                        val parts = entry.date.split(".")
                        if (parts.size == 3) {
                            val entryMonth = parts[1].toInt() - 1
                            val entryYear = parts[2].toInt()
                            if (entryMonth == targetMonth && entryYear == targetYear) {
                                count++
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
            result.add(Pair(monthLabel, count))
        }
        chartView.setData(result)
    }

    private fun calculateBusiestDay(): String {
        if (GameManager.journalEntries.isEmpty()) return "Brak danych"

        val dayCounts = mutableMapOf<Int, Int>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        GameManager.journalEntries.forEach { entry ->
            if (entry.type == 1) {
                try {
                    calendar.time = dateFormat.parse(entry.date)!!
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    dayCounts[dayOfWeek] = dayCounts.getOrDefault(dayOfWeek, 0) + 1
                } catch (e: Exception) {}
            }
        }

        if (dayCounts.isEmpty()) return "-"

        val bestDay = dayCounts.maxByOrNull { it.value }?.key ?: return "-"

        calendar.set(Calendar.DAY_OF_WEEK, bestDay)
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time).replaceFirstChar { it.uppercase() }
    }

    private fun calculateAverageXp(): Int {
        val completedEntries = GameManager.journalEntries.filter { it.type == 1 }
        if (completedEntries.isEmpty()) return 0

        val activeDays = completedEntries.map { it.date }.distinct().size
        if (activeDays == 0) return 0

        val totalTasks = completedEntries.size
        val avgTasks = totalTasks / activeDays

        return avgTasks * 40
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down)
    }
}