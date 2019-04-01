package com.github.intmainreturn00.cocoslice

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import org.cocos2dx.lib.Cocos2dxFragment

class MyPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment? = when (position) {
        0 -> AndroidTab()
        1 -> AnotherAndroidTab()//Cocos2dxFragment()
        //2 -> //AnotherAndroidTab()
        else -> null
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence? = when (position) {
        0 -> "Android"
        1 -> "Cocos"
        2 -> "Another Android"
        else -> null
    }
}
