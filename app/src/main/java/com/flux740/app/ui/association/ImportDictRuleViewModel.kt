package com.flux740.app.ui.association

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.flux740.app.R
import com.flux740.app.base.BaseViewModel
import com.flux740.app.constant.AppConst
import com.flux740.app.constant.AppLog
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.DictRule
import com.flux740.app.exception.NoStackTraceException
import com.flux740.app.help.http.decompressed
import com.flux740.app.help.http.newCallResponseBody
import com.flux740.app.help.http.okHttpClient
import com.flux740.app.help.http.text
import com.flux740.app.utils.GSON
import com.flux740.app.utils.fromJsonArray
import com.flux740.app.utils.fromJsonObject
import com.flux740.app.utils.isAbsUrl
import com.flux740.app.utils.isJsonArray
import com.flux740.app.utils.isJsonObject
import com.flux740.app.utils.isUri
import com.flux740.app.utils.readText
import splitties.init.appCtx

class ImportDictRuleViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<DictRule>()
    val checkSources = arrayListOf<DictRule?>()
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
            val selectSource = arrayListOf<DictRule>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    selectSource.add(allSources[index])
                }
            }
            appDb.dictRuleDao.insert(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                GSON.fromJsonObject<DictRule>(text).getOrThrow().let {
                    allSources.add(it)
                }
            }

            text.isJsonArray() -> GSON.fromJsonArray<DictRule>(text).getOrThrow().let { items ->
                allSources.addAll(items)
            }

            text.isAbsUrl() -> {
                importSourceUrl(text)
            }

            text.isUri() -> {
                importSourceAwait(text.toUri().readText(appCtx))
            }

            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.decompressed().text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.dictRuleDao.getByName(it.name)
                checkSources.add(source)
                selectStatus.add(source == null)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}