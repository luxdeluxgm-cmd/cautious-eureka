package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// === KLASY DANYCH ===

data class Quest(
    val title: String, val xp: Int, var isCompleted: Boolean,
    val type: Int, val tagName: String? = null, val tagColor: Int? = null
)

data class JournalEntry(
    val title: String, val content: String, val type: Int,
    val date: String, val time: String = "",
    val imageUri: String? = null, val imageBase64: String? = null
)

data class FullGameState(
    val nickname: String, val description: String, val avatarUri: String,
    val avatarBase64Data: String?, val isFirstRun: Boolean, val currentLevel: Int,
    val currentXp: Int,
    val totalLifetimeXp: Int,
    val totalTasksDone: Int, val currentStreak: Int,
    val taskDoneToday: Boolean, val lastLoginDay: Int, val lastLoginWeek: Int,
    val lastLoginYear: Int, val dailyQuests: List<Quest>, val weeklyQuests: List<Quest>,
    val yearlyQuests: List<Quest>, val journalEntries: List<JournalEntry>
)

object GameManager {
    private const val PREFS_NAME = "RpgLifeSave"
    private val gson = Gson()

    var nickname: String = "Bezimienny"; var description: String = "..."
    var avatarUri: String = ""; var isFirstRun: Boolean = true

    var currentLevel = 1
    var currentXp = 0
    var totalLifetimeXp = 0

    var totalTasksDone = 0; var currentStreak = 0; var taskDoneToday = false

    private var lastLoginDay = -1; private var lastLoginWeek = -1; private var lastLoginYear = -1
    var reminderHour: Int = 20; var reminderMinute: Int = 0

    var dailyQuests = mutableListOf<Quest>()
    var weeklyQuests = mutableListOf<Quest>()
    var yearlyQuests = mutableListOf<Quest>()
    var journalEntries = mutableListOf<JournalEntry>()
    var appLanguage: String = "PL"
    var appThemeColor: Int = -10011977

    fun getNextLevelThreshold(level: Int): Int {
        return 750 + ((level - 1) * 50)
    }

    fun addXp(amount: Int, questTitle: String, context: Context) {
        currentXp += amount
        totalLifetimeXp += amount
        totalTasksDone++

        if (!taskDoneToday) { taskDoneToday = true; currentStreak++ }

        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        journalEntries.add(0, JournalEntry("Completed: $questTitle", "Reward: +$amount XP", 1, date, time))

        AnimationHelper.showXpToast(context, amount)
        checkForLevelUp(context)
        saveGame(context)
    }

    fun removeXp(amount: Int, questTitle: String, context: Context) {
        currentXp -= amount
        totalLifetimeXp -= amount
        if (totalLifetimeXp < 0) totalLifetimeXp = 0

        if (totalTasksDone > 0) totalTasksDone--

        val entry = journalEntries.firstOrNull { it.title == "Completed: $questTitle" && it.type == 1 }

        if (entry != null) journalEntries.remove(entry)

        while (currentXp < 0) {
            if (currentLevel > 1) {
                currentLevel--
                currentXp += getNextLevelThreshold(currentLevel)
            } else {
                currentXp = 0
                break
            }
        }
        saveGame(context)
    }

    private fun checkForLevelUp(context: Context) {
        var leveledUp = false
        var threshold = getNextLevelThreshold(currentLevel)

        while (currentXp >= threshold) {
            currentXp -= threshold
            currentLevel++
            leveledUp = true
            threshold = getNextLevelThreshold(currentLevel)
        }
        if (leveledUp) AnimationHelper.showLevelUpDialog(context, currentLevel)
    }

    private fun recalculateTotalXp() {
        if (currentLevel > 1 && totalLifetimeXp <= currentXp) {
            var calculatedTotal = currentXp
            for (i in 1 until currentLevel) {
                calculatedTotal += getNextLevelThreshold(i)
            }
            totalLifetimeXp = calculatedTotal
        }
    }

    fun checkAndResetQuests(context: Context): Boolean {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_YEAR)
        val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
        val currentYear = now.get(Calendar.YEAR)

        var changed = false

