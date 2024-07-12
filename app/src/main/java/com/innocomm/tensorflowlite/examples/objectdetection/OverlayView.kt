/*
 * Copyright 2024 InnoComm Mobile Technology Corporation , All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.innocomm.tensorflowlite.examples.objectdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var mysurfaceHeight: Int =0
    private var mysurfaceWidth: Int = 0
    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()
    private var isPortrait = false

    init {
        initPaints()
    }

    fun clear() {
        scaleFactor = 1F
        mysurfaceHeight = 0
        mysurfaceWidth = 0
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        initPaints()
        invalidate()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor+getoffsetY()
            val bottom = boundingBox.bottom * scaleFactor+getoffsetY()
            val left = (boundingBox.left ) * scaleFactor +getoffsetX()
            val right = (boundingBox.right ) * scaleFactor +getoffsetX()

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val drawableText =
                result.categories[0].label + " " +
                        String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    fun setResults(
      detectionResults: MutableList<Detection>,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        //scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        if (isPortrait)
            scaleFactor = width * 1f / imageWidth
        else
            scaleFactor = height * 1f / imageHeight

        if (showFactor){
            showFactor = false
            Log.v("innocomm", "scaleFactor:"+scaleFactor)
            Log.v("innocomm", "offset:"+getoffsetX()+"x"+getoffsetY())
        }
    }
    var showFactor = false
    fun setSurfaceSize(surfaceWidth: Int, surfaceHeight: Int) {
        isPortrait = height>width
        Log.v("innocomm", "setSurfaceSize:"+width+"/"+surfaceWidth+" "+height+"/"+surfaceHeight)
        Log.v("innocomm", "isPortrait:"+ isPortrait)
        if(surfaceWidth!=mysurfaceWidth || mysurfaceHeight!= surfaceHeight) showFactor = true
        mysurfaceWidth = surfaceWidth
        mysurfaceHeight= surfaceHeight
    }

    fun getoffsetX():Float{
        val offset = (width-mysurfaceWidth)/2F
        return if (isPortrait) 0f else offset
    }
    fun getoffsetY():Float{
        val offset = (height-mysurfaceHeight)/2F
        return  offset
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
