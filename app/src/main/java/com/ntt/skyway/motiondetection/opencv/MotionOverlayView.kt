package com.ntt.skyway.motiondetection.opencv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class MotionOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private var motionRects: List<android.graphics.Rect> = emptyList()
    private var frameWidth: Int = 1  // Default to prevent division by zero
    private var frameHeight: Int = 1

    fun updateMotionRects(rects: List<org.opencv.core.Rect>, frameWidth: Int, frameHeight: Int) {
        val viewWidth = this.width.toFloat()
        val viewHeight = this.height.toFloat()

        motionRects = rects.map { rect ->
            // Original mapping (which you said appears "most accurate")
            val originalLeft = (rect.y.toFloat() / frameHeight) * viewWidth
            val top = (rect.x.toFloat() / frameWidth) * viewHeight
            val originalRight = ((rect.y + rect.height).toFloat() / frameHeight) * viewWidth
            val bottom = ((rect.x + rect.width).toFloat() / frameWidth) * viewHeight

            // Flip horizontally by mirroring with respect to the view width
            val left = viewWidth - originalRight
            val right = viewWidth - originalLeft

            android.graphics.Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        }
        postInvalidate()  // Request redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in motionRects) {
            canvas.drawRect(rect, paint)
        }
    }
}


