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

    private var isReversed = false

    fun setReversed(value: Boolean) {
        isReversed = value
    }

    fun updateRotation() {
        val speedLeft = 2f + (progress * 6f)
        val speedRight = 8f - (progress * 6f)

        if (!isReversed) {
            angleLeft += speedLeft
            angleRight -= speedRight
        } else {
            // Invertido
            angleLeft -= speedLeft
            angleRight += speedRight
        }

        invalidate()
    }

    fun setProgress(p: Float) {
        progress = if (!isReversed) {
            p.coerceIn(0f, 1f)
        } else {
            (1f - p).coerceIn(0f, 1f)
        }
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

        val cx = width * 0.5f

        val baseRadius = 60f

        val minThickness = 10f
        val maxThickness = 40f

        val windowWidth = width * 0.20f
        val windowHeight = height * 0.30f

        val paintWindow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }

        // Fondo de la base
        val bodyWidth = width * 0.28f
        val bodyHeight = height * 0.60f

        val bodyLeft = (width / 2) - (bodyWidth / 2)
        val bodyTop = (height / 2) - (bodyHeight / 2)
        val bodyRight = bodyLeft + bodyWidth
        val bodyBottom = bodyTop + bodyHeight

        val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A2A2A") // gris oscuro bonito
        }

        canvas.drawRoundRect(
            bodyLeft,
            bodyTop,
            bodyRight,
            bodyBottom,
            20f,
            20f,
            paintBody
        )

        val bodyCenterY = (bodyTop + bodyBottom) / 2f

        val reelSpacing = bodyHeight * 0.38f

        val cyTop = bodyCenterY - reelSpacing
        val cyBottom = bodyCenterY + reelSpacing

        val rectLeft = (width / 2) - (windowWidth / 2)
        val centerY = (cyTop + cyBottom) / 2f
        val rectTop = centerY - (windowHeight / 2)

        canvas.drawRoundRect(
            rectLeft,
            rectTop,
            rectLeft + windowWidth,
            rectTop + windowHeight,
            8f,
            8f,
            paintWindow
        )

        val bottomThickness = maxThickness - (progress * (maxThickness - minThickness))
        val topThickness = minThickness + (progress * (maxThickness - minThickness))

        // Reel inferior
        paintReel.strokeWidth = bottomThickness
        canvas.drawCircle(cx, cyBottom, baseRadius, paintReel)

        // Reel derecho
        paintReel.strokeWidth = topThickness
        canvas.drawCircle(cx, cyTop, baseRadius, paintReel)

        // Centros
        canvas.drawCircle(cx, cyBottom, 55f, paintCenter)
        canvas.drawCircle(cx, cyTop, 55f, paintCenter)

        drawTeeth(canvas, cx, cyBottom, angleLeft)
        drawTeeth(canvas, cx, cyTop, angleRight)
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