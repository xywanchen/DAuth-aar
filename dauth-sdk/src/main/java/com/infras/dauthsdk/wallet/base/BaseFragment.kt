package com.infras.dauthsdk.wallet.base

import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {
    protected val logTag: String = javaClass.simpleName
}