package com.novanest.mpesatracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var data: List<Float> = emptyList()
    private var labels: List<String> = emptyList()

    fun setData(data: List<Float>, labels: List<String>) {
        this.data = data
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // If no data or all zeros, show text
        if (data.isEmpty() || data.all { it == 0f }) {
            paint.color = Color.WHITE
            paint.textSize = 40f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("No spending this week", width / 2f, height / 2f, paint)
            return
        }

        val maxVal = data.maxOrNull() ?: 1f
        val barWidth = (width.toFloat() / data.size) * 0.6f
        val gap = (width.toFloat() / data.size) * 0.4f
        val bottomPadding = 80f
        val maxBarHeight = height - bottomPadding - 20f

        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER

        for (i in data.indices) {
            // Even if the bar is 0, we draw a tiny line so the day is visible
            val barHeight = if (data[i] == 0f) 4f else (data[i] / maxVal) * maxBarHeight
            val left = (i * (barWidth + gap)) + (gap / 2)
            val top = height - bottomPadding - barHeight
            val right = left + barWidth
            val bottom = height - bottomPadding

            paint.color = Color.parseColor("#00E676") // Emerald Green
            rectF.set(left, top, right, bottom)
            canvas.drawRoundRect(rectF, 16f, 16f, paint)

            if (i < labels.size) {
                paint.color = Color.WHITE
                canvas.drawText(labels[i], left + (barWidth / 2), height - 30f, paint)
            }
        }
    }
}
