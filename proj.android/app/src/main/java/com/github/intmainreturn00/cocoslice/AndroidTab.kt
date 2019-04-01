package com.github.intmainreturn00.cocoslice

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_android_tab.*
import kotlin.random.Random


class AndroidTab : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_android_tab, container, false)
    }

    override fun onStart() {
        super.onStart()
        button.setOnClickListener { rating.rating = Random.nextInt(1, 5).toFloat() }
    }

}
