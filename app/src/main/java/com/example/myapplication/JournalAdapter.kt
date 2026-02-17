package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.concurrent.Executors

class JournalAdapter(
    private var entries: MutableList<JournalEntry>,
    private val onClick: (JournalEntry) -> Unit
) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    class JournalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_journal_title)
        val tvContent: TextView = view.findViewById(R.id.tv_journal_content)
        val cvImage: CardView = view.findViewById(R.id.cv_journal_image)
        val ivImage: ImageView = view.findViewById(R.id.iv_journal_image)

        // === NOWE POLA (ZGODNIE Z TWOIM ŻYCZENIEM) ===
        val tvTime: TextView = view.findViewById(R.id.tv_journal_time)        // Godzina
        val tvDateOnly: TextView = view.findViewById(R.id.tv_journal_date_only) // Data
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_journal_entry, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val entry = entries[position]
        // ... (Twój kod ustawiający tytuł i treść) ...

        // === DODAJ TE DWIE LINIJKI ===
        // 1. Kolor godziny
        holder.itemView.findViewById<TextView>(R.id.tv_journal_time).setTextColor(GameManager.appThemeColor)

        // 2. Kolor ikonki ołówka
        holder.itemView.findViewById<ImageView>(R.id.iv_journal_icon).imageTintList = android.content.res.ColorStateList.valueOf(GameManager.appThemeColor)
        holder.tvTitle.text = entry.title
        holder.tvContent.text = entry.content

        // === PRZYPISANIE DATY I GODZINY DO OSOBNYCH PÓL ===
        // Jeśli godzina jest pusta (stare wpisy), nic nie wyświetlamy w polu godziny
        if (entry.time.isNotEmpty()) {
            holder.tvTime.text = entry.time
            holder.tvTime.visibility = View.VISIBLE
        } else {
            holder.tvTime.visibility = View.GONE
        }

        // Data zawsze trafia do pola daty
        holder.tvDateOnly.text = entry.date
        // ==================================================

        holder.itemView.setOnClickListener { onClick(entry) }

        if (!entry.imageUri.isNullOrEmpty()) {
            val file = File(entry.imageUri)
            if (file.exists()) {
                holder.cvImage.visibility = View.VISIBLE
                holder.ivImage.tag = entry.imageUri
                holder.ivImage.setImageDrawable(null)

                val cachedBitmap = GameManager.memoryCache.get(entry.imageUri)
                if (cachedBitmap != null) {
                    holder.ivImage.setImageBitmap(cachedBitmap)
                } else {
                    loadThumbnailAsync(holder.ivImage, entry.imageUri!!, 200)
                }
            } else {
                holder.cvImage.visibility = View.GONE
            }
        } else {
            holder.cvImage.visibility = View.GONE
        }
    }

    override fun getItemCount() = entries.size

    fun updateList(newEntries: List<JournalEntry>) {
        entries = newEntries.toMutableList()
        notifyDataSetChanged()
    }

    private fun loadThumbnailAsync(imageView: ImageView, path: String, reqSize: Int) {
        executor.execute {
            try {
                val currentTag = imageView.tag
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(path, options)
                options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
                options.inJustDecodeBounds = false

                var bitmap = BitmapFactory.decodeFile(path, options)
                if (bitmap != null) {
                    bitmap = rotateBitmapIfNeeded(bitmap, path)
                    GameManager.memoryCache.put(path, bitmap)
                    mainHandler.post {
                        if (imageView.tag == currentTag) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
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