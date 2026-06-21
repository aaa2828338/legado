package com.flux740.app.ui.main.explore

import android.app.Application
import com.flux740.app.base.BaseViewModel
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.BookSourcePart
import com.flux740.app.help.config.SourceConfig
import com.flux740.app.help.source.SourceHelp

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(bookSource)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            SourceHelp.deleteBookSource(source.bookSourceUrl)
        }
    }

}