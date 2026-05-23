package com.example.walkmanapp.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class WaveformView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val amplitudes = mutableListOf<Float>()

    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 6f
    }

    private val barWidth = 10f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (amplitudes.size * barWidth).toInt()
        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    fun addAmplitude(amp: Float) {
        amplitudes.add(amp)
        requestLayout() // esto ahora es correcto
        invalidate()
    }

    fun setWaveform(data: List<Float>) {

        amplitudes.clear()

        amplitudes.addAll(data)

        requestLayout()

        invalidate()
    }

    fun clear() {
        amplitudes.clear()

        layoutParams = layoutParams.apply {
            width = 0
        }

        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerY = height / 2f

        amplitudes.forEachIndexed { i, amp ->
            val x = i * barWidth
            val lineHeight = amp * height

            canvas.drawLine(
                x,
                centerY - lineHeight / 2,
                x,
                centerY + lineHeight / 2,
                paint
            )
        }
    }
}