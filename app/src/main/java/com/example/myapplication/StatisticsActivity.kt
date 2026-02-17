package com.example.myapplication

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        // 1. Podepnij widoki
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

        // 2. Wypełnij standardowe kafelki
        tvStreak.text = GameManager.currentStreak.toString()
        tvTotal.text = GameManager.totalTasksDone.toString()
        tvXp.text = GameManager.totalLifetimeXp.toString()
        tvLevel.text = GameManager.currentLevel.toString()

        // 3. Oblicz i wyświetl NOWE kreatywne statystyki
        tvBestDay.text = calculateBusiestDay()
        tvAvgXp.text = calculateAverageXp().toString()

        // 4. Obsługa przycisków wykresu
        setupChartButtons()

        // Domyślnie ładujemy 7 dni
        loadChartData(7)

        btnBack.setOnClickListener { finish() }
    }

    private fun setupChartButtons() {
        val activeColor = ColorStateList.valueOf(GameManager.appThemeColor)
        val inactiveColor = ColorStateList.valueOf(Color.parseColor("#B0BEC5"))

        btnWeek.setOnClickListener {
            loadChartData(7)
            btnWeek.backgroundTintList = activeColor; btnMonth.backgroundTintList = inactiveColor; btnYear.backgroundTintList = inactiveColor
        }
        btnMonth.setOnClickListener {
            loadChartData(30)
            btnWeek.backgroundTintList = inactiveColor; btnMonth.backgroundTintList = activeColor; btnYear.backgroundTintList = inactiveColor
        }
        btnYear.setOnClickListener {
            loadChartYearData() // Specjalna funkcja dla roku (grupowanie po miesiącach)
            btnWeek.backgroundTintList = inactiveColor; btnMonth.backgroundTintList = inactiveColor; btnYear.backgroundTintList = activeColor
        }
    }

    // Wykres dzienny (7 lub 30 dni)
    private fun loadChartData(days: Int) {
        // ZMIANA: Tekst na angielski
        tvTitle.text = "Activity (Last $days days)"

        val result = mutableListOf<Pair<String, Int>>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // ZMIANA: Wymuszenie Locale.ENGLISH dla dni tygodnia (Mon, Tue...)
        val labelFormat = if (days == 7) SimpleDateFormat("EEE", Locale.ENGLISH) else SimpleDateFormat("dd", Locale.ENGLISH)

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

    // Wykres roczny (grupowanie po miesiącach)
    private fun loadChartYearData() {
        // ZMIANA: Tekst na angielski
        tvTitle.text = "Activity (Last 12 months)"

        val result = mutableListOf<Pair<String, Int>>()

        // ZMIANA: Wymuszenie Locale.ENGLISH dla miesięcy (Jan, Feb...)
        val monthLabelFormat = SimpleDateFormat("MMM", Locale.ENGLISH)

        for (i in 11 downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -i)
            val monthLabel = monthLabelFormat.format(calendar.time)

            // Tutaj musimy sprawdzić, czy data w dzienniku (dd.MM.yyyy) pasuje do miesiąca
            val targetMonth = calendar.get(Calendar.MONTH)
            val targetYear = calendar.get(Calendar.YEAR)

            var count = 0
            GameManager.journalEntries.forEach { entry ->
                if (entry.type == 1) { // Tylko wykonane zadania
                    try {
                        val parts = entry.date.split(".") // [19, 12, 2025]
                        if (parts.size == 3) {
                            val entryMonth = parts[1].toInt() - 1 // Calendar.MONTH liczy od 0
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

    // --- KREATYWNE STATYSTYKI ---

    // Jaki dzień tygodnia jest Twoim ulubionym?
    private fun calculateBusiestDay(): String {
        // ZMIANA: Tekst na angielski
        if (GameManager.journalEntries.isEmpty()) return "No data"

        val dayCounts = mutableMapOf<Int, Int>() // Calendar.DAY_OF_WEEK -> Ilość
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

        // Znajdź dzień z max liczbą
        val bestDay = dayCounts.maxByOrNull { it.value }?.key ?: return "-"

        // Zamień int na nazwę (np. "Monday") - WYMUSZENIE ANGIELSKIEGO
        calendar.set(Calendar.DAY_OF_WEEK, bestDay)
        return SimpleDateFormat("EEEE", Locale.ENGLISH).format(calendar.time).replaceFirstChar { it.uppercase() }
    }

    // Ile średnio XP zdobywasz, w dni kiedy w ogóle grasz?
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