package com.example.rcarpremium

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object RcarDialogUi {
    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun panel(context: Context): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(Color.rgb(15, 17, 23))
        cornerRadius = dp(context, 22).toFloat()
        setStroke(dp(context, 1), Color.rgb(255, 122, 0))
    }

    fun field(context: Context): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(Color.rgb(21, 24, 31))
        cornerRadius = dp(context, 14).toFloat()
        setStroke(dp(context, 1), Color.rgb(58, 64, 74))
    }

    fun styleInput(editText: EditText, multiline: Boolean = false) {
        val c = editText.context
        editText.setTextColor(Color.WHITE)
        editText.setHintTextColor(Color.rgb(125, 132, 143))
        editText.textSize = 14f
        editText.background = field(c)
        editText.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12))
        if (!multiline) editText.setSingleLine(true)
    }

    fun styleSectionTitle(view: TextView) {
        view.setTextColor(Color.rgb(255, 122, 0))
        view.textSize = 12f
    }

    fun styleDialog(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.rgb(255, 122, 0))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.rgb(205, 205, 205))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(Color.rgb(205, 205, 205))
    }

    fun wrapPanel(context: Context, inner: LinearLayout): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8))
        addView(inner, LinearLayout.LayoutParams(-1, -2))
    }
}
