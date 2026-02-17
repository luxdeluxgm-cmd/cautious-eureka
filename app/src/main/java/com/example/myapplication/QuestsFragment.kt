package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.util.Calendar

class QuestsFragment : Fragment() {

    private lateinit var llDaily: LinearLayout
    private lateinit var llWeekly: LinearLayout
    private lateinit var llYearly: LinearLayout
    private lateinit var tvDailyTimer: TextView
    private lateinit var tvWeeklyTimer: TextView
    private lateinit var tvYearlyTimer: TextView
    private lateinit var pbDaily: ProgressBar
    private lateinit var pbWeekly: ProgressBar
    private lateinit var pbYearly: ProgressBar
    private var countdownTimer: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_quests, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        llDaily = view.findViewById(R.id.ll_daily_quests)
        llWeekly = view.findViewById(R.id.ll_weekly_quests)
        llYearly = view.findViewById(R.id.ll_yearly_quests)
        tvDailyTimer = view.findViewById(R.id.tv_daily_timer)
        tvWeeklyTimer = view.findViewById(R.id.tv_weekly_timer)
        tvYearlyTimer = view.findViewById(R.id.tv_yearly_timer)
        pbDaily = view.findViewById(R.id.progressBar_daily)
        pbWeekly = view.findViewById(R.id.progressBar_weekly)
        pbYearly = view.findViewById(R.id.progressBar_yearly)

        setupAddButtons(view)
        refreshQuestLists()
        startRealTimeTimer()
    }

    override fun onResume() {
        super.onResume()
        val colorState = ColorStateList.valueOf(GameManager.appThemeColor)

        pbDaily.progressTintList = colorState
        pbWeekly.progressTintList = colorState
        pbYearly.progressTintList = colorState

        view?.findViewById<ImageButton>(R.id.btn_add_daily)?.imageTintList = colorState
        view?.findViewById<ImageButton>(R.id.btn_add_weekly)?.imageTintList = colorState
        view?.findViewById<ImageButton>(R.id.btn_add_yearly)?.imageTintList = colorState

        refreshQuestLists()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
    }

    private fun startRealTimeTimer() {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millis: Long) {
                val ctx = context ?: return
                updateTimersUI()
                if (GameManager.checkAndResetQuests(ctx)) {
                    refreshQuestLists()
                    Toast.makeText(ctx, "Quests reset!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFinish() {}
        }.start()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimersUI() {
        val now = Calendar.getInstance()

        // Dzienny
        val endDay = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
        val diffD = endDay.timeInMillis - now.timeInMillis
        tvDailyTimer.text = "${diffD / 3600000}h ${(diffD / 60000) % 60}m"
        pbDaily.progress = (100 - (diffD * 100 / 86400000)).toInt()

        // Tygodniowy
        val endWeek = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }
        val diffW = endWeek.timeInMillis - now.timeInMillis
        tvWeeklyTimer.text = "${diffW / 86400000} days"
        pbWeekly.progress = (100 - (diffW * 100 / 604800000)).toInt()

        // Roczny
        val endYear = Calendar.getInstance().apply { set(Calendar.MONTH, 11); set(Calendar.DAY_OF_MONTH, 31); set(Calendar.HOUR_OF_DAY, 23) }
        val diffY = endYear.timeInMillis - now.timeInMillis
        tvYearlyTimer.text = "${diffY / 86400000} days"
        pbYearly.progress = (100 - (diffY * 100 / 31536000000L)).toInt()
    }

    private fun setupAddButtons(view: View) {
        view.findViewById<ImageButton>(R.id.btn_add_daily).setOnClickListener { showAddQuestDialog(0) }
        view.findViewById<ImageButton>(R.id.btn_add_weekly).setOnClickListener { showAddQuestDialog(1) }
        view.findViewById<ImageButton>(R.id.btn_add_yearly).setOnClickListener { showAddQuestDialog(2) }
    }

    private fun showAddQuestDialog(type: Int) {
        val dialog = AddQuestDialogFragment.newInstance(type)
        dialog.show(childFragmentManager, AddQuestDialogFragment.TAG)
    }

    fun refreshQuestLists() {
        llDaily.removeAllViews(); llWeekly.removeAllViews(); llYearly.removeAllViews()

        fun add(q: Quest, originalList: MutableList<Quest>, layout: LinearLayout) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_quest, layout, false)
            val cb = view.findViewById<CheckBox>(R.id.cb_quest)
            val tvTag = view.findViewById<TextView>(R.id.tv_quest_tag)
            val tvXp = view.findViewById<TextView>(R.id.tv_quest_xp)
            val btnDel = view.findViewById<ImageButton>(R.id.btn_delete_quest)

            cb.text = q.title
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = q.isCompleted
            tvXp.text = "${q.xp} XP"

            cb.buttonTintList = ColorStateList.valueOf(GameManager.appThemeColor)

            // === POPRAWKA WYGLĄDU ZAZNACZANIA ===
            if (q.isCompleted) {
                // Przekreślamy tekst
                cb.paintFlags = cb.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                // Zmieniamy kolor tekstu na szary
                cb.setTextColor(Color.GRAY)
                // WAŻNE: Pełna widoczność (brak szarej poświaty na tle)
                view.alpha = 1.0f
            } else {
                // Brak przekreślenia
                cb.paintFlags = cb.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                // Tekst biały
                cb.setTextColor(Color.WHITE)
                view.alpha = 1.0f
            }
            // ====================================

            if (!q.tagName.isNullOrEmpty()) {
                tvTag.text = q.tagName
                tvTag.visibility = View.VISIBLE
                q.tagColor?.let { tvTag.backgroundTintList = ColorStateList.valueOf(it) }
            } else {
                tvTag.visibility = View.GONE
            }

            cb.setOnCheckedChangeListener { _, isChecked ->
                q.isCompleted = isChecked
                if (isChecked) GameManager.addXp(q.xp, q.title, requireContext())
                else GameManager.removeXp(q.xp, q.title, requireContext())
                refreshQuestLists()
            }

            btnDel.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Delete") { _, _ ->
                        originalList.remove(q)
                        GameManager.saveGame(requireContext())
                        refreshQuestLists()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            layout.addView(view)
        }

        val sortedDaily = GameManager.dailyQuests.sortedWith(compareBy<Quest> { it.isCompleted }.thenBy { it.xp })
        val sortedWeekly = GameManager.weeklyQuests.sortedWith(compareBy<Quest> { it.isCompleted }.thenBy { it.xp })
        val sortedYearly = GameManager.yearlyQuests.sortedWith(compareBy<Quest> { it.isCompleted }.thenBy { it.xp })

        sortedDaily.forEach { add(it, GameManager.dailyQuests, llDaily) }
        sortedWeekly.forEach { add(it, GameManager.weeklyQuests, llWeekly) }
        sortedYearly.forEach { add(it, GameManager.yearlyQuests, llYearly) }
    }

    fun addQuestFromDialog(title: String, difficulty: String, tagName: String?, tagColor: Int?, type: Int) {
        // === POPRAWKA XP POD ANGIELSKI ===
        val xp = when (type) {
            0 -> when {
                difficulty.contains("Easy") -> 10  // Było "Łatwe"
                difficulty.contains("Hard") -> 50  // Było "Trudne"
                else -> 30 // Medium
            }
            1 -> when {
                difficulty.contains("Easy") -> 100
                difficulty.contains("Hard") -> 750
                else -> 250
            }
            2 -> when {
                difficulty.contains("Easy") -> 2500
                difficulty.contains("Hard") -> 10000
                else -> 5000
            }
            else -> 10
        }

        val newQuest = Quest(title, xp, false, type, tagName, tagColor)
        when (type) {
            0 -> GameManager.dailyQuests.add(newQuest)
            1 -> GameManager.weeklyQuests.add(newQuest)
            2 -> GameManager.yearlyQuests.add(newQuest)
        }
        GameManager.saveGame(requireContext())
        refreshQuestLists()
    }
}