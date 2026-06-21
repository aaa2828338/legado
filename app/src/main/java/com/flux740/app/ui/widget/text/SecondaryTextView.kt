package com.flux740.app.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.flux740.app.lib.theme.secondaryTextColor

/**
 * @author Aidan Follestad (afollestad)
 */
@Suppress("unused")
class SecondaryTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        setTextColor(context.secondaryTextColor)
    }
}
