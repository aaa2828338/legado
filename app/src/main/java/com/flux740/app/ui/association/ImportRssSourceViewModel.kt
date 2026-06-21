package com.flux740.app.ui.association

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import com.flux740.app.R
import com.flux740.app.base.BaseViewModel
import com.flux740.app.constant.AppConst
import com.flux740.app.constant.AppLog
import com.flux740.app.constant.AppPattern
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.RssSource
import com.flux740.app.exception.NoStackTraceException
import com.flux740.app.help.config.AppConfig
import com.flux740.app.help.http.decompressed
import com.flux740.app.help.http.newCallResponseBody
import com.flux740.app.help.http.okHttpClient
import com.flux740.app.help.source.SourceHelp
import com.flux740.app.model.RuleUpdate
import com.flux740.app.utils.GSON
import com.flux740.app.utils.fromJsonArray
import com.flux740.app.utils.fromJsonObject
import com.flux740.app.utils.isAbsUrl
import com.flux740.app.utils.isJsonArray
import com.flux740.app.utils.isJsonObject
import com.flux740.app.utils.isUri
import com.flux740.app.utils.jsonPath
import com.flux740.app.utils.readText
import com.flux740.app.utils.splitNotBlank
import splitties.init.appCtx

class ImportRssSourceViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<RssSource>()
    val checkSources = arrayListOf<RssSource?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val group = groupName?.trim()
            val keepName = AppConfig.importKeepName
            val keepGroup = AppConfig.importKeepGroup
            val keepEnable = AppConfig.importKeepEnable
            val selectSource = arrayListOf<RssSource>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val source = allSources[index]
                    checkSources[index]?.let {
                        if (keepName) {
                            source.sourceName = it.sourceName
                        }
                        if (keepGroup) {
                            source.sourceGroup = it.sourceGroup
                        }
                        if (keepEnable) {
                            source.enabled = it.enabled
                        }
                        source.customOrder = it.customOrder
                    }
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            source.sourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            source.sourceGroup = groups.joinToString(",")
                        } else {
                            source.sourceGroup = group
                        }
                    }
                    selectSource.add(source)
                }
            }
            SourceHelp.insertRssSource(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text)
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        val mText = text.trim()
        when {
            mText.isJsonObject() -> kotlin.runCatching {
                val json = JsonPath.parse(mText)
                val urls = json.read<List<String>>("$.sourceUrls")
                if (!urls.isNullOrEmpty()) {
                    urls.forEach {
                        importSourceUrl(it)
                    }
                }
            }.onFailure {
                GSON.fromJsonArray<RssSource>(mText).getOrThrow().let {
                    val source = it.firstOrNull() ?: return@let
                    if (source.sourceUrl.isEmpty()) {
                        throw NoStackTraceException("不是订阅源")
                    }
                    allSources.addAll(it)
                }
            }

            mText.isJsonArray() -> {
                GSON.fromJsonArray<RssSource>(mText).getOrThrow().let {
                    val source = it.firstOrNull() ?: return@let
                    if (source.sourceUrl.isEmpty()) {
                        throw NoStackTraceException("不是订阅源")
                    }
                    allSources.addAll(it)
                }
            }

            mText.isAbsUrl() -> {
                importSourceUrl(mText)
            }

            mText.isUri() -> {
                importSourceAwait(mText.toUri().readText(appCtx))
            }

            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        RuleUpdate.cacheRssSourceMap[url]?.also {
            allSources.addAll(it)
            RuleUpdate.cacheRssSourceMap.remove(url)
            return
        }
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.decompressed().byteStream().use { body ->
            val items: List<Map<String, Any>> = jsonPath.parse(body).read("$")
            for (item in items) {
                if (!item.containsKey("sourceUrl")) {
                    throw NoStackTraceException("不是订阅源")
                }
                val jsonItem = jsonPath.parse(item)
                GSON.fromJsonObject<RssSource>(jsonItem.jsonString()).getOrThrow().let { source ->
                    allSources.add(source)
                }
            }
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val has = appDb.rssSourceDao.getByKey(it.sourceUrl)
                checkSources.add(has)
                selectStatus.add(has == null || has.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}