package com.example.myapplication

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddQuestDialogFragment : BottomSheetDialogFragment() {

    private var selectedColor: Int = Color.parseColor("#42A5F5")
    private var questType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questType = arguments?.getInt("type") ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.dialog_add_quest, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etTitle = view.findViewById<EditText>(R.id.et_quest_title)
        val rgDiff = view.findViewById<RadioGroup>(R.id.rg_difficulty)
        val rgTags = view.findViewById<RadioGroup>(R.id.rg_tags)
        val llCustom = view.findViewById<LinearLayout>(R.id.ll_custom_tag_options)
        val etCustomName = view.findViewById<EditText>(R.id.et_custom_tag_name)
        val square = view.findViewById<ColorPickerSquare>(R.id.tag_square_picker)
        val hue = view.findViewById<SeekBar>(R.id.tag_seek_hue)
        val preview = view.findViewById<View>(R.id.tag_view_preview)
        val etHex = view.findViewById<EditText>(R.id.tag_et_hex)
        val btnSave = view.findViewById<Button>(R.id.btn_save_quest)

        // === NOWOŚĆ: Ustawienie koloru przycisku z motywu ===
        btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(GameManager.appThemeColor)
        // ====================================================

        // HUE Gradient
        val colors = IntArray(361) { i -> Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)) }
        hue.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors).apply { cornerRadius = 50f }

        hue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { square.hue = p.toFloat() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        square.onColorChanged = { color ->
            selectedColor = color
            preview.setBackgroundColor(color)
            etHex.setText(String.format("#%06X", (0xFFFFFF and color)))
        }

        rgTags.setOnCheckedChangeListener { _, id ->
            llCustom.visibility = if (id == R.id.rb_tag_custom) View.VISIBLE else View.GONE
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            if (title.isEmpty()) {
                Toast.makeText(context, "Wpisz tytuł zadania!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // === ZABEZPIECZENIE PRZED CRASHEM ===
            val selectedDiffId = rgDiff.checkedRadioButtonId
            if (selectedDiffId == -1) {
                Toast.makeText(context, "Wybierz poziom trudności!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pobieramy tekst (Upewnij się, że w XML masz "Łatwe" / "Trudne" w nazwach przycisków!)
            val difficultyText = view.findViewById<RadioButton>(selectedDiffId).text.toString()

            var tName: String? = null
            var tColor: Int? = null

            when (rgTags.checkedRadioButtonId) {
                R.id.rb_tag_work -> { tName = "Work"; tColor = Color.parseColor("#42A5F5") }
                R.id.rb_tag_school -> { tName = "School"; tColor = Color.parseColor("#F5A623") }
                R.id.rb_tag_self -> { tName = "Self"; tColor = Color.parseColor("#4CAF50") }
                R.id.rb_tag_custom -> { tName = etCustomName.text.toString().ifEmpty { "Custom" }; tColor = selectedColor }
            }

            (parentFragment as? QuestsFragment)?.addQuestFromDialog(title, difficultyText, tName, tColor, questType)
            dismiss()
        }
    }

    companion object {
        const val TAG = "AddQuestDialog"
        fun newInstance(type: Int) = AddQuestDialogFragment().apply {
            arguments = Bundle().apply { putInt("type", type) }
        }
    }
}