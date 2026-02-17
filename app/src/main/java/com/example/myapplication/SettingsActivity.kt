package com.example.myapplication

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

class SettingsActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var etNick: EditText
    private lateinit var etDesc: EditText
    // private lateinit var toggleLang: ToggleButton <--- USUNIĘTE
    private lateinit var viewCurrentColor: View
    private lateinit var btnReminderTime: Button

    private var tempAvatarUri: Uri? = null
    // Zmieniamy na 0, przypiszemy wartość w loadCurrentData()
    private var tempSelectedColor: Int = 0

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val exportFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) saveExportToFile(uri)
    }

    private val importFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) loadImportFromFile(uri)
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { tempAvatarUri = uri; ivAvatar.setImageURI(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        ivAvatar = findViewById(R.id.iv_settings_avatar)
        etNick = findViewById(R.id.et_settings_nick)
        etDesc = findViewById(R.id.et_settings_desc)
        // toggleLang = findViewById(R.id.toggle_language) <--- USUNIĘTE
        viewCurrentColor = findViewById(R.id.view_current_color)
        btnReminderTime = findViewById(R.id.btn_reminder_time)

        // To ładuje dane, ustawia tempSelectedColor ORAZ KOLORYUJE GUZIKI
        loadCurrentData()

        // Wybór koloru
        findViewById<LinearLayout>(R.id.btn_change_color).setOnClickListener { showCustomRgbPicker() }

        // Wybór godziny powiadomienia
        btnReminderTime.setOnClickListener { showTimePicker() }

        // Wybór avatara
        ivAvatar.setOnClickListener { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

        // Przyciski akcji
        findViewById<Button>(R.id.btn_save_settings).setOnClickListener { saveProfileData() }
        findViewById<ImageButton>(R.id.btn_back_settings).setOnClickListener { finish() }

        // Eksport / Import
        findViewById<Button>(R.id.btn_export).apply {
            text = "Eksportuj do pliku .json"
            setOnClickListener { exportFileLauncher.launch("rpg_save_${System.currentTimeMillis()}.json") }
        }

        findViewById<Button>(R.id.btn_import).apply {
            text = "Wczytaj z pliku .json"
            setOnClickListener { importFileLauncher.launch(arrayOf("application/json")) }
        }
    }

    // === OBSŁUGA ALARMU ===
    private fun rescheduleAlarm(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this, 1001, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        try {
            alarmManager.cancel(pendingIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    return
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
            Toast.makeText(this, "Alarm ustawiony na ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showTimePicker() {
        val dialog = TimePickerDialog(this, { _, hour, minute ->
            GameManager.reminderHour = hour
            GameManager.reminderMinute = minute
            btnReminderTime.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            GameManager.saveGame(this)
            rescheduleAlarm(hour, minute)
        }, GameManager.reminderHour, GameManager.reminderMinute, true)
        dialog.show()
    }

    // === COLOR PICKER ===
    private fun showCustomRgbPicker() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rgb_picker, null)
        val square = dialogView.findViewById<ColorPickerSquare>(R.id.square_picker)
        val hue = dialogView.findViewById<SeekBar>(R.id.seek_hue)
        val preview = dialogView.findViewById<View>(R.id.view_preview)
        val etHex = dialogView.findViewById<EditText>(R.id.et_hex_code)

        // Ważne: Startujemy z aktualnie wybranego (tymczasowego) koloru
        var currentDialogColor = tempSelectedColor

        preview.setBackgroundColor(currentDialogColor)
        etHex.setText(String.format("#%06X", (0xFFFFFF and currentDialogColor)))
        square.setColor(currentDialogColor) // Ustawiamy kropkę na start

        val hueColors = IntArray(361) { i -> Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)) }
        hue.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, hueColors).apply { cornerRadius = 50f }

        hue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { square.hue = p.toFloat() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        square.onColorChanged = { color ->
            currentDialogColor = color
            preview.setBackgroundColor(color)
            val hexStr = String.format("#%06X", (0xFFFFFF and color))
            if (etHex.text.toString() != hexStr) etHex.setText(hexStr)
        }

        etHex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                try {
                    val color = Color.parseColor(s.toString())
                    currentDialogColor = color
                    preview.setBackgroundColor(color)
                    square.setColor(color)
                } catch (e: Exception) { }
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Wybierz") { _, _ ->
                // 1. Aktualizujemy widok w ustawieniach
                tempSelectedColor = currentDialogColor
                viewCurrentColor.setBackgroundColor(tempSelectedColor)

                // 2. Zapisujemy od razu w GameManagerze i pamięci telefonu
                GameManager.appThemeColor = tempSelectedColor
                GameManager.saveGame(this)

                Toast.makeText(this, "Kolor został zmieniony!", Toast.LENGTH_SHORT).show()

                // 3. Odświeżamy kolory przycisków od razu (żeby użytkownik widział zmianę)
                val newColorState = ColorStateList.valueOf(tempSelectedColor)
                findViewById<Button>(R.id.btn_save_settings).backgroundTintList = newColorState
                findViewById<Button>(R.id.btn_export).backgroundTintList = newColorState
                findViewById<Button>(R.id.btn_import).backgroundTintList = newColorState
                btnReminderTime.setTextColor(tempSelectedColor)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    // === PLIKI I DANE ===
    private fun saveExportToFile(uri: Uri) {
        executor.execute {
            try {
                val json = GameManager.getExportJson()
                contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                mainHandler.post { Toast.makeText(this, "Zapisano plik!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                mainHandler.post { Toast.makeText(this, "Błąd zapisu!", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun loadImportFromFile(uri: Uri) {
        executor.execute {
            try {
                val sb = StringBuilder()
                contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) { sb.append(line); line = reader.readLine() }
                    }
                }
                val success = GameManager.importData(sb.toString(), this)
                mainHandler.post {
                    if (success) {
                        Toast.makeText(this, "Wczytano! Restart...", Toast.LENGTH_LONG).show()
                        finish()
                    } else Toast.makeText(this, "Plik uszkodzony!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                mainHandler.post { Toast.makeText(this, "Błąd odczytu!", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun loadCurrentData() {
        etNick.setText(GameManager.nickname)
        etDesc.setText(GameManager.description)
        // toggleLang.isChecked = ... <--- USUNIĘTE

        // Pobieramy kolor z GameManager i ustawiamy go jako tymczasowy
        tempSelectedColor = GameManager.appThemeColor
        viewCurrentColor.setBackgroundColor(tempSelectedColor)

        btnReminderTime.text = String.format(Locale.getDefault(), "%02d:%02d", GameManager.reminderHour, GameManager.reminderMinute)

        if (GameManager.avatarUri.isNotEmpty()) {
            val file = File(GameManager.avatarUri)
            if (file.exists()) ivAvatar.setImageURI(Uri.fromFile(file))
        }

        // === KOLOROWANIE PRZYCISKÓW ===
        val colorState = ColorStateList.valueOf(GameManager.appThemeColor)
        try {
            findViewById<Button>(R.id.btn_save_settings).backgroundTintList = colorState
            findViewById<Button>(R.id.btn_export).backgroundTintList = colorState
            findViewById<Button>(R.id.btn_import).backgroundTintList = colorState
            btnReminderTime.setTextColor(GameManager.appThemeColor)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveProfileData() {
        GameManager.nickname = etNick.text.toString().trim()
        GameManager.description = etDesc.text.toString().trim()
        // GameManager.appLanguage = ... <--- USUNIĘTE

        // Zapisujemy wybrany kolor
        GameManager.appThemeColor = tempSelectedColor

        if (tempAvatarUri != null) {
            executor.execute {
                val file = File(filesDir, "my_rpg_avatar.jpg")
                contentResolver.openInputStream(tempAvatarUri!!)?.use { input -> FileOutputStream(file).use { it -> input.copyTo(it) } }
                GameManager.clearCacheFor(GameManager.avatarUri)
                GameManager.avatarUri = file.absolutePath
                mainHandler.post {
                    GameManager.saveGame(this)
                    Toast.makeText(this, "Zapisano ustawienia!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            GameManager.saveGame(this)
            Toast.makeText(this, "Zapisano ustawienia!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down)
    }
}