package com.github.intmainreturn00.cocoslice

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*
import org.cocos2dx.lib.Cocos2dxHelper

class MainActivity: AppCompatActivity(), Cocos2dxHelper.Cocos2dxHelperListener {
    override fun showDialog(pTitle: String?, pMessage: String?) {
    }

    override fun runOnGLThread(pRunnable: Runnable?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val myPagerAdapter = MyPagerAdapter(supportFragmentManager)
        pager.adapter = myPagerAdapter
        tablayout.setupWithViewPager(pager)
    }
}