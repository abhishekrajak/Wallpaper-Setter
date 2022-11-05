package com.wallpaper.common.component

import android.content.Context
import androidx.appcompat.app.AlertDialog

class AlertBox {

    companion object {

        private lateinit var alertDialog: AlertDialog

        fun showDialog(
            context: Context,
            header: String,
            message: String,
            positiveText: String? = null,
            negativeText: String? = null,
            neutralText: String? = null,
            positiveCallback: (() -> Unit)? = null,
            negativeCallback: (() -> Unit)? = null,
            neutralCallback: (() -> Unit)? = null
        ) {
            alertDialog = initialiseDialog(context)
            alertDialog.run {
                setTitle(header)
                setMessage(message)
                if (positiveText != null) setButton(AlertDialog.BUTTON_POSITIVE, positiveText){_, _ -> positiveCallback?.invoke()}
                if (negativeText != null) setButton(AlertDialog.BUTTON_NEGATIVE, negativeText){_, _ -> negativeCallback?.invoke()}
                if (neutralText != null) setButton(AlertDialog.BUTTON_NEUTRAL, neutralText){_, _ -> neutralCallback?.invoke()}
                show()
            }
        }

        private fun initialiseDialog(context: Context) : AlertDialog{
            return AlertDialog.Builder(context).create()
        }
    }
}