package com.flux740.app.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.flux740.app.databinding.ViewNavigationBadgeBinding
import com.flux740.app.help.config.AppConfig
import com.flux740.app.lib.theme.Selector
import com.flux740.app.lib.theme.ThemeStore
import com.flux740.app.lib.theme.bottomBackground
import com.flux740.app.lib.theme.getSecondaryTextColor
import com.flux740.app.lib.theme.transparentNavBar
import com.flux740.app.ui.widget.text.BadgeView
import com.flux740.app.utils.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import com.flux740.app.lib.theme.elevation

class ThemeBottomNavigationVIew(context: Context, attrs: AttributeSet) :
    BottomNavigationView(context, attrs) {

    init {
        val transparentNavBar = context.transparentNavBar
        val bgColor = context.bottomBackground
        if (transparentNavBar) {
            setBackgroundColor(Color.TRANSPARENT)
        } else {
            setBackgroundColor(bgColor)
            elevation = context.elevation
        }
        val textIsDark = ColorUtils.isColorLight(bgColor)
        val textColor = context.getSecondaryTextColor(textIsDark)
        val colorStateList = Selector.colorBuild()
            .setDefaultColor(textColor)
            .setSelectedColor(ThemeStore.accentColor(context))
            .create()
        itemIconTintList = colorStateList
        itemTextColor = colorStateList
        if (AppConfig.isEInkMode || transparentNavBar) {
            isItemHorizontalTranslationEnabled = false
            itemBackground = Color.TRANSPARENT.toDrawable()
        }

        ViewCompat.setOnApplyWindowInsetsListener(this, null)
    }

    fun addBadgeView(index: Int): BadgeView {
        //获取底部菜单view
        val menuView = getChildAt(0) as ViewGroup
        //获取第index个itemView
        val itemView = menuView.getChildAt(index) as ViewGroup
        val badgeBinding = ViewNavigationBadgeBinding.inflate(LayoutInflater.from(context))
        itemView.addView(badgeBinding.root)
        return badgeBinding.viewBadge
    }

}