package com.flux740.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import com.flux740.app.lib.theme.accentColor
import com.flux740.app.utils.applyTint

class ThemeProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}