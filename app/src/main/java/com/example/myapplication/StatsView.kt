package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class StatsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint().apply { color = GameManager.appThemeColor; style = Paint.Style.FILL; isAntiAlias = true }
    private val textPaint = Paint().apply { color = Color.DKGRAY; textSize = 30f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 2f; isAntiAlias = true }

    private var dataPoints: List<Pair<String, Int>> = emptyList()
    private var maxVal = 1

    fun setData(data: List<Pair<String, Int>>) {
        dataPoints = data
        maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
        barPaint.color = GameManager.appThemeColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 64f
        val graphHeight = h - (2 * padding)

        // Obliczamy szerokość jednego słupka dynamicznie
        // Im więcej danych, tym węższe słupki i mniejsze odstępy
        val totalBarSpace = w - (2 * padding)
        val singleSlotWidth = totalBarSpace / dataPoints.size
        val barWidth = singleSlotWidth * 0.6f // Słupek zajmuje 60% dostępnego miejsca

        // Rysujemy linię bazową
        canvas.drawLine(padding, h - padding, w - padding, h - padding, linePaint)

        // === LOGIKA "CO KTÓRY NAPIS RYSOWAĆ" ===
        // Jeśli mamy > 10 słupków (np. miesiąc), rysuj co 5-ty napis.
        // Jeśli > 7 (tydzień), rysuj wszystkie.
        val step = when {
            dataPoints.size > 20 -> 5  // Dla 30 dni: co 5 dzień
            dataPoints.size > 10 -> 2  // Dla średnich zakresów
            else -> 1                  // Dla tygodnia: każdy dzień
        }

        dataPoints.forEachIndexed { index, pair ->
            val count = pair.second
            val label = pair.first

            val barHeight = (count.toFloat() / maxVal.toFloat()) * graphHeight

            // Wyliczamy środek slotu
            val centerX = padding + (index * singleSlotWidth) + (singleSlotWidth / 2)

            val left = centerX - (barWidth / 2)
            val right = centerX + (barWidth / 2)
            val top = (h - padding) - barHeight
            val bottom = h - padding

            // 1. Rysuj słupek
            canvas.drawRect(left, top, right, bottom, barPaint)

            // 2. Rysuj liczbę NAD słupkiem (tylko jak > 0)
            // Jeśli jest gęsto (30 dni), rysuj liczbę tylko jak jest duża, albo wcale, żeby nie robić syfu.
            // Tutaj: Rysujemy zawsze, ale mniejszą czcionką jak gęsto.
            if (count > 0) {
                if (dataPoints.size > 20) textPaint.textSize = 20f else textPaint.textSize = 30f
                canvas.drawText(count.toString(), centerX, top - 10f, textPaint)
            }

            // 3. Rysuj datę POD słupkiem (Z UŻYCIEM KROKU 'STEP')
            if (index % step == 0) {
                textPaint.textSize = 30f // Przywróć rozmiar dla daty
                canvas.drawText(label, centerX, h - padding + 40f, textPaint)
            }
        }
    }
}