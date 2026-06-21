package com.flux740.app.ui.browser

import android.app.Application
import android.content.Intent
import android.util.Base64
import android.webkit.URLUtil
import android.webkit.WebView
import com.flux740.app.base.BaseViewModel
import com.flux740.app.constant.AppConst
import com.flux740.app.constant.AppConst.imagePathKey
import com.flux740.app.constant.SourceType
import com.flux740.app.data.appDb
import com.flux740.app.exception.NoStackTraceException
import com.flux740.app.help.http.newCallResponseBody
import com.flux740.app.help.http.okHttpClient
import com.flux740.app.help.source.SourceHelp
import com.flux740.app.help.source.SourceVerificationHelp
import com.flux740.app.model.analyzeRule.AnalyzeUrl
import com.flux740.app.utils.ACache
import com.flux740.app.utils.FileDoc
import com.flux740.app.utils.createFileIfNotExist
import com.flux740.app.utils.openOutputStream
import com.flux740.app.utils.printOnDebug
import com.flux740.app.utils.toastOnUi
import org.apache.commons.text.StringEscapeUtils
import java.util.Date
import com.flux740.app.data.entities.BaseSource
import com.flux740.app.help.webView.WebJsExtensions.Companion.JS_INJECTION2

class WebViewModel(application: Application) : BaseViewModel(application) {
    var source: BaseSource? = null
    var intent: Intent? = null
    var baseUrl: String = ""
    var html: String? = null
    var localHtml: Boolean = false
    val headerMap: HashMap<String, String> = hashMapOf()
    var sourceVerificationEnable: Boolean = false
    var refetchAfterSuccess: Boolean = true
    var sourceName: String = ""
    var sourceOrigin: String = ""
    var sourceType = SourceType.book

    fun initData(
        intent: Intent,
        success: () -> Unit
    ) {
        execute {
            this@WebViewModel.intent = intent
            val url = intent.getStringExtra("url")
                ?: throw NoStackTraceException("url不能为空")
            sourceName = intent.getStringExtra("sourceName") ?: ""
            sourceOrigin = intent.getStringExtra("sourceOrigin") ?: ""
            sourceType = intent.getIntExtra("sourceType", SourceType.book)
            sourceVerificationEnable = intent.getBooleanExtra("sourceVerificationEnable", false)
            refetchAfterSuccess = intent.getBooleanExtra("refetchAfterSuccess", true)
            html = intent.getStringExtra("html")?.let{
                localHtml = true
                val headIndex = it.indexOf("<head", ignoreCase = true)
                if (headIndex >= 0) {
                    val closingHeadIndex = it.indexOf('>', startIndex = headIndex)
                    if (closingHeadIndex >= 0) {
                        val insertPos = closingHeadIndex + 1
                        StringBuilder(it).insert(insertPos, "<script>$JS_INJECTION2</script>").toString()
                    } else {
                        "<head><script>$JS_INJECTION2</script></head>$it"
                    }
                } else {
                    "<head><script>$JS_INJECTION2</script></head>$it"
                }
            }
            source = SourceHelp.getSource(sourceOrigin, sourceType)
            val analyzeUrl = AnalyzeUrl(url, source = source, coroutineContext = coroutineContext)
            baseUrl = analyzeUrl.url
            headerMap.putAll(analyzeUrl.headerMap)
            if (analyzeUrl.isPost()) {
                html = analyzeUrl.getStrResponseAwait(useWebView = false).body
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
    }

    fun saveImage(webPic: String?, path: String) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            webData2bitmap(webPic)?.let { byteArray ->
                val fileDoc = FileDoc.fromDir(path)
                val picFile = fileDoc.createFileIfNotExist(fileName)
                picFile.openOutputStream().getOrThrow().use {
                    it.write(byteArray)
                }
            } ?: throw Throwable("NULL")
        }.onError {
            ACache.get().remove(imagePathKey)
            context.toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            context.toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
            okHttpClient.newCallResponseBody {
                url(data)
            }.bytes()
        } else {
            Base64.decode(data.split(",").toTypedArray()[1], Base64.DEFAULT)
        }
    }

    fun saveVerificationResult(webView: WebView, success: () -> Unit) {
        if (!sourceVerificationEnable) {
            return success.invoke()
        }
        if (refetchAfterSuccess) {
            execute {
                val url = intent!!.getStringExtra("url")!!
                val source = appDb.bookSourceDao.getBookSource(sourceOrigin)
                if (html == null) {
                    html = AnalyzeUrl(
                        url,
                        headerMapF = headerMap,
                        source = source,
                        coroutineContext = coroutineContext
                    ).getStrResponseAwait(useWebView = false).body
                }
                SourceVerificationHelp.setResult(sourceOrigin, html ?: "", baseUrl)
            }.onSuccess {
                success.invoke()
            }
        } else {
            webView.evaluateJavascript("document.documentElement.outerHTML") {
                execute {
                    html = StringEscapeUtils.unescapeJson(it).trim('"')
                }.onSuccess {
                    SourceVerificationHelp.setResult(sourceOrigin, html ?: "",  webView.url ?: "")
                    success.invoke()
                }
            }
        }
    }

    fun disableSource(block: () -> Unit) {
        execute {
            SourceHelp.enableSource(sourceOrigin, sourceType, false)
        }.onSuccess {
            block.invoke()
        }
    }

    fun deleteSource(block: () -> Unit) {
        execute {
            SourceHelp.deleteSource(sourceOrigin, sourceType)
        }.onSuccess {
            block.invoke()
        }
    }

}