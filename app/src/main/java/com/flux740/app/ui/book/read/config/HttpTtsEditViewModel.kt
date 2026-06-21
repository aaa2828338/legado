package com.flux740.app.ui.book.read.config

import android.app.Application
import android.os.Bundle
import com.flux740.app.base.BaseViewModel
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.HttpTTS
import com.flux740.app.exception.NoStackTraceException
import com.flux740.app.help.ConcurrentRateLimiter.Companion.concurrentRecordMap
import com.flux740.app.model.ReadAloud
import com.flux740.app.utils.getClipText
import com.flux740.app.utils.isJsonArray
import com.flux740.app.utils.isJsonObject
import com.flux740.app.utils.toastOnUi

class HttpTtsEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null
    var httpTTS: HttpTTS? = null

    fun initData(arguments: Bundle?, success: (httpTTS: HttpTTS) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    val source = appDb.httpTTSDao.get(argumentId)
                    httpTTS = source
                    return@execute source
                }
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(httpTTS: HttpTTS, success: (() -> Unit)? = null) {
        id = httpTTS.id
        this.httpTTS = httpTTS
        execute {
            appDb.httpTTSDao.insert(httpTTS)
            concurrentRecordMap.remove(httpTTS.getKey()) //删除并发限制缓存
            if (ReadAloud.ttsEngine == httpTTS.id.toString()) ReadAloud.upReadAloudClass()
        }.onSuccess {
            success?.invoke()
        }
    }

    fun importFromClip(onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text = context.getClipText()
        if (text.isNullOrBlank()) {
            context.toastOnUi("剪贴板为空")
        } else {
            importSource(text, onSuccess)
        }
    }

    fun importSource(text: String, onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text1 = text.trim()
        execute {
            when {
                text1.isJsonObject() -> {
                    HttpTTS.fromJson(text1).getOrThrow()
                }
                text1.isJsonArray() -> {
                    HttpTTS.fromJsonArray(text1).getOrThrow().first()
                }
                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onSuccess {
            onSuccess.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

}