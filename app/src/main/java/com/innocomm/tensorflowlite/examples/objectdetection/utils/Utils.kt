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
package com.innocomm.tensorflowlite.examples.objectdetection.utils

import android.graphics.Bitmap
import android.util.Log
import kotlin.math.abs

inline fun <T1: Any, T2: Any, R: Any> safeLet(p1: T1?, p2: T2?, block: (T1, T2)->R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}
inline fun <T1: Any, T2: Any, T3: Any, R: Any> safeLet(p1: T1?, p2: T2?, p3: T3?, block: (T1, T2, T3)->R?): R? {
    return if (p1 != null && p2 != null && p3 != null) block(p1, p2, p3) else null
}
inline fun <T1: Any, T2: Any, T3: Any, T4: Any, R: Any> safeLet(p1: T1?, p2: T2?, p3: T3?, p4: T4?, block: (T1, T2, T3, T4)->R?): R? {
    return if (p1 != null && p2 != null && p3 != null && p4 != null) block(p1, p2, p3, p4) else null
}
inline fun <T1: Any, T2: Any, T3: Any, T4: Any, T5: Any, R: Any> safeLet(p1: T1?, p2: T2?, p3: T3?, p4: T4?, p5: T5?, block: (T1, T2, T3, T4, T5)->R?): R? {
    return if (p1 != null && p2 != null && p3 != null && p4 != null && p5 != null) block(p1, p2, p3, p4, p5) else null
}

/** Crop Bitmap to maintain aspect ratio of model input.   */
const val MODEL_WIDTH = 640
const val MODEL_HEIGHT = 480
fun cropBitmap(bitmap: Bitmap): Bitmap {
    val bitmapRatio = bitmap.height.toFloat() / bitmap.width
    val modelInputRatio = MODEL_HEIGHT.toFloat() / MODEL_WIDTH
    var croppedBitmap = bitmap

    // Acceptable difference between the modelInputRatio and bitmapRatio to skip cropping.
    val maxDifference = 1e-5

    // Checks if the bitmap has similar aspect ratio as the required model input.
    when {
        abs(modelInputRatio - bitmapRatio) < maxDifference -> return croppedBitmap
        modelInputRatio < bitmapRatio -> {
            // New image is taller so we are height constrained.
            val cropHeight = bitmap.height - (bitmap.width.toFloat() / modelInputRatio)

            croppedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                (cropHeight / 2).toInt(),
                bitmap.width,
                (bitmap.height - cropHeight).toInt()
            )
            bitmap.recycle()
        }
        else -> {
            val cropWidth = bitmap.width - (bitmap.height.toFloat() * modelInputRatio)
            croppedBitmap = Bitmap.createBitmap(
                bitmap,
                (cropWidth / 2).toInt(),
                0,
                (bitmap.width - cropWidth).toInt(),
                bitmap.height
            )
            bitmap.recycle()
        }
    }
    Log.v("innocomm","Crop bitmap "+croppedBitmap.width+"x"+croppedBitmap.height)
    return croppedBitmap
}

fun scaleBitmap(bitmap: Bitmap): Bitmap {
    var scaledBitmap = bitmap
    val scaleRatio = bitmap.width.toFloat() / MODEL_WIDTH

    if((bitmap.width<1000 && bitmap.height<1000) || scaleRatio==1f) return scaledBitmap

    scaledBitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width/scaleRatio).toInt(),
        (bitmap.height/scaleRatio).toInt(), true)
    bitmap.recycle()
    Log.v("innocomm","scaleBitmap "+scaledBitmap.width+"x"+scaledBitmap.height)
    return scaledBitmap
}
