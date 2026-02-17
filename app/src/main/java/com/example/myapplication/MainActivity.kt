package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.forEach
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private var tempAvatarUri: Uri? = null
    private lateinit var ivDialogAvatar: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { tempAvatarUri = uri; ivDialogAvatar.setImageURI(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Ładujemy stan gry
        GameManager.loadGame(this)

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.offscreenPageLimit = 2

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_journal -> viewPager.currentItem = 0
                R.id.navigation_quests -> viewPager.currentItem = 1
                R.id.navigation_profile -> viewPager.currentItem = 2
            }
            true
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pos: Int) {
                navView.menu.getItem(pos).isChecked = true
            }
        })

        // === USUNIĘCIE DYMKÓW (TOASTÓW) W DOLNYM MENU ===
        navView.menu.forEach { menuItem ->
            val view = navView.findViewById<View>(menuItem.itemId)
            view.setOnLongClickListener { true }
        }

        // 2. Powiadomienia (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // 3. Budzik
        scheduleDailyReminder()

        // 4. Pierwsze uruchomienie - Profil
        if (GameManager.isFirstRun) {
            showSetupProfileDialog()
        }
    }

    // === NOWOŚĆ: Odświeżanie motywu po powrocie do aktywności ===
    override fun onResume() {
        super.onResume()
        applyUserTheme()
    }

    // === NOWOŚĆ: Funkcja malująca interfejs ===
    private fun applyUserTheme() {
        val themeColor = GameManager.appThemeColor

        // 1. Kolor paska statusu (góra ekranu)
        window.statusBarColor = Color.BLACK

        // Ikony na pasku (zegar, bateria) -> ZAWSZE BIAŁE (bo tło jest czarne)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        // 2. Tint BottomNavigationView (Ikony na dole)
        val navView = findViewById<BottomNavigationView>(R.id.nav_view)

        // Stany: Wybrany (Twój kolor), Niewybrany (Szary)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )

        val colors = intArrayOf(
            themeColor,
            Color.GRAY
        )

        val colorStateList = ColorStateList(states, colors)

        navView.itemIconTintList = colorStateList
        navView.itemTextColor = colorStateList
    }

    private fun scheduleDailyReminder() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, ReminderReceiver::class.java)

            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, GameManager.reminderHour)
                set(Calendar.MINUTE, GameManager.reminderMinute)
                set(Calendar.SECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showSetupProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_setup_profile, null)

        ivDialogAvatar = dialogView.findViewById(R.id.iv_setup_avatar)
        val etName = dialogView.findViewById<EditText>(R.id.et_setup_nickname)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_setup_desc)
        val btnSave = dialogView.findViewById<android.widget.Button>(R.id.btn_setup_save)

        ivDialogAvatar.setOnClickListener { pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) {
                GameManager.nickname = name
                GameManager.description = etDesc.text.toString().trim()

                if (tempAvatarUri != null) {
                    try {
                        val file = File(filesDir, "my_rpg_avatar.jpg")
                        contentResolver.openInputStream(tempAvatarUri!!)?.use { input ->
                            FileOutputStream(file).use { output -> input.copyTo(output) }
                        }
                        GameManager.avatarUri = file.absolutePath
                    } catch (e: Exception) { e.printStackTrace() }
                }
                GameManager.isFirstRun = false
                GameManager.saveGame(this)
                dialog.dismiss()
                recreate()
            } else {
                etName.error = "Wpisz imię!"
            }
        }

        dialog.show()
    }
}