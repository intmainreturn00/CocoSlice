//package org.cocos2dx.cpp
package org.cocos2dx.lib

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import org.cocos2dx.cpp.MyCocos2dxGLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

class Cocos2dxFragment : Fragment(), Cocos2dxHelper.Cocos2dxHelperListener {

    private lateinit var surface: Cocos2dxGLSurfaceView
    private lateinit var mGLContextAttrs: IntArray
    private var mWebViewHelper: Cocos2dxWebViewHelper? = null
    private var mEditBoxHelper: Cocos2dxEditBoxHelper? = null
    private var visible = false
    private var paused = true
    private lateinit var rootLayout: ResizeLayout


    override fun showDialog(pTitle: String?, pMessage: String?) {}

    override fun runOnGLThread(pRunnable: Runnable?) {
        this.surface.queueEvent(pRunnable)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            rootLayout


    fun getSurfaceView(): Cocos2dxGLSurfaceView {
        return surface
    }

    fun setKeepScreenOn(value: Boolean) {
        activity?.runOnUiThread { surface.keepScreenOn = value }
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
            activity?.finish()
            println("[Workaround] Ignore the activity started from icon!")
            return
        }

        onLoadNativeLibraries()

        Cocos2dxHelper.init(activity)

        this.mGLContextAttrs = getGLContextAttrs()
        this.init()

        if (mWebViewHelper == null) {
            mWebViewHelper = Cocos2dxWebViewHelper(rootLayout)
        }

        if (mEditBoxHelper == null) {
            mEditBoxHelper = Cocos2dxEditBoxHelper(rootLayout)
        }

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        Cocos2dxEngineDataManager.init(context, surface)
    }


    //native method,call GLViewImpl::getGLContextAttrs() to get the OpenGL ES context attributions
    private external fun getGLContextAttrs(): IntArray

    override fun onResume() {
        paused = false
        super.onResume()
    }

//    override fun setUserVisibleHint(visible: Boolean) {
//        println("setUserVisibleHint($visible)")
//        super.setUserVisibleHint(visible)
//        this.visible = visible
//        if (visible) {
//            resumeIfHasActive()
//            Cocos2dxEngineDataManager.resume()
//        } else {
//            Cocos2dxHelper.onPause()
//            surface.onPause()
//            Cocos2dxEngineDataManager.pause()
//        }
//    }

    private fun resumeIfHasActive() {
        val readyToPlay = !isDeviceLocked() && !isDeviceAsleep()

        if (visible && readyToPlay) {
            Cocos2dxHelper.onResume()
            surface.onResume()
        }
    }


    override fun onPause() {
        paused = true
        super.onPause()
    }


    override fun onDestroy() {
        super.onDestroy()
        Cocos2dxEngineDataManager.destroy()
    }


    fun init() {
        val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)

        rootLayout = ResizeLayout(context)
        rootLayout.layoutParams = params

        val params2 = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        val editText = Cocos2dxEditBox(context)
        editText.layoutParams = params2
        rootLayout.addView(editText)

        this.surface = this.onCreateView()
        rootLayout.addView(this.surface)

        this.surface.setCocos2dxRenderer(Cocos2dxRenderer())
        this.surface.cocos2dxEditText = editText
    }


    private fun onCreateView(): Cocos2dxGLSurfaceView {
        //val glSurfaceView = Cocos2dxGLSurfaceView(context)
        val glSurfaceView = MyCocos2dxGLSurfaceView(context!!) {
            activity?.onBackPressed()
        }
        //this line is need on some device if we specify an alpha bits
        if (mGLContextAttrs?.get(3)!! > 0) glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        // use custom EGLConfigureChooser
        val chooser = Cocos2dxEGLConfigChooser(this.mGLContextAttrs!!)
        glSurfaceView.setEGLConfigChooser(chooser)

        return glSurfaceView
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