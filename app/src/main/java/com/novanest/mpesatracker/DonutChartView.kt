package com.novanest.mpesatracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class DonutChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var data: List<Float> = emptyList()
    private var colors: List<Int> = emptyList()

    fun setData(data: List<Float>, colors: List<Int>) {
        this.data = data
        this.colors = colors
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty() || data.sum() == 0f) {
            // Draw empty gray circle
            paint.color = Color.parseColor("#E0E0E0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 60f
            rectF.set(60f, 60f, width - 60f, height - 60f)
            canvas.drawArc(rectF, 0f, 360f, false, paint)
            return
        }

        val total = data.sum()
        var startAngle = -90f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 60f
        rectF.set(60f, 60f, width - 60f, height - 60f)

        for (i in data.indices) {
            paint.color = colors.getOrElse(i) { Color.GRAY }
            val sweepAngle = (data[i] / total) * 360f
            canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
            startAngle += sweepAngle
        }
    }
}