        if (lastLoginDay != -1 && (lastLoginDay != currentDay || lastLoginYear != currentYear)) {
            val lastLoginDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, lastLoginYear)
                set(Calendar.DAY_OF_YEAR, lastLoginDay)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val todayDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.DAY_OF_YEAR, currentDay)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = todayDate.timeInMillis - lastLoginDate.timeInMillis
            val diffDays = diffMillis / (24 * 60 * 60 * 1000)

            if (diffDays <= 1L) {
                if (!taskDoneToday) { currentStreak = 0 }
            } else {
                currentStreak = 0
            }

            dailyQuests.forEach { it.isCompleted = false }
            taskDoneToday = false
            changed = true

        } else if (lastLoginDay == -1) {
            lastLoginDay = currentDay
            lastLoginYear = currentYear
        }

        if (lastLoginWeek != -1 && lastLoginWeek != currentWeek) {
            weeklyQuests.forEach { it.isCompleted = false }
            changed = true
        }

        if (lastLoginYear != -1 && lastLoginYear != currentYear) {
            yearlyQuests.forEach { it.isCompleted = false }
            changed = true
        }

        if (changed || lastLoginDay != currentDay) {
            lastLoginDay = currentDay
            lastLoginWeek = currentWeek
            lastLoginYear = currentYear
            saveGame(context)
        }
        return changed
    }

    fun saveGame(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        prefs.putString("KEY_DAILY", gson.toJson(dailyQuests))
        prefs.putString("KEY_WEEKLY", gson.toJson(weeklyQuests))
        prefs.putString("KEY_YEARLY", gson.toJson(yearlyQuests))
        prefs.putString("KEY_JOURNAL", gson.toJson(journalEntries))

        prefs.putInt("KEY_LEVEL", currentLevel)
        prefs.putInt("KEY_XP", currentXp)
        prefs.putInt("KEY_TOTAL_LIFETIME_XP", totalLifetimeXp)

        prefs.putInt("KEY_TOTAL_TASKS", totalTasksDone)
        prefs.putInt("KEY_STREAK", currentStreak)
        prefs.putBoolean("KEY_TASK_DONE_TODAY", taskDoneToday)
        prefs.putInt("KEY_LAST_DAY", lastLoginDay).putInt("KEY_LAST_WEEK", lastLoginWeek).putInt("KEY_LAST_YEAR", lastLoginYear)
        prefs.putString("KEY_NICKNAME", nickname).putString("KEY_DESC", description).putString("KEY_AVATAR", avatarUri)
        prefs.putBoolean("KEY_IS_FIRST_RUN", isFirstRun).putInt("KEY_COLOR_INT", appThemeColor).putString("KEY_LANG", appLanguage)
        prefs.putInt("KEY_REM_HOUR", reminderHour).putInt("KEY_REM_MIN", reminderMinute)
        prefs.apply()
    }

    fun loadGame(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        nickname = prefs.getString("KEY_NICKNAME", "Bezimienny") ?: "Bezimienny"
        description = prefs.getString("KEY_DESC", "...") ?: "..."
        avatarUri = prefs.getString("KEY_AVATAR", "") ?: ""

        currentLevel = prefs.getInt("KEY_LEVEL", 1)
        currentXp = prefs.getInt("KEY_XP", 0)
        totalLifetimeXp = prefs.getInt("KEY_TOTAL_LIFETIME_XP", 0)

        totalTasksDone = prefs.getInt("KEY_TOTAL_TASKS", 0)
        currentStreak = prefs.getInt("KEY_STREAK", 0)
        taskDoneToday = prefs.getBoolean("KEY_TASK_DONE_TODAY", false)
        lastLoginDay = prefs.getInt("KEY_LAST_DAY", -1); lastLoginWeek = prefs.getInt("KEY_LAST_WEEK", -1); lastLoginYear = prefs.getInt("KEY_LAST_YEAR", -1)

        appThemeColor = prefs.getInt("KEY_COLOR_INT", -10011977)
        if (appThemeColor == -11751600) appThemeColor = -10011977

        appLanguage = prefs.getString("KEY_LANG", "PL") ?: "PL"
        reminderHour = prefs.getInt("KEY_REM_HOUR", 20); reminderMinute = prefs.getInt("KEY_REM_MIN", 0)

        if (!prefs.contains("KEY_IS_FIRST_RUN")) isFirstRun = (currentXp == 0 && nickname == "Bezimienny")
        else isFirstRun = prefs.getBoolean("KEY_IS_FIRST_RUN", true)

        val qType = object : TypeToken<MutableList<Quest>>() {}.type
        val jType = object : TypeToken<MutableList<JournalEntry>>() {}.type
        prefs.getString("KEY_DAILY", null)?.let { dailyQuests = gson.fromJson(it, qType) }
        prefs.getString("KEY_WEEKLY", null)?.let { weeklyQuests = gson.fromJson(it, qType) }
        prefs.getString("KEY_YEARLY", null)?.let { yearlyQuests = gson.fromJson(it, qType) }
        prefs.getString("KEY_JOURNAL", null)?.let { journalEntries = gson.fromJson(it, jType) }

        recalculateTotalXp()
        checkAndResetQuests(context)
    }

    fun getExportJson(): String {
        val packedJournal = journalEntries.map { if (it.imageUri != null) it.copy(imageBase64 = compressAndEncodeImage(it.imageUri, 500)) else it }
        val avatarBase64 = if (avatarUri.isNotEmpty()) compressAndEncodeImage(avatarUri, 300) else null

        val state = FullGameState(
            nickname, description, avatarUri, avatarBase64, isFirstRun,
            currentLevel, currentXp,
            totalLifetimeXp,
            totalTasksDone, currentStreak, taskDoneToday, lastLoginDay, lastLoginWeek, lastLoginYear, dailyQuests, weeklyQuests, yearlyQuests, packedJournal
        )
        return gson.toJson(state)
    }

    fun importData(jsonData: String, context: Context): Boolean {
        return try {
            val json = if (!jsonData.trim().startsWith("{")) String(Base64.decode(jsonData, Base64.NO_WRAP)) else jsonData
            val state = gson.fromJson(json, FullGameState::class.java)
            nickname = state.nickname; description = state.description
            currentLevel = state.currentLevel; currentXp = state.currentXp

            totalLifetimeXp = if (state.totalLifetimeXp == 0 && state.currentLevel > 1) {
                var calc = state.currentXp
                for(i in 1 until state.currentLevel) calc += getNextLevelThreshold(i)
                calc
            } else { state.totalLifetimeXp }

            totalTasksDone = state.totalTasksDone; currentStreak = state.currentStreak; isFirstRun = false
            lastLoginDay = state.lastLoginDay; lastLoginWeek = state.lastLoginWeek; lastLoginYear = state.lastLoginYear
            dailyQuests = state.dailyQuests.toMutableList(); weeklyQuests = state.weeklyQuests.toMutableList(); yearlyQuests = state.yearlyQuests.toMutableList()
            journalEntries.clear()
            state.journalEntries.forEach { e -> var u = e.imageUri; if (e.imageBase64 != null) u = decodeAndSaveImage(e.imageBase64, "j_${System.nanoTime()}.jpg", context); journalEntries.add(e.copy(imageUri = u, imageBase64 = null)) }
            if (state.avatarBase64Data != null) avatarUri = decodeAndSaveImage(state.avatarBase64Data, "imp_av.jpg", context) ?: ""
            saveGame(context); true
        } catch (e: Exception) { false }
    }
    private fun compressAndEncodeImage(path: String, maxDim: Int): String? { try { val b = BitmapFactory.decodeFile(path) ?: return null; val s = Bitmap.createScaledBitmap(b, maxDim, maxDim, true); val os = ByteArrayOutputStream(); s.compress(Bitmap.CompressFormat.JPEG, 70, os); return Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP) } catch (e: Exception) { return null } }
    private fun decodeAndSaveImage(base64: String, name: String, ctx: Context): String? { try { val b = Base64.decode(base64, Base64.NO_WRAP); val f = File(ctx.filesDir, name); FileOutputStream(f).use { it.write(b) }; return f.absolutePath } catch (e: Exception) { return null } }
}