package com.example.myapplication

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class JournalFragment : Fragment() {

    private lateinit var adapter: JournalAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateView: LinearLayout

    private var tempImageUri: Uri? = null
    private var tempPreviewImageView: ImageView? = null
    private var tempPreviewContainer: CardView? = null

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

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
        val btnAdd: ImageButton = view.findViewById(R.id.btn_add_note)
        // 1. Znajdź guzik (jeśli jeszcze tego nie masz)
        val btnAddNote: ImageButton = view.findViewById(R.id.btn_add_note)

// === DODAJ TĘ LINIJKĘ, ABY POKOLOROWAĆ PLUSIK ===
// Ustawia kolor ikonki (źródła obrazka) na wybrany akcent z GameManager
        btnAddNote.imageTintList = android.content.res.ColorStateList.valueOf(GameManager.appThemeColor)
// ===============================================

// (Dalej pewnie masz btnAddNote.setOnClickListener { ... })

        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = JournalAdapter(GameManager.journalEntries) { clickedEntry ->
            showDetailDialog(clickedEntry)
        }
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            showAddNoteDialog()
        }

        checkEmptyState()
    }

    override fun onResume() {
        super.onResume()
        val btnAdd = view?.findViewById<android.widget.ImageButton>(R.id.btn_add_note)

        // Ustawiamy kolor ikony (plusika) na wybrany motyw
        btnAdd?.imageTintList = android.content.res.ColorStateList.valueOf(GameManager.appThemeColor)
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

        tempImageUri = null
        btnAddPhoto.setOnClickListener { pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        btnRemovePhoto.setOnClickListener { tempImageUri = null; tempPreviewContainer?.visibility = View.GONE }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Opublikuj", null)
            .setNegativeButton("Anuluj", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isNotEmpty() || content.isNotEmpty()) {
                // === TU JEST POPRAWKA ===
                // Pobieramy czas DOKŁADNIE TERAZ, w momencie kliknięcia
                val now = Date()
                val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(now)
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)

                var savedImagePath: String? = null
                if (tempImageUri != null) {
                    savedImagePath = saveJournalImage(tempImageUri!!)
                }

                val finalTitle = if (title.isEmpty()) "Notatka" else title

                // Tworzymy wpis
                val newEntry = JournalEntry(
                    finalTitle,
                    content,
                    0,
                    currentDate,
                    currentTime,
                    savedImagePath
                )

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

    // === TO JEST FUNKCJA, KTÓRĄ ZMIENILIŚMY ===
    private fun showDetailDialog(entry: JournalEntry) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_journal_detail, null)

        val tvDate = dialogView.findViewById<TextView>(R.id.tv_detail_date)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_detail_title)
        val tvContent = dialogView.findViewById<TextView>(R.id.tv_detail_content)
        val cvImage = dialogView.findViewById<CardView>(R.id.cv_detail_image)
        val ivImage = dialogView.findViewById<ImageView>(R.id.iv_detail_image)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close_detail) // Zmienione na View (bezpieczniej)
        val btnDelete = dialogView.findViewById<ImageButton>(R.id.btn_delete_entry)

        tvDate.text = entry.date
        tvTitle.text = entry.title
        tvContent.text = entry.content

        // Ładowanie zdjęcia
        if (!entry.imageUri.isNullOrEmpty()) {
            val file = File(entry.imageUri)
            if (file.exists()) {
                cvImage.visibility = View.VISIBLE

                // Ładujemy w wysokiej jakości
                loadImageAsync(ivImage, entry.imageUri!!, 1024)

                // USUNĄŁEM TU setOnClickListener
                // Zdjęcie się wyświetla, ale nie da się go kliknąć, żeby powiększyć
            }
        }

        val dialog = AlertDialog.Builder(context).setView(dialogView).create()

        // Obsługa usuwania
        btnDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Usuń wpis")
                .setMessage("Na pewno? Tego nie da się cofnąć.")
                .setPositiveButton("Tak, usuń") { _, _ ->
                    if (entry.imageUri != null) {
                        val file = File(entry.imageUri)
                        if (file.exists()) file.delete()
                    }
                    GameManager.journalEntries.remove(entry)
                    GameManager.saveGame(requireContext())

                    adapter.notifyDataSetChanged()
                    checkEmptyState()

                    dialog.dismiss()
                    Toast.makeText(context, "Wpis usunięty.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
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

    private fun loadImageAsync(imageView: ImageView, path: String, reqSize: Int) {
        executor.execute {
            try {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(path, options)
                options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
                options.inJustDecodeBounds = false
                var bitmap = BitmapFactory.decodeFile(path, options)
                if (bitmap != null) {
                    bitmap = rotateBitmapIfNeeded(bitmap, path)
                    mainHandler.post { imageView.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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