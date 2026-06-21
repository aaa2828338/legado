package com.flux740.app.help.source

import com.flux740.app.constant.SourceType
import com.flux740.app.data.entities.BaseSource
import com.flux740.app.data.entities.BookSource
import com.flux740.app.data.entities.RssSource
import com.flux740.app.model.SharedJsScope
import org.mozilla.javascript.Scriptable
import kotlin.coroutines.CoroutineContext

fun BaseSource.getShareScope(coroutineContext: CoroutineContext? = null): Scriptable? {
    return SharedJsScope.getScope(jsLib, coroutineContext)
}

fun BaseSource.getSourceType(): Int {
    return when (this) {
        is BookSource -> SourceType.book
        is RssSource -> SourceType.rss
        else -> error("unknown source type: ${this::class.simpleName}.")
    }
}
