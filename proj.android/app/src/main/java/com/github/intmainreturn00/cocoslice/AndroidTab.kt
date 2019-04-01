package com.github.intmainreturn00.cocoslice

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_android_tab.*
import kotlin.random.Random


class AndroidTab : Fragment() {

    private var listener: OnNextButtonPressed? = null

    public interface OnNextButtonPressed {
        fun buttonPressed()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? OnNextButtonPressed
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_android_tab, container, false)
    }

    override fun onStart() {
        super.onStart()
        button.setOnClickListener { listener?.buttonPressed() }
    }

}
