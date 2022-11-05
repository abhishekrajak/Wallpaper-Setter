package com.wallpaper.wallpapersetter.utils

import android.content.Context

object Utils {
    fun getWidth(context: Context?) = context?.resources?.displayMetrics?.widthPixels ?: 0
    fun getHeight(context: Context?) = context?.resources?.displayMetrics?.heightPixels ?: 0
}