package org.cocos2dx.cpp

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import org.cocos2dx.lib.Cocos2dxGLSurfaceView


class MyCocos2dxGLSurfaceView : Cocos2dxGLSurfaceView {
    lateinit var back: () -> Unit

    constructor(context: Context, back: () -> Unit) : super(context) {
        this.back = back
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onKeyDown(pKeyCode: Int, pKeyEvent: KeyEvent): Boolean {
        return if (pKeyCode == KeyEvent.KEYCODE_BACK) {
            back()
            true
        } else super.onKeyDown(pKeyCode, pKeyEvent)
    }
}