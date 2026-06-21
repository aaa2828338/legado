package com.flux740.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import com.flux740.app.lib.theme.accentColor
import com.flux740.app.utils.applyTint

/**
 * @author Aidan Follestad (afollestad)
 */
class ThemeSeekBar(context: Context, attrs: AttributeSet) : AppCompatSeekBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
