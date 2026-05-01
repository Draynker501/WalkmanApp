package com.example.walkmanapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CassetteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintReel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintCenter = Paint(Paint.ANTI_ALIAS_FLAG)

    private var progress = 0f

    private var angleLeft = 0f
    private var angleRight = 0f

    fun updateRotation() {
        val speedLeft = 2f + (progress * 6f)
        val speedRight = 8f - (progress * 6f)

        angleLeft += speedLeft
        angleRight -= speedRight

        invalidate()
    }

    fun setProgress(p: Float) {
        progress = p.coerceIn(0f, 1f)
        invalidate()
    }

    init {
        paintReel.style = Paint.Style.STROKE
        paintReel.color = Color.BLACK

        paintCenter.style = Paint.Style.FILL
        paintCenter.color = Color.DKGRAY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cxLeft = width * 0.25f
        val cxRight = width * 0.75f
        val cy = height * 0.5f

        val baseRadius = 60f

        val minThickness = 10f
        val maxThickness = 40f

        val windowWidth = width * 0.30f
        val windowHeight = height * 0.26f

        val rectLeft = (width / 2) - (windowWidth / 2)
        val rectTop = cy - (windowHeight / 2)

        val paintWindow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }

        canvas.drawRoundRect(
            rectLeft,
            rectTop,
            rectLeft + windowWidth,
            rectTop + windowHeight,
            8f,
            8f,
            paintWindow
        )

        val leftThickness = maxThickness - (progress * (maxThickness - minThickness))
        val rightThickness = minThickness + (progress * (maxThickness - minThickness))

        // Reel izquierdo
        paintReel.strokeWidth = leftThickness
        canvas.drawCircle(cxLeft, cy, baseRadius, paintReel)

        // Reel derecho
        paintReel.strokeWidth = rightThickness
        canvas.drawCircle(cxRight, cy, baseRadius, paintReel)

        // Centros
        canvas.drawCircle(cxLeft, cy, 55f, paintCenter)
        canvas.drawCircle(cxRight, cy, 55f, paintCenter)

        drawTeeth(canvas, cxLeft, cy, angleLeft)
        drawTeeth(canvas, cxRight, cy, angleRight)
    }

    private fun drawTeeth(canvas: Canvas, cx: Float, cy: Float, angle: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val teeth = 14
        val outerRadius = 48f
        val innerRadius = 52f

        val path = Path()

        for (i in 0 until teeth * 2) {
            val isOuter = i % 2 == 0
            val radius = if (isOuter) outerRadius else innerRadius

            val theta = Math.toRadians((i * (360.0 / (teeth * 2))))
            val x = (cx + radius * Math.cos(theta)).toFloat()
            val y = (cy + radius * Math.sin(theta)).toFloat()

            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        path.close()

        canvas.save()
        canvas.rotate(angle, cx, cy)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
}