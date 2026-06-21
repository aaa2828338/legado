package com.flux740.app.ui.association

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.flux740.app.base.BaseViewModel
import com.flux740.app.constant.AppConst
import com.flux740.app.constant.AppLog
import com.flux740.app.constant.AppPattern
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.ReplaceRule
import com.flux740.app.exception.NoStackTraceException
import com.flux740.app.help.ReplaceAnalyzer
import com.flux740.app.help.http.decompressed
import com.flux740.app.help.http.newCallResponseBody
import com.flux740.app.help.http.okHttpClient
import com.flux740.app.help.http.text
import com.flux740.app.model.RuleUpdate
import com.flux740.app.utils.isAbsUrl
import com.flux740.app.utils.isJsonArray
import com.flux740.app.utils.isJsonObject
import com.flux740.app.utils.isUri
import com.flux740.app.utils.readText
import com.flux740.app.utils.splitNotBlank
import splitties.init.appCtx

class ImportReplaceRuleViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allRules = arrayListOf<ReplaceRule>()
    val checkRules = arrayListOf<ReplaceRule?>()
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
            val selectRules = arrayListOf<ReplaceRule>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val rule = allRules[index]
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            rule.group?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            rule.group = groups.joinToString(",")
                        } else {
                            rule.group = group
                        }
                    }
                    selectRules.add(rule)
                }
            }
            appDb.replaceRuleDao.insert(*selectRules.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun import(text: String) {
        execute {
            importAwait(text.trim())
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importAwait(text: String) {
        when {
            text.isAbsUrl() -> importUrl(text)
            text.isJsonArray() -> {
                val rules = ReplaceAnalyzer.jsonToReplaceRules(text).getOrThrow()
                allRules.addAll(rules)
            }

            text.isJsonObject() -> {
                val rule = ReplaceAnalyzer.jsonToReplaceRule(text).getOrThrow()
                allRules.add(rule)
            }

            text.isUri() -> {
                importAwait(text.toUri().readText(appCtx))
            }

            else -> throw NoStackTraceException("格式不对")
        }
    }

    private suspend fun importUrl(url: String) {
        RuleUpdate.cacheReplaceRuleMap[url]?.also {
            allRules.addAll(it)
            RuleUpdate.cacheReplaceRuleMap.remove(url)
            return
        }
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.decompressed().text("utf-8").let {
            importAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allRules.forEach {
                val rule = appDb.replaceRuleDao.findById(it.id)
                checkRules.add(rule)
                selectStatus.add(rule == null)
            }
        }.onSuccess {
            successLiveData.postValue(allRules.size)
        }
    }
}