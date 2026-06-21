package com.flux740.app.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.flux740.app.lib.theme.ThemeStore

/**
 * @author Aidan Follestad (afollestad)
 */
@Suppress("unused")
class PrimaryTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        setTextColor(ThemeStore.textColorPrimary(context))
    }
}
