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
import android.util.Log
import android.util.Size
import androidx.multidex.MultiDex
import com.innocomm.tensorflowlite.examples.objectdetection.utils.MMKVUtils
import com.jiangdg.ausbc.base.BaseApplication
import kotlin.math.abs

class Application: BaseApplication() {

    companion object {
        private const val TAG = "InnoApplication"
        var previewSize = Size(640,480)
        val modelInputRatio = 640f / 480f
        val maxDifference = 1e-5
        private lateinit var instance: Application
        var mShouldRestrict = false
        fun shouldRestrict():Boolean{
            return mShouldRestrict
        }

        fun getInstance(): Application {
            return instance
        }

        var object_threshold = 0.5f
        var object_numThreads= 2
        var object_maxResults= 3
        var object_currentDelegate=0
        var object_currentModel=0
    }
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        MMKVUtils.init(this)
    }

    fun setPreviewSize(size: Size){
        val inputRatio = size.width.toFloat() / size.height.toFloat()
        previewSize = size
        mShouldRestrict = abs(modelInputRatio - inputRatio) > maxDifference
        Log.v(TAG,"mShouldRestrict "+mShouldRestrict+" "+modelInputRatio+"/"+inputRatio)
    }
}