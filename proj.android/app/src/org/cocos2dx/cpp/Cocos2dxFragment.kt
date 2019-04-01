//package org.cocos2dx.cpp
package org.cocos2dx.lib

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.os.PowerManager
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

class Cocos2dxFragment : Fragment(), Cocos2dxHelper.Cocos2dxHelperListener {

    companion object {
        const val TAG = "Cocos2dxFragment"
    }

    private var mGLSurfaceView: Cocos2dxGLSurfaceView? = null
    private var mGLContextAttrs: IntArray? = null
    private var handler: Cocos2dxHandler? = null
    private var mVideoHelper: Cocos2dxVideoHelper? = null
    private var mWebViewHelper: Cocos2dxWebViewHelper? = null
    private var mEditBoxHelper: Cocos2dxEditBoxHelper? = null
    private var hasFocus = false
    private var showVirtualButton = false
    private var gainAudioFocus = false
    private var paused = true


    override fun showDialog(pTitle: String?, pMessage: String?) {
        val msg = Message()
        msg.what = Cocos2dxHandler.HANDLER_SHOW_DIALOG
        msg.obj = Cocos2dxHandler.DialogMessage(pTitle, pMessage)
        this.handler?.sendMessage(msg)
    }

    override fun runOnGLThread(pRunnable: Runnable?) {
        this.mGLSurfaceView?.queueEvent(pRunnable)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return mFrameLayout
    }


    fun getGLSurfaceView(): Cocos2dxGLSurfaceView {
        return mGLSurfaceView!!
    }

    fun setKeepScreenOn(value: Boolean) {
        activity?.runOnUiThread(Runnable { mGLSurfaceView?.setKeepScreenOn(value) })
    }

    fun setEnableVirtualButton(value: Boolean) {
        this.showVirtualButton = value
    }

    fun setEnableAudioFocusGain(value: Boolean) {
        if (gainAudioFocus != value) {
            if (!paused) {
                if (value)
                    Cocos2dxAudioFocusManager.registerAudioFocusListener(context)
                else
                    Cocos2dxAudioFocusManager.unregisterAudioFocusListener(context)
            }
            gainAudioFocus = value
        }
    }

    private fun onLoadNativeLibraries() {
        try {
            val ai = context?.packageManager?.getApplicationInfo(context?.packageName, PackageManager.GET_META_DATA)
            val bundle = ai?.metaData
            val libName = bundle?.getString("android.app.lib_name")
            System.loadLibrary(libName!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Workaround in https://stackoverflow.com/questions/16283079/re-launch-of-activity-on-home-button-but-only-the-first-time/16447508
        if (!activity?.isTaskRoot!!) {
            // Android launched another instance of the root activity into an existing task
            //  so just quietly finish and go away, dropping the user back into the activity
            //  at the top of the stack (ie: the last state of this task)
            activity?.finish()
            Log.w(TAG, "[Workaround] Ignore the activity started from icon!")
            return
        }

        this.hideVirtualButton()

        onLoadNativeLibraries()

        //sContext = context
        //this.handler = Cocos2dxHandler(activity)

        Cocos2dxHelper.init(activity)

        this.mGLContextAttrs = getGLContextAttrs()
        this.init()

        /*if (mVideoHelper == null) {
            mVideoHelper = Cocos2dxVideoHelper(activity, mFrameLayout)
        }
        */
        if (mWebViewHelper == null) {
            mWebViewHelper = Cocos2dxWebViewHelper(mFrameLayout)
        }

        if (mEditBoxHelper == null) {
            mEditBoxHelper = Cocos2dxEditBoxHelper(mFrameLayout)
        }

        val window = activity?.window
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        // Audio configuration
        //this.setVolumeControlStream(AudioManager.STREAM_MUSIC)

        Cocos2dxEngineDataManager.init(context, mGLSurfaceView)
    }

    //native method,call GLViewImpl::getGLContextAttrs() to get the OpenGL ES context attributions
    private external fun getGLContextAttrs(): IntArray

    override fun onResume() {
        Log.d(TAG, "onResume()")
        paused = false
        super.onResume()
        if (gainAudioFocus)
            Cocos2dxAudioFocusManager.registerAudioFocusListener(context)
        this.hideVirtualButton()
        resumeIfHasFocus()

        Cocos2dxEngineDataManager.resume()
    }

    override fun setUserVisibleHint(visible: Boolean) {
        super.setUserVisibleHint(visible)
        hasFocus = visible
        if (hasFocus)
            resumeIfHasFocus()
    }

    private fun resumeIfHasFocus() {
        //It is possible for the app to receive the onWindowsFocusChanged(true) event
        //even though it is locked or asleep
        val readyToPlay = !isDeviceLocked() && !isDeviceAsleep()

        if (hasFocus && readyToPlay) {
            this.hideVirtualButton()
            Cocos2dxHelper.onResume()
            mGLSurfaceView?.onResume()
        }
    }


    override fun onPause() {
        Log.d(TAG, "onPause()")
        paused = true
        super.onPause()
        if (gainAudioFocus)
            Cocos2dxAudioFocusManager.unregisterAudioFocusListener(context)
        Cocos2dxHelper.onPause()
        mGLSurfaceView?.onPause()
        Cocos2dxEngineDataManager.pause()
    }

    override fun onDestroy() {
        if (gainAudioFocus)
            Cocos2dxAudioFocusManager.unregisterAudioFocusListener(context)
        super.onDestroy()

        Cocos2dxEngineDataManager.destroy()
    }


    lateinit var mFrameLayout: ResizeLayout
    fun init() {

        val framelayout_params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)

        mFrameLayout = ResizeLayout(context)

        mFrameLayout.layoutParams = framelayout_params

        // Cocos2dxEditText layout
        val edittext_layout_params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        val edittext = Cocos2dxEditBox(context)
        edittext.layoutParams = edittext_layout_params
        mFrameLayout.addView(edittext)

        this.mGLSurfaceView = this.onCreateView()
        mFrameLayout.addView(this.mGLSurfaceView)

        this.mGLSurfaceView?.setCocos2dxRenderer(Cocos2dxRenderer())
        this.mGLSurfaceView?.cocos2dxEditText = edittext
    }


    private fun onCreateView(): Cocos2dxGLSurfaceView {
        val glSurfaceView = Cocos2dxGLSurfaceView(context)
        //this line is need on some device if we specify an alpha bits
        if (mGLContextAttrs?.get(3)!! > 0) glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        // use custom EGLConfigureChooser
        val chooser = Cocos2dxEGLConfigChooser(this.mGLContextAttrs!!)
        glSurfaceView.setEGLConfigChooser(chooser)

        return glSurfaceView
    }

    private fun hideVirtualButton() {
        if (showVirtualButton) {
            return
        }

        if (Build.VERSION.SDK_INT >= 19) {
            // use reflection to remove dependence of API level

            val viewClass = View::class.java

            try {
                val SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = Cocos2dxReflectionHelper.getConstantValue<Int>(viewClass, "SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION")!!
                val SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = Cocos2dxReflectionHelper.getConstantValue<Int>(viewClass, "SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN")!!
                val SYSTEM_UI_FLAG_HIDE_NAVIGATION = Cocos2dxReflectionHelper.getConstantValue<Int>(viewClass, "SYSTEM_UI_FLAG_HIDE_NAVIGATION")!!
                val SYSTEM_UI_FLAG_FULLSCREEN = Cocos2dxReflectionHelper.getConstantValue<Int>(viewClass, "SYSTEM_UI_FLAG_FULLSCREEN")!!
                val SYSTEM_UI_FLAG_IMMERSIVE_STICKY = Cocos2dxReflectionHelper.getConstantValue<Int>(viewClass, "SYSTEM_UI_FLAG_IMMERSIVE_STICKY")!!
                val SYSTEM_UI_FLAG_LAYOUT_STABLE = Cocos2dxReflectionHelper.getConstantValue<Int>(viewClass, "SYSTEM_UI_FLAG_LAYOUT_STABLE")!!

                // getWindow().getDecorView().setSystemUiVisibility();
                val parameters = arrayOf<Any>(SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar

                        or SYSTEM_UI_FLAG_FULLSCREEN // hide status bar

                        or SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                Cocos2dxReflectionHelper.invokeInstanceMethod<Void>(activity?.window?.decorView,
                        "setSystemUiVisibility",
                        arrayOf<Class<*>>(Integer.TYPE),
                        parameters)
            } catch (e: NullPointerException) {
                Log.e(TAG, "hideVirtualButton", e)
            }

        }
    }


    private fun isDeviceLocked(): Boolean {
        val keyguardManager = context?.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.inKeyguardRestrictedInputMode() ?: false
    }

    private fun isDeviceAsleep(): Boolean {
        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            !powerManager.isInteractive
        } else {
            !powerManager.isScreenOn
        }
    }

    private inner class Cocos2dxEGLConfigChooser : GLSurfaceView.EGLConfigChooser {
        private var mConfigAttributes: IntArray? = null
        private val EGL_OPENGL_ES2_BIT = 0x04
        private val EGL_OPENGL_ES3_BIT = 0x40

        constructor(redSize: Int, greenSize: Int, blueSize: Int, alphaSize: Int, depthSize: Int, stencilSize: Int, multisamplingCount: Int) {
            mConfigAttributes = intArrayOf(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize, multisamplingCount)
        }

        constructor(attributes: IntArray) {
            mConfigAttributes = attributes
        }

        override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig? {
            val EGLAttributes = arrayOf(intArrayOf(
                    // GL ES 2 with user set
                    EGL10.EGL_RED_SIZE, mConfigAttributes!![0], EGL10.EGL_GREEN_SIZE, mConfigAttributes!![1], EGL10.EGL_BLUE_SIZE, mConfigAttributes!![2], EGL10.EGL_ALPHA_SIZE, mConfigAttributes!![3], EGL10.EGL_DEPTH_SIZE, mConfigAttributes!![4], EGL10.EGL_STENCIL_SIZE, mConfigAttributes!![5], EGL10.EGL_SAMPLE_BUFFERS, if (mConfigAttributes!![6] > 0) 1 else 0, EGL10.EGL_SAMPLES, mConfigAttributes!![6], EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE), intArrayOf(
                    // GL ES 2 with user set 16 bit depth buffer
                    EGL10.EGL_RED_SIZE, mConfigAttributes!![0], EGL10.EGL_GREEN_SIZE, mConfigAttributes!![1], EGL10.EGL_BLUE_SIZE, mConfigAttributes!![2], EGL10.EGL_ALPHA_SIZE, mConfigAttributes!![3], EGL10.EGL_DEPTH_SIZE, if (mConfigAttributes!![4] >= 24) 16 else mConfigAttributes!![4], EGL10.EGL_STENCIL_SIZE, mConfigAttributes!![5], EGL10.EGL_SAMPLE_BUFFERS, if (mConfigAttributes!![6] > 0) 1 else 0, EGL10.EGL_SAMPLES, mConfigAttributes!![6], EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE), intArrayOf(
                    // GL ES 2 with user set 16 bit depth buffer without multisampling
                    EGL10.EGL_RED_SIZE, mConfigAttributes!![0], EGL10.EGL_GREEN_SIZE, mConfigAttributes!![1], EGL10.EGL_BLUE_SIZE, mConfigAttributes!![2], EGL10.EGL_ALPHA_SIZE, mConfigAttributes!![3], EGL10.EGL_DEPTH_SIZE, if (mConfigAttributes!![4] >= 24) 16 else mConfigAttributes!![4], EGL10.EGL_STENCIL_SIZE, mConfigAttributes!![5], EGL10.EGL_SAMPLE_BUFFERS, 0, EGL10.EGL_SAMPLES, 0, EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE), intArrayOf(
                    // GL ES 2 by default
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE))

            var result: EGLConfig? = null
            for (eglAtribute in EGLAttributes) {
                result = this.doChooseConfig(egl, display, eglAtribute)
                if (result != null)
                    return result
            }

            Log.e(Context.DEVICE_POLICY_SERVICE, "Can not select an EGLConfig for rendering.")
            return null
        }

        private fun doChooseConfig(egl: EGL10, display: EGLDisplay, attributes: IntArray): EGLConfig? {
            val configs = arrayOfNulls<EGLConfig>(1)
            val matchedConfigNum = IntArray(1)
            val result = egl.eglChooseConfig(display, attributes, configs, 1, matchedConfigNum)
            return if (result && matchedConfigNum[0] > 0) {
                configs[0]
            } else null
        }
    }

}