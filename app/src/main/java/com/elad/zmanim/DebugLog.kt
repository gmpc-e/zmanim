package com.elad.zmanim

import android.util.Log

object DebugLog {
    private const val TAG = "Zmanim"
    fun d(msg: String) = Log.d(TAG, msg)
    fun e(msg: String, t: Throwable? = null) = Log.e(TAG, msg, t)
}
