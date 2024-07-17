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
package com.innocomm.tensorflowlite.examples.objectdetection.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.PopupWindow
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.innocomm.tensorflowlite.examples.objectdetection.Application
import com.innocomm.tensorflowlite.examples.objectdetection.ObjectDetectorHelper
import com.innocomm.tensorflowlite.examples.objectdetection.databinding.DialogMoreBinding
import com.innocomm.tensorflowlite.examples.objectdetection.databinding.FragmentUvccamBinding
import com.innocomm.tensorflowlite.examples.objectdetection.utils.InnoCameraFragment
import com.innocomm.tensorflowlite.examples.objectdetection.utils.MMKVUtils
import com.innocomm.tensorflowlite.examples.objectdetection.utils.safeLet
import com.innocomm.tensorflowlite.examples.objectdetection.uvc.EffectListDialog
import com.innocomm.tensorflowlite.examples.objectdetection.uvc.EffectListDialog.Companion.KEY_ANIMATION
import com.innocomm.tensorflowlite.examples.objectdetection.uvc.EffectListDialog.Companion.KEY_FILTER
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IPlayCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.render.effect.EffectBlackWhite
import com.jiangdg.ausbc.render.effect.EffectSoul
import com.jiangdg.ausbc.render.effect.EffectZoom
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.utils.bus.BusKey
import com.jiangdg.ausbc.utils.bus.EventBus
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.CaptureMediaView
import com.jiangdg.ausbc.widget.IAspectRatio
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.ByteArrayOutputStream
import java.util.LinkedList
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


data class PreviewData(var data: ByteArray, var width: Int, var height: Int)
data class AnalyzerState(
    var processing: Boolean,
    var lastProcessTime: Long,
    var counter: Int,
    var lastFPS: Int
)

class UVCCamFragment : InnoCameraFragment(), View.OnClickListener,
    CaptureMediaView.OnViewClickListener, ObjectDetectorHelper.DetectorListener {
    //innocomm private var mMultiCameraDialog: MultiCameraDialog? = null
    private lateinit var mMoreBindingView: DialogMoreBinding
    private var mMoreMenu: PopupWindow? = null
    private var isCapturingVideoOrAudio: Boolean = false
    private var isPlayingMic: Boolean = false
    private var mRecTimer: Timer? = null
    private var mRecSeconds = 0
    private var mRecMinute = 0
    private var mRecHours = 0

    //innocomm
    private lateinit var cameraExecutor: ExecutorService
    val analyzerState = Array(1) { AnalyzerState(false, 0, 0, 0) }
    private var objectDetectorHelper: ObjectDetectorHelper? = null
    private var cameraActive = false
    private  var bitmapBuffer: Bitmap?= null

    private val mEffectDataList by lazy {
        arrayListOf(
            CameraEffect.NONE_FILTER,
            CameraEffect(
                EffectBlackWhite.ID,
                "BlackWhite",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectBlackWhite(requireActivity()),
                coverResId = com.innocomm.tensorflowlite.examples.objectdetection.R.mipmap.filter0
            ),
            CameraEffect.NONE_ANIMATION,
            CameraEffect(
                EffectZoom.ID,
                "Zoom",
                CameraEffect.CLASSIFY_ID_ANIMATION,
                effect = EffectZoom(requireActivity()),
                coverResId = com.innocomm.tensorflowlite.examples.objectdetection.R.mipmap.filter2
            ),
            CameraEffect(
                EffectSoul.ID,
                "Soul",
                CameraEffect.CLASSIFY_ID_ANIMATION,
                effect = EffectSoul(requireActivity()),
                coverResId = com.innocomm.tensorflowlite.examples.objectdetection.R.mipmap.filter1
            ),
        )
    }

    private val mMainHandler: Handler by lazy {
        Handler(Looper.getMainLooper()) {
            when (it.what) {

            }
            true
        }
    }

    private var mCameraMode = CaptureMediaView.CaptureMode.MODE_CAPTURE_PIC

    private lateinit var mViewBinding: FragmentUvccamBinding

    override fun initView() {
        super.initView()
        mViewBinding.effectsBtn.setOnClickListener(this)
        mViewBinding.cameraTypeBtn.setOnClickListener(this)
        mViewBinding.settingsBtn.setOnClickListener(this)
        mViewBinding.voiceBtn.setOnClickListener(this)
        mViewBinding.resolutionBtn.setOnClickListener(this)
        mViewBinding.toggleBottomSheet.setOnClickListener(this)
        //innocomm
        Log.v(TAG, "initView()")
        mViewBinding.settingsBtn.visibility = View.GONE
        mViewBinding.effectsBtn.visibility = View.GONE
        mViewBinding.cameraTypeBtn.visibility = View.GONE
        mViewBinding.voiceBtn.visibility = View.GONE
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun initObjectDetector(){
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )
    }

    override fun initData() {
        super.initData()
        EventBus.with<Int>(BusKey.KEY_FRAME_RATE).observe(this, {
            val detectFPS = analyzerState[0].lastFPS
            mViewBinding.frameRateTv.text = Application.previewSize.toString() + "\nFPS:  $it fps\nObjectDetect: $detectFPS fps"
        })

        EventBus.with<Boolean>(BusKey.KEY_RENDER_READY).observe(this, { ready ->
            if (!ready) return@observe
            getDefaultEffect()?.apply {
                when (getClassifyId()) {
                    CameraEffect.CLASSIFY_ID_FILTER -> {
                        // check if need to set anim
                        val animId = MMKVUtils.getInt(KEY_ANIMATION, -99)
                        if (animId != -99) {
                            mEffectDataList.find {
                                it.id == animId
                            }?.also {
                                if (it.effect != null) {
                                    addRenderEffect(it.effect!!)
                                }
                            }
                        }
                        // set effect
                        val filterId = MMKVUtils.getInt(KEY_FILTER, -99)
                        if (filterId != -99) {
                            removeRenderEffect(this)
                            mEffectDataList.find {
                                it.id == filterId
                            }?.also {
                                if (it.effect != null) {
                                    addRenderEffect(it.effect!!)
                                }
                            }
                            return@apply
                        }
                        MMKVUtils.set(KEY_FILTER, getId())
                    }

                    CameraEffect.CLASSIFY_ID_ANIMATION -> {
                        // check if need to set filter
                        val filterId = MMKVUtils.getInt(KEY_ANIMATION, -99)
                        if (filterId != -99) {
                            mEffectDataList.find {
                                it.id == filterId
                            }?.also {
                                if (it.effect != null) {
                                    addRenderEffect(it.effect!!)
                                }
                            }
                        }
                        // set anim
                        val animId = MMKVUtils.getInt(KEY_ANIMATION, -99)
                        if (animId != -99) {
                            removeRenderEffect(this)
                            mEffectDataList.find {
                                it.id == animId
                            }?.also {
                                if (it.effect != null) {
                                    addRenderEffect(it.effect!!)
                                }
                            }
                            return@apply
                        }
                        MMKVUtils.set(KEY_ANIMATION, getId())
                    }

                    else -> throw IllegalStateException("Unsupported classify")
                }
            }
        })
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
            ICameraStateCallBack.State.ERROR -> handleCameraError(msg)
        }
    }

    private fun handleCameraError(msg: String?) {
        mViewBinding.uvcLogoIv.visibility = View.VISIBLE
        mViewBinding.frameRateTv.visibility = View.GONE
        ToastUtils.show("camera opened error: $msg")
        Log.v(TAG, "handleCameraError $msg")
        cameraActive = false
    }

    private fun handleCameraClosed() {
        mViewBinding.uvcLogoIv.visibility = View.VISIBLE
        mViewBinding.frameRateTv.visibility = View.GONE
        ToastUtils.show("camera closed success")
        Log.v(TAG, "handleCameraClosed")
        cameraActive = false
        objectDetectorHelper?.clearObjectDetector()
        mViewBinding.overlay.clear()
    }

    private fun handleCameraOpened() {
        Log.v(TAG, "handleCameraOpened ")
        initObjectDetector()
        initBottomSheetControls()
        mViewBinding.uvcLogoIv.visibility = View.GONE
        mViewBinding.frameRateTv.visibility = View.VISIBLE
        mViewBinding.bottomSheetLayout.brightnessSb.max =
            100//innocomm (getCurrentCamera() as? CameraUVC)?.getBrightnessMax() ?: 100
        mViewBinding.bottomSheetLayout.brightnessSb.progress =
            (getCurrentCamera() as? CameraUVC)?.getBrightness() ?: 0
        Log.v(
            TAG,
            "max = ${mViewBinding.bottomSheetLayout.brightnessSb.max}, progress = ${mViewBinding.bottomSheetLayout.brightnessSb.progress}"
        )

        val corners = Rect()
        if(mViewBinding.cameraViewContainer.getGlobalVisibleRect(corners))
        {
            Log.v(  TAG, "corners = "+corners )
        }
        mViewBinding.bottomSheetLayout.brightnessSb.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                (getCurrentCamera() as? CameraUVC)?.setBrightness(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        ToastUtils.show("camera opened success")
        cameraActive = true
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return mViewBinding.cameraViewContainer
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentUvccamBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun getGravity(): Int = Gravity.CENTER

    override fun onViewClick(mode: CaptureMediaView.CaptureMode?) {
        if (!isCameraOpened()) {
            ToastUtils.show("camera not worked!")
            return
        }
        when (mode) {
            CaptureMediaView.CaptureMode.MODE_CAPTURE_PIC -> {
            }

            CaptureMediaView.CaptureMode.MODE_CAPTURE_AUDIO -> {
            }

            else -> {
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.v(TAG, "onConfigurationChanged()")
        refreshFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //innocomm mMultiCameraDialog?.hide()
        Log.v(TAG, "onDestroyView()")
        cameraExecutor.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.v(TAG, "onDestroy()")
        bitmapBuffer?.let {
            it.recycle()
        }
    }

    override fun onClick(v: View?) {
//        if (! isCameraOpened()) {
//            ToastUtils.show("camera not worked!")
//            return
//        }
        clickAnimation(v!!, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                when (v) {


                    mViewBinding.effectsBtn -> {
                        showEffectDialog()
                    }

                    mViewBinding.cameraTypeBtn -> {
                    }

                    mViewBinding.settingsBtn -> {
                        showMoreMenu()
                    }

                    mViewBinding.voiceBtn -> {
                        playMic()
                    }

                    mViewBinding.resolutionBtn -> {
                        showResolutionDialog()
                    }

                    mViewBinding.toggleBottomSheet ->{
                        val behavior = BottomSheetBehavior.from(mViewBinding.bottomSheetLayout.bottomSheetLayout)
                        if(behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        }else {
                            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }

                    }
                    else -> {
                    }
                }
            }
        })
    }

    @SuppressLint("CheckResult")
    private fun showUsbDevicesDialog(
        usbDeviceList: MutableList<UsbDevice>?,
        curDevice: UsbDevice?
    ) {
        if (usbDeviceList.isNullOrEmpty()) {
            ToastUtils.show("Get usb device failed")
            return
        }
        val list = arrayListOf<String>()
        var selectedIndex: Int = -1
        for (index in (0 until usbDeviceList.size)) {
            val dev = usbDeviceList[index]
            val devName =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !dev.productName.isNullOrEmpty()) {
                    "${dev.productName}(${curDevice?.deviceId})"
                } else {
                    dev.deviceName
                }
            val curDevName =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !curDevice?.productName.isNullOrEmpty()) {
                    "${curDevice!!.productName}(${curDevice.deviceId})"
                } else {
                    curDevice?.deviceName
                }
            if (devName == curDevName) {
                selectedIndex = index
            }
            list.add(devName)
        }
        MaterialDialog(requireContext()).show {
            listItemsSingleChoice(
                items = list,
                initialSelection = selectedIndex
            ) { dialog, index, text ->
                if (selectedIndex == index) {
                    return@listItemsSingleChoice
                }
                switchCamera(usbDeviceList[index])
            }
        }
    }

    private fun showEffectDialog() {
        EffectListDialog(requireActivity()).apply {
            setData(mEffectDataList, object : EffectListDialog.OnEffectClickListener {
                override fun onEffectClick(effect: CameraEffect) {
                    mEffectDataList.find { it.id == effect.id }.also {
                        if (it == null) {
                            ToastUtils.show("set effect failed!")
                            return@also
                        }
                        updateRenderEffect(it.classifyId, it.effect)
                        // save to sp
                        if (effect.classifyId == CameraEffect.CLASSIFY_ID_ANIMATION) {
                            KEY_ANIMATION
                        } else {
                            KEY_FILTER
                        }.also { key ->
                            MMKVUtils.set(key, effect.id)
                        }
                    }
                }
            })
            show()
        }
    }

    @SuppressLint("CheckResult")
    private fun showResolutionDialog() {
        mMoreMenu?.dismiss()
        getAllPreviewSizes().let { previewSizes ->
            if (previewSizes.isNullOrEmpty()) {
                ToastUtils.show("Get camera preview size failed")
                return
            }
            val list = arrayListOf<String>()
            var selectedIndex: Int = -1
            for (index in (0 until previewSizes.size)) {
                val w = previewSizes[index].width
                val h = previewSizes[index].height
                getCurrentPreviewSize()?.apply {
                    if (width == w && height == h) {
                        selectedIndex = index
                    }
                }
                list.add("$w x $h")
            }
            MaterialDialog(requireContext()).show {
                listItemsSingleChoice(
                    items = list,
                    initialSelection = selectedIndex
                ) { dialog, index, text ->
                    if (selectedIndex == index) {
                        return@listItemsSingleChoice
                    }
                    //innocomm
                    //updateResolution(previewSizes[index].width, previewSizes[index].height)
                    Application.getInstance().setPreviewSize(Size(previewSizes[index].width, previewSizes[index].height))
                    mMainHandler.postDelayed({ refreshFragment()},0)
                }
            }
        }
    }

    private fun goToMultiplexActivity() {
        mMoreMenu?.dismiss()
    }

    private fun showContactDialog() {
        mMoreMenu?.dismiss()
        MaterialDialog(requireContext()).show {
            title(com.innocomm.tensorflowlite.examples.objectdetection.R.string.dialog_contact_title)
            message(text = getString(com.innocomm.tensorflowlite.examples.objectdetection.R.string.dialog_contact_message, getVersionName()))
        }
    }

    private fun getVersionName(): String? {
        context ?: return null
        val packageManager = requireContext().packageManager
        try {
            val packageInfo = packageManager?.getPackageInfo(requireContext().packageName, 0)
            return packageInfo?.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    private fun playMic() {
        if (isPlayingMic) {
            stopPlayMic()
            return
        }
        startPlayMic(object : IPlayCallBack {
            override fun onBegin() {
                mViewBinding.voiceBtn.setImageResource(com.innocomm.tensorflowlite.examples.objectdetection.R.mipmap.camera_voice_on)
                isPlayingMic = true
            }

            override fun onError(error: String) {
                mViewBinding.voiceBtn.setImageResource(com.innocomm.tensorflowlite.examples.objectdetection.R.mipmap.camera_voice_off)
                isPlayingMic = false
            }

            override fun onComplete() {
                mViewBinding.voiceBtn.setImageResource(com.innocomm.tensorflowlite.examples.objectdetection.R.mipmap.camera_voice_off)
                isPlayingMic = false
            }
        })
    }

    private fun clickAnimation(v: View, listener: Animator.AnimatorListener) {
        val scaleXAnim: ObjectAnimator = ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 0.4f, 1.0f)
        val scaleYAnim: ObjectAnimator = ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 0.4f, 1.0f)
        val alphaAnim: ObjectAnimator = ObjectAnimator.ofFloat(v, "alpha", 1.0f, 0.4f, 1.0f)
        val animatorSet = AnimatorSet()
        animatorSet.duration = 150
        animatorSet.addListener(listener)
        animatorSet.playTogether(scaleXAnim, scaleYAnim, alphaAnim)
        animatorSet.start()
    }

    private fun showMoreMenu() {
        if (mMoreMenu == null) {
            layoutInflater.inflate(com.innocomm.tensorflowlite.examples.objectdetection.R.layout.dialog_more, null).apply {
                mMoreBindingView = DialogMoreBinding.bind(this)
                mMoreBindingView.multiplex.setOnClickListener(this@UVCCamFragment)
                mMoreBindingView.multiplexText.setOnClickListener(this@UVCCamFragment)
                mMoreBindingView.contact.setOnClickListener(this@UVCCamFragment)
                mMoreBindingView.contactText.setOnClickListener(this@UVCCamFragment)
                mMoreBindingView.resolution.setOnClickListener(this@UVCCamFragment)
                mMoreBindingView.resolutionText.setOnClickListener(this@UVCCamFragment)
                mMoreBindingView.contract.setOnClickListener(this@UVCCamFragment)
                mMoreBindingView.contractText.setOnClickListener(this@UVCCamFragment)
                mMoreMenu = PopupWindow(
                    this,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                ).apply {
                    isOutsideTouchable = true
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            com.innocomm.tensorflowlite.examples.objectdetection.R.mipmap.camera_icon_one_inch_alpha
                        )
                    )
                }
            }
        }
        try {
            mMoreMenu?.showAsDropDown(mViewBinding.settingsBtn, 0, 0, Gravity.START)
        } catch (e: Exception) {
            Logger.e(TAG, "showMoreMenu fail", e)
        }
    }

    private fun startMediaTimer() {
        val pushTask: TimerTask = object : TimerTask() {
            override fun run() {
                //秒
                mRecSeconds++
                //分
                if (mRecSeconds >= 60) {
                    mRecSeconds = 0
                    mRecMinute++
                }
                //时
                if (mRecMinute >= 60) {
                    mRecMinute = 0
                    mRecHours++
                    if (mRecHours >= 24) {
                        mRecHours = 0
                        mRecMinute = 0
                        mRecSeconds = 0
                    }
                }
                mMainHandler.sendEmptyMessage(WHAT_START_TIMER)
            }
        }
        if (mRecTimer != null) {
            stopMediaTimer()
        }
        mRecTimer = Timer()
        //执行schedule后1s后运行run，之后每隔1s运行run
        mRecTimer?.schedule(pushTask, 1000, 1000)
    }

    private fun stopMediaTimer() {
        if (mRecTimer != null) {
            mRecTimer?.cancel()
            mRecTimer = null
        }
        mRecHours = 0
        mRecMinute = 0
        mRecSeconds = 0
        mMainHandler.sendEmptyMessage(WHAT_STOP_TIMER)
    }

    private fun calculateTime(seconds: Int, minute: Int, hour: Int? = null): String {
        val mBuilder = java.lang.StringBuilder()
        //时
        if (hour != null) {
            if (hour < 10) {
                mBuilder.append("0")
                mBuilder.append(hour)
            } else {
                mBuilder.append(hour)
            }
            mBuilder.append(":")
        }
        // 分
        if (minute < 10) {
            mBuilder.append("0")
            mBuilder.append(minute)
        } else {
            mBuilder.append(minute)
        }
        //秒
        mBuilder.append(":")
        if (seconds < 10) {
            mBuilder.append("0")
            mBuilder.append(seconds)
        } else {
            mBuilder.append(seconds)
        }
        return mBuilder.toString()
    }

    companion object {
        private const val TAG = "UVCCamFragment"
        private const val WHAT_START_TIMER = 0x00
        private const val WHAT_STOP_TIMER = 0x01
    }

    //innocomm
    fun processWithControlledFPS(
        data: PreviewData,
        run: (image: Bitmap, cb: (Boolean) -> Unit) -> Unit
    ) {
        cameraExecutor.execute {

            val state = analyzerState[0]
            if (!state.processing) {
                state.processing = true
                try {
                    if (SystemClock.uptimeMillis() - state.lastProcessTime > 1000) {
                        state.lastProcessTime = SystemClock.uptimeMillis()
                        state.lastFPS = state.counter
                        //Log.v(TAG, "FPS " + analyzerState[0])
                        state.counter = 0
                    } else {
                        state.counter++
                    }

                    val yuvImage =
                        YuvImage(data.data, ImageFormat.NV21, data.width, data.height, null)
                    ByteArrayOutputStream().use { outStream ->
                        yuvImage.compressToJpeg(
                            Rect(0, 0, yuvImage.width, yuvImage.height),
                            75,
                            outStream
                        )
                        val imageBytes = outStream.toByteArray()
                        bitmapBuffer?.let {
                            Log.v(TAG, "bitmap should null??")
                            it.recycle()
                        }
                        bitmapBuffer = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        bitmapBuffer?.let {
                            run(it) {

                            }
                        }

                    }
                } catch (e: Exception) {
                    Log.v(TAG, "Exception: " + e.toString())
                    state.processing = false
                }
            }else{
                Log.v(TAG, "drop frame()")
            }

        }
    }

    var speedControlTime = 0L
    override fun onPreviewCallBack(
        data: ByteArray?,
        width: Int,
        height: Int,
        format: IPreviewDataCallBack.DataFormat
    ) {
        //Innocomm: Slowdown to fix OOM issue on High resolution Input
        if((SystemClock.uptimeMillis()-speedControlTime)<50  && Application.shouldRestrict()) return
        speedControlTime = SystemClock.uptimeMillis()
        safeLet(objectDetectorHelper,data) {detector,previewData->
            //Log.v(TAG,"width:"+width+" height:"+height+" "+format+" "+it.size)
            processWithControlledFPS(PreviewData(previewData, width, height)) { bitmap, onDone ->
                try {
                    //Log.v(TAG,"width:"+bitmap.width+" height:"+bitmap.height+" "+format)
                    //bitmap.recycle()
                    //analyzerState[0].processing = false
                    //720p has memory leak?
                    detector.detect(bitmap,0)
                    onDone(true)
                } catch (e: Exception) {
                    Log.v(TAG, "scanner Exception: " + e.toString())
                    onDone(false)
                }
            }
        }
    }

    override fun onSurfaceSizeChanged(surfaceWidth: Int, surfaceHeight: Int) {
        mViewBinding.overlay.setSurfaceSize(surfaceWidth,surfaceHeight)
    }

    //Object Detect Result
    override fun onError(error: String) {
        Log.v(TAG, "onError: " + error)
        val state = analyzerState[0]
        state.processing = false
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        if(cameraActive) {
            /*results?.forEachIndexed { idx, result ->
                val rect = result.boundingBox
                val categorys = result.categories
                //Log.v(TAG, "Object: " + idx + " " + rect.top + ":" + rect.left + " " + categorys.map { it.label }.toSet().joinToString(separator = ","))
            }*/
            activity?.runOnUiThread {
                mViewBinding.bottomSheetLayout.inferenceTimeVal.text =  String.format("%d ms", inferenceTime)

                // Pass necessary information to OverlayView for drawing on the canvas
                mViewBinding.overlay.setResults(
                    results ?: LinkedList<Detection>(),
                    imageHeight,
                    imageWidth
                )

                // Force a redraw
                mViewBinding.overlay.invalidate()
            }
        }

        bitmapBuffer?.let {
            //Log.v(TAG, "recycle:")
            it.recycle()
        }
        bitmapBuffer= null

        val state = analyzerState[0]
        state.processing = false
    }

    fun refreshFragment(){
        val controller = Navigation.findNavController(requireActivity(), com.innocomm.tensorflowlite.examples.objectdetection.R.id.fragment_container)
        val fragmentId = controller.currentDestination?.id
        controller.popBackStack(fragmentId!!,true)
        controller.navigate(fragmentId)
    }

    private fun initBottomSheetControls() {
        objectDetectorHelper?.let { helper ->
            helper.threshold = Application.object_threshold
            helper.maxResults= Application.object_maxResults
            helper.numThreads=Application.object_numThreads
            helper.currentDelegate=Application.object_currentDelegate
            helper.currentModel=Application.object_currentModel

            mViewBinding.bottomSheetLayout.maxResultsValue.text =
                helper.maxResults.toString()
            mViewBinding.bottomSheetLayout.thresholdValue.text =
                String.format("%.2f", helper.threshold)
            mViewBinding.bottomSheetLayout.threadsValue.text =
                helper.numThreads.toString()

            Log.e(TAG, "initBottomSheetControls threshold: " + helper.threshold+" maxResults: " + helper.maxResults+" currentDelegate: " + helper.currentDelegate
                    +" currentModel: " + helper.currentModel)
            // When clicked, lower detection score threshold floor
            mViewBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
                if (helper.threshold >= 0.1) {
                    helper.threshold -= 0.1f
                    updateControlsUi(true)
                }
            }

            // When clicked, raise detection score threshold floor
            mViewBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
                if (helper.threshold <= 0.8) {
                    helper.threshold += 0.1f
                    updateControlsUi(true)
                }
            }

            // When clicked, reduce the number of objects that can be detected at a time
            mViewBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
                if (helper.maxResults > 1) {
                    helper.maxResults--
                    updateControlsUi(true)
                }
            }

            // When clicked, increase the number of objects that can be detected at a time
            mViewBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
                if (helper.maxResults < 5) {
                    helper.maxResults++
                    updateControlsUi(true)
                }
            }

            // When clicked, decrease the number of threads used for detection
            mViewBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
                if (helper.numThreads > 1) {
                    helper.numThreads--
                    updateControlsUi(true)
                }
            }

            // When clicked, increase the number of threads used for detection
            mViewBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
                if (helper.numThreads < 4) {
                    helper.numThreads++
                    updateControlsUi(true)
                }
            }

            // When clicked, change the underlying hardware used for inference. Current options are CPU
            // GPU, and NNAPI
            mViewBinding.bottomSheetLayout.spinnerDelegate.setSelection(Application.object_currentDelegate, false)
            mViewBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        helper.currentDelegate = p2
                        updateControlsUi(true)
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        /* no op */
                    }
                }

            // When clicked, change the underlying model used for object detection
            mViewBinding.bottomSheetLayout.spinnerModel.setSelection(Application.object_currentModel, false)
            mViewBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        helper.currentModel = p2
                        updateControlsUi(true)
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        /* no op */
                    }
                }
        }
    }

    private fun updateControlsUi(restart: Boolean) {

        objectDetectorHelper?.let {
             Application.object_threshold = it.threshold
             Application.object_maxResults= it.maxResults
             Application.object_numThreads=it.numThreads
             Application.object_currentDelegate=it.currentDelegate
             Application.object_currentModel=it.currentModel

            mViewBinding.bottomSheetLayout.maxResultsValue.text =
                it.maxResults.toString()
            mViewBinding.bottomSheetLayout.thresholdValue.text =
                String.format("%.2f", it.threshold)
            mViewBinding.bottomSheetLayout.threadsValue.text =
                it.numThreads.toString()

            // Needs to be cleared instead of reinitialized because the GPU
            // delegate needs to be initialized on the thread using it when applicable
            it.clearObjectDetector()
            mViewBinding.overlay.clear()
            if (restart) mMainHandler.postDelayed({ refreshFragment()},0)
        }
    }
}
