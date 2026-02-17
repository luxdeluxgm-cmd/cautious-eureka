package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ColorPickerSquare @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var hue: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var onColorChanged: ((Int) -> Unit)? = null
    private var sat = 0f
    private var val_ = 1f

    private val paint = Paint()
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // Gradient Saturation (Biały -> Hue)
        val shaderSat = LinearGradient(0f, 0f, width, 0f, Color.WHITE, Color.HSVToColor(floatArrayOf(hue, 1f, 1f)), Shader.TileMode.CLAMP)
        // Gradient Value (Przezroczysty -> Czarny)
        val shaderVal = LinearGradient(0f, 0f, 0f, height, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)

        paint.shader = ComposeShader(shaderSat, shaderVal, PorterDuff.Mode.DARKEN)
        canvas.drawRect(0f, 0f, width, height, paint)

        // Rysuj kółko wyboru
        canvas.drawCircle(sat * width, (1f - val_) * height, 15f, selectorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        sat = (event.x / width).coerceIn(0f, 1f)
        val_ = (1f - (event.y / height)).coerceIn(0f, 1f)
        onColorChanged?.invoke(Color.HSVToColor(floatArrayOf(hue, sat, val_)))
        invalidate()
        return true
    }

    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hue = hsv[0]
        sat = hsv[1]
        val_ = hsv[2]
        invalidate()
    }
}