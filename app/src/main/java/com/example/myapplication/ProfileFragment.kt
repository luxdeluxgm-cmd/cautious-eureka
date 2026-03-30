package com.example.myapplication

import android.content.Intent
import android.os.Bundle
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
import androidx.fragment.app.Fragment
import coil.load
import java.io.File

class ProfileFragment : Fragment() {

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

        val themeColor = GameManager.appThemeColor
        val colorState = android.content.res.ColorStateList.valueOf(themeColor)

        view.findViewById<TextView>(R.id.tv_level_badge).setTextColor(themeColor)
        view.findViewById<TextView>(R.id.tv_xp_value).setTextColor(themeColor)
        view.findViewById<ProgressBar>(R.id.progressBar_xp).progressTintList = colorState
        view.findViewById<LinearLayout>(R.id.ll_stats_container).backgroundTintList = colorState
        view.findViewById<Button>(R.id.btn_open_stats).backgroundTintList = colorState

        val ivAvatar = view.findViewById<ImageView>(R.id.iv_avatar)
        ivAvatar.setImageDrawable(null)

        // Użycie nowej biblioteki Coil!
        if (GameManager.avatarUri.isNotEmpty()) {
            val file = File(GameManager.avatarUri)
            if (file.exists()) {
                ivAvatar.load(file) {
                    crossfade(true)
                }
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

                // Użycie nowej biblioteki Coil!
                imgView.load(File(path)) {
                    crossfade(true)
                }

                card.setOnClickListener {
                    DialogHelper.showImagePreview(requireContext(), path, entry.date)
                }
            } else {
                imgView.setImageDrawable(null)
                card.setOnClickListener(null)
            }
        }
    }
}