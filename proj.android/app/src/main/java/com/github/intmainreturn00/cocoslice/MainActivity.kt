package com.github.intmainreturn00.cocoslice

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

import kotlinx.android.synthetic.main.activity_main.*
import org.cocos2dx.lib.Cocos2dxHelper


class MainActivity : AppCompatActivity(), Cocos2dxHelper.Cocos2dxHelperListener, AndroidTab.OnNextButtonPressed {

    override fun showDialog(pTitle: String?, pMessage: String?) {}

    override fun runOnGLThread(pRunnable: Runnable?) {}

    override fun buttonPressed() = pager.setCurrentItem(2, true)

    lateinit var adapter: MyPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adapter = MyPagerAdapter(supportFragmentManager)
        pager.adapter = adapter
        tablayout.setupWithViewPager(pager)
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

}