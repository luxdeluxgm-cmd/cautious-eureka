package com.example.myapplication

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.EdgeEffect
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalFragment : Fragment() {

    private lateinit var adapter: JournalAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateView: LinearLayout

    private var tempImageUri: Uri? = null
    private var tempPreviewImageView: ImageView? = null
    private var tempPreviewContainer: CardView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            tempImageUri = uri
            tempPreviewContainer?.visibility = View.VISIBLE
            tempPreviewImageView?.setImageURI(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_journal)
        emptyStateView = view.findViewById(R.id.ll_empty_state)
        val btnAddNote: ImageButton = view.findViewById(R.id.btn_add_note)

        // Czyste, natywne kolorowanie ikony plusa
        ImageViewCompat.setImageTintList(btnAddNote, ColorStateList.valueOf(GameManager.appThemeColor))
        ImageViewCompat.setImageTintMode(btnAddNote, PorterDuff.Mode.SRC_IN)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // === MAGIA DETALI: Efekt poświaty przy przewijaniu (Overscroll) w Twoim kolorze! ===
        recyclerView.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return EdgeEffect(view.context).apply {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        color = GameManager.appThemeColor
                    }
                }
            }
        }
        // ===================================================================================

        adapter = JournalAdapter(GameManager.journalEntries) { clickedEntry ->
            showDetailDialog(clickedEntry)
        }
        recyclerView.adapter = adapter

        btnAddNote.setOnClickListener {
            showAddNoteDialog()
        }

        checkEmptyState()
    }

    override fun onResume() {
        super.onResume()
        val btnAdd = view?.findViewById<ImageButton>(R.id.btn_add_note)
        if (btnAdd != null) {
            ImageViewCompat.setImageTintList(btnAdd, ColorStateList.valueOf(GameManager.appThemeColor))
            ImageViewCompat.setImageTintMode(btnAdd, PorterDuff.Mode.SRC_IN)
        }

        adapter.notifyDataSetChanged()
        checkEmptyState()
    }

    private fun checkEmptyState() {
        if (GameManager.journalEntries.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    private fun showAddNoteDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_note, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_note_title)
        val etContent = dialogView.findViewById<EditText>(R.id.et_note_content)
        val btnAddPhoto = dialogView.findViewById<View>(R.id.btn_add_photo)
        tempPreviewImageView = dialogView.findViewById(R.id.iv_preview)
        tempPreviewContainer = dialogView.findViewById(R.id.cv_preview_container)
        val btnRemovePhoto = dialogView.findViewById<View>(R.id.btn_remove_photo)

        val btnSave = dialogView.findViewById<Button>(R.id.btn_save_note)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_note)

        btnSave.backgroundTintList = ColorStateList.valueOf(GameManager.appThemeColor)
        btnSave.setTextColor(Color.BLACK)

        tempImageUri = null
        btnAddPhoto.setOnClickListener { pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        btnRemovePhoto.setOnClickListener { tempImageUri = null; tempPreviewContainer?.visibility = View.GONE }

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isNotEmpty() || content.isNotEmpty()) {
                val now = Date()
                val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(now)
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

                var savedImagePath: String? = null
                if (tempImageUri != null) {
                    savedImagePath = saveJournalImage(tempImageUri!!)
                }

                val finalTitle = if (title.isEmpty()) "Notatka" else title

                val newEntry = JournalEntry(finalTitle, content, 0, currentDate, currentTime, savedImagePath)

                GameManager.journalEntries.add(0, newEntry)
                GameManager.saveGame(requireContext())

                adapter.notifyDataSetChanged()
                checkEmptyState()

                dialog.dismiss()
            } else {
                etContent.error = "Napisz coś!"
            }
        }
    }

    private fun showDetailDialog(entry: JournalEntry) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_journal_detail, null)

        val tvDate = dialogView.findViewById<TextView>(R.id.tv_detail_date)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_detail_title)
        val tvContent = dialogView.findViewById<TextView>(R.id.tv_detail_content)
        val cvImage = dialogView.findViewById<CardView>(R.id.cv_detail_image)
        val ivImage = dialogView.findViewById<ImageView>(R.id.iv_detail_image)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close_detail)
        val btnDelete = dialogView.findViewById<ImageButton>(R.id.btn_delete_entry)

        btnClose.backgroundTintList = ColorStateList.valueOf(GameManager.appThemeColor)
        btnClose.setTextColor(Color.BLACK)

        ImageViewCompat.setImageTintList(btnDelete, ColorStateList.valueOf(GameManager.appThemeColor))
        ImageViewCompat.setImageTintMode(btnDelete, PorterDuff.Mode.SRC_IN)

        tvDate.text = "${entry.date} ${entry.time}"
        tvTitle.text = entry.title
        tvContent.text = entry.content

        if (!entry.imageUri.isNullOrEmpty()) {
            val file = File(entry.imageUri)
            if (file.exists()) {
                cvImage.visibility = View.VISIBLE
                ivImage.load(file) { crossfade(true) }
            }
        }

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        btnDelete.setOnClickListener {
            // === NOWOŚĆ: Twoje autorskie, eleganckie okno usuwania wpisu! ===
            val confirmView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_delete, null)
            val confirmDialog = AlertDialog.Builder(context).setView(confirmView).create()

            val btnCancelDelete = confirmView.findViewById<Button>(R.id.btn_cancel_delete)
            val btnConfirmDelete = confirmView.findViewById<Button>(R.id.btn_confirm_delete)

            // Przycisk usunięcia pokolorowany na wybrany motyw dla zachowania spójności
            btnConfirmDelete.backgroundTintList = ColorStateList.valueOf(GameManager.appThemeColor)

            btnCancelDelete.setOnClickListener { confirmDialog.dismiss() }
            btnConfirmDelete.setOnClickListener {
                if (entry.imageUri != null) {
                    val file = File(entry.imageUri)
                    if (file.exists()) file.delete()
                }
                GameManager.journalEntries.remove(entry)
                GameManager.saveGame(requireContext())

                adapter.notifyDataSetChanged()
                checkEmptyState()

                confirmDialog.dismiss()
                dialog.dismiss()
                Toast.makeText(context, "Wpis usunięty.", Toast.LENGTH_SHORT).show()
            }

            confirmDialog.show()
            confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            // =================================================================
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun saveJournalImage(uri: Uri): String? {
        return try {
            val context = requireContext()
            val fileName = "journal_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
            file.absolutePath
        } catch (e: Exception) { null }
    }
}