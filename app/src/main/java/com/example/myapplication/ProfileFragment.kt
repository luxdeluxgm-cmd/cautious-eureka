package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import java.io.File
import java.util.concurrent.Executors

class ProfileFragment : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation)
        }

        val btnStats = view.findViewById<Button>(R.id.btn_open_stats)
        btnStats.setOnClickListener {
            val intent = Intent(requireContext(), StatisticsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProfileData()
    }

    private fun refreshProfileData() {
        val view = view ?: return

        // === NAPRAWA: DYNAMICZNY PRÓG XP ===
        // Zamiast sztywnego 750, pobieramy próg dla AKTUALNEGO poziomu
        val xpThreshold = GameManager.getNextLevelThreshold(GameManager.currentLevel)

        view.findViewById<TextView>(R.id.tv_nickname).text = if (GameManager.nickname.isNotEmpty()) GameManager.nickname else "Bezimienny"
        view.findViewById<TextView>(R.id.tv_about_description).text = if (GameManager.description.isNotEmpty()) GameManager.description else "Brak opisu."

        view.findViewById<TextView>(R.id.tv_level_badge).text = "LVL ${GameManager.currentLevel}"
        view.findViewById<TextView>(R.id.tv_xp_value).text = "${GameManager.currentXp} / $xpThreshold XP"

        val pbXp = view.findViewById<ProgressBar>(R.id.progressBar_xp)
        pbXp.max = xpThreshold
        pbXp.progress = GameManager.currentXp

        view.findViewById<TextView>(R.id.tv_stat_quests).text = "${GameManager.totalTasksDone}"
        view.findViewById<TextView>(R.id.tv_stat_streak).text = "${GameManager.currentStreak}"

        // ... (tutaj jest Twój kod ustawiający teksty: tv_stat_quests, tv_stat_streak itp.) ...

        // === DODAJ TEN BLOK KODU PONIŻEJ ===
        val themeColor = GameManager.appThemeColor
        val colorState = android.content.res.ColorStateList.valueOf(themeColor)

        // 1. Kolor tekstu LVL
        view.findViewById<TextView>(R.id.tv_level_badge).setTextColor(themeColor)

        // 2. Kolor tekstu XP
        view.findViewById<TextView>(R.id.tv_xp_value).setTextColor(themeColor)

        // 3. Kolor paska postępu
        view.findViewById<ProgressBar>(R.id.progressBar_xp).progressTintList = colorState

        // 4. Kolor tła statystyk (Outline/Wypełnienie - zależy jak zrobiony jest drawable)
        view.findViewById<LinearLayout>(R.id.ll_stats_container).backgroundTintList = colorState

        // 5. Kolor tła przycisku
        view.findViewById<Button>(R.id.btn_open_stats).backgroundTintList = colorState
        // ====================================

        // ... (dalej jest Twój stary kod od Avatara) ...

        val ivAvatar = view.findViewById<ImageView>(R.id.iv_avatar)
        ivAvatar.setImageDrawable(null) // Czyścimy stare

        if (GameManager.avatarUri.isNotEmpty()) {
            val file = File(GameManager.avatarUri)
            if (file.exists()) {
                loadAvatarAsync(ivAvatar, file.absolutePath)
                ivAvatar.setOnClickListener {
                    DialogHelper.showImagePreview(requireContext(), GameManager.avatarUri, "Mój Awatar")
                }
            } else {
                ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } else {
            ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        setupTopGallery(view)
    }

    // --- Reszta kodu bez zmian (ładowanie obrazków) ---

    private fun loadAvatarAsync(imageView: ImageView, path: String) {
        executor.execute {
            try {
                val cached = GameManager.memoryCache.get(path)
                if (cached != null) {
                    mainHandler.post { imageView.setImageBitmap(cached) }
                    return@execute
                }
                val options = BitmapFactory.Options()
                options.inSampleSize = 2
                var bitmap = BitmapFactory.decodeFile(path, options)
                if (bitmap != null) {
                    bitmap = rotateBitmapIfNeeded(bitmap, path)
                    GameManager.memoryCache.put(path, bitmap)
                    mainHandler.post { imageView.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupTopGallery(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.ll_journal_preview)
        val imageEntries = GameManager.journalEntries
            .filter { !it.imageUri.isNullOrEmpty() && File(it.imageUri!!).exists() }
            .take(4)

        for (i in 0 until container.childCount) {
            val card = container.getChildAt(i) as CardView
            val imgView = card.getChildAt(0) as ImageView

            if (i < imageEntries.size) {
                val entry = imageEntries[i]
                val path = entry.imageUri!!
                loadThumbnail(imgView, path)
                card.setOnClickListener {
                    DialogHelper.showImagePreview(requireContext(), path, entry.date)
                }
            } else {
                imgView.setImageDrawable(null)
                card.setOnClickListener(null)
            }
        }
    }

    private fun loadThumbnail(imageView: ImageView, path: String) {
        val cached = GameManager.memoryCache.get(path)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }
        executor.execute {
            try {
                val options = BitmapFactory.Options()
                options.inSampleSize = 4
                var bitmap = BitmapFactory.decodeFile(path, options)
                if (bitmap != null) {
                    bitmap = rotateBitmapIfNeeded(bitmap, path)
                    GameManager.memoryCache.put(path, bitmap)
                    mainHandler.post { imageView.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, path: String): Bitmap {
        var rotatedBitmap = bitmap
        try {
            val ei = ExifInterface(path)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationInDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            if (rotationInDegrees != 0) {
                val matrix = Matrix()
                matrix.preRotate(rotationInDegrees.toFloat())
                rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotatedBitmap != bitmap) bitmap.recycle()
            }
        } catch (e: Exception) { e.printStackTrace() }
        return rotatedBitmap
    }
}