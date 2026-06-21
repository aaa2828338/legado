package com.flux740.app.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.postDelayed
import com.flux740.app.base.BaseActivity
import com.flux740.app.constant.PreferKey
import com.flux740.app.constant.Theme
import com.flux740.app.data.appDb
import com.flux740.app.databinding.ActivityWelcomeBinding
import com.flux740.app.help.config.AppConfig
import com.flux740.app.help.config.ThemeConfig
import com.flux740.app.lib.theme.accentColor
import com.flux740.app.lib.theme.backgroundColor
import com.flux740.app.ui.book.read.ReadBookActivity
import com.flux740.app.ui.main.MainActivity
import com.flux740.app.utils.BitmapUtils
import com.flux740.app.utils.fullScreen
import com.flux740.app.utils.getPrefBoolean
import com.flux740.app.utils.getPrefInt
import com.flux740.app.utils.getPrefString
import com.flux740.app.utils.setStatusBarColorAuto
import com.flux740.app.utils.startActivity
import com.flux740.app.utils.viewbindingdelegate.viewBinding
import com.flux740.app.utils.visible
import com.flux740.app.utils.windowSize

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            // 避免从桌面启动程序后，会重新实例化入口类的activity
            finish()
        } else {
            val welcomeShowTime = getPrefInt(PreferKey.welcomeShowTime, 500)
            if (welcomeShowTime == 0) {
                startMainActivity()
            } else {
                binding.root.postDelayed(welcomeShowTime.toLong()) { startMainActivity() }
            }
        }
        binding.ivBook.setColorFilter(accentColor)
        binding.vwTitleLine.setBackgroundColor(accentColor)
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    override fun upBackgroundImage() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> {
                        getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                            if (path.endsWith(".9.png")) {
                                BitmapUtils.decodeNinePatchDrawable(path)?.let {
                                    window.decorView.background = it
                                }
                            } else {
                                val size = windowManager.windowSize
                                BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels)?.let {
                                    window.decorView.background = it.toDrawable(resources)
                                }
                            }
                        }
                        binding.tvLegado.visible(AppConfig.welcomeShowTextDark)
                        binding.ivBook.visible(AppConfig.welcomeShowIconDark)
                        binding.tvGzh.visible(AppConfig.welcomeShowTextDark)
                        return
                    }
                    else -> {
                        getPrefString(PreferKey.welcomeImage)?.let { path ->
                            if (path.endsWith(".9.png")) {
                                BitmapUtils.decodeNinePatchDrawable(path)?.let {
                                    window.decorView.background = it
                                }
                            } else {
                                val size = windowManager.windowSize
                                BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels)?.let {
                                    window.decorView.background = it.toDrawable(resources)
                                }
                            }
                        }
                        binding.tvLegado.visible(AppConfig.welcomeShowText)
                        binding.ivBook.visible(AppConfig.welcomeShowIcon)
                        binding.tvGzh.visible(AppConfig.welcomeShowText)
                        return
                    }
                }
            }
        }
        super.upBackgroundImage()
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead) && appDb.bookDao.lastReadBook != null) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()
class Launcher7 : WelcomeActivity()