package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.concurrent.Executors

object DialogHelper {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showImagePreview(context: Context, imagePath: String, title: String? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview_unified, null)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.iv_unified_preview)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_unified_title)
        val btnClose = dialogView.findViewById<View>(R.id.btn_unified_close)

        // Ustawiamy tytuł lub ukrywamy
        if (title != null) {
            tvTitle.text = title
            tvTitle.visibility = View.VISIBLE
        } else {
            tvTitle.visibility = View.GONE
        }

        // Ładujemy zdjęcie w wysokiej jakości (ale asynchronicznie!)
        loadImageHighRes(ivPreview, imagePath)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // Przezroczyste tło dialogu (żeby było widać zaokrąglenia layoutu)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun loadImageHighRes(imageView: ImageView, path: String) {
        executor.execute {
            try {
                // Tu ładujemy większe (np. 1080p), bo to fullscreen
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(path, options)

                // Sample 1 = Full size, Sample 2 = Half size.
                // Dla bezpieczeństwa dajemy 2 (redukcja o połowę), chyba że to małe zdjęcie
                options.inSampleSize = if (options.outWidth > 2000) 2 else 1
                options.inJustDecodeBounds = false

                var bitmap = BitmapFactory.decodeFile(path, options)
                if (bitmap != null) {
                    bitmap = rotateBitmapIfNeeded(bitmap, path)
                    mainHandler.post { imageView.setImageBitmap(bitmap) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, path: String): Bitmap {
        // (Ta sama logika obrotu co wszędzie - EXIF)
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
        } catch (e: Exception) { }
        return rotatedBitmap
    }
}