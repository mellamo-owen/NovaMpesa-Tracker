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
        
        if (data.isEmpty() || data.all { it == 0f }) {
            paint.color = Color.parseColor("#A0AABF")
            paint.textSize = 36f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("No spending this week", width / 2f, height / 2f, paint)
            return
        }

        val maxVal = data.maxOrNull() ?: 1f
        val barWidth = (width.toFloat() / data.size) * 0.5f
        val gap = (width.toFloat() / data.size) * 0.5f
        val bottomPadding = 60f
        val maxBarHeight = height - bottomPadding - 20f

        paint.textSize = 28f
        paint.textAlign = Paint.Align.CENTER

        for (i in data.indices) {
            val barHeight = if (data[i] == 0f) 4f else (data[i] / maxVal) * maxBarHeight
            val left = (i * (barWidth + gap)) + (gap / 2)
            val top = height - bottomPadding - barHeight
            val right = left + barWidth
            val bottom = height - bottomPadding

            // Kalo Style: Lime Green Bars (Dimmed if zero)
            paint.color = if (data[i] == 0f) Color.parseColor("#333333") else Color.parseColor("#A3E635")
            rectF.set(left, top, right, bottom)
            canvas.drawRoundRect(rectF, 12f, 12f, paint)

            if (i < labels.size) {
                paint.color = Color.parseColor("#A0AABF")
                canvas.drawText(labels[i], left + (barWidth / 2), height - 15f, paint)
            }
        }
    }
}
