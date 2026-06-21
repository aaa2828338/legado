@file:Suppress("unused")

package com.flux740.app.help.book

import com.flux740.app.data.entities.BookChapter
import com.flux740.app.help.RuleBigDataHelp.getDanmakuFile

fun BookChapter.getDanmaku(): Any? { //读取弹幕数据
    return variableMap["danmaku"] ?: getDanmakuFile(bookUrl, url)
}