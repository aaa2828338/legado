package com.flux740.app.ui.book.import.remote

import android.app.Application
import com.flux740.app.base.BaseViewModel
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.Server
import com.flux740.app.utils.toastOnUi

class ServerConfigViewModel(application: Application): BaseViewModel(application) {

    var mServer: Server? = null

    fun init(id: Long?, onSuccess: () -> Unit) {
        //mServer不为空可能是旋转屏幕界面重新创建,不用更新数据
        if (mServer != null) return
        execute {
            mServer = if (id != null) {
                appDb.serverDao.get(id)
            } else {
                Server()
            }
        }.onSuccess {
            onSuccess.invoke()
        }
    }

    fun save(server: Server, onSuccess: () -> Unit) {
        execute {
            mServer?.let {
                appDb.serverDao.delete(it)
            }
            mServer = server
            appDb.serverDao.insert(server)
        }.onSuccess {
            onSuccess.invoke()
        }.onError {
            context.toastOnUi("保存出错\n${it.localizedMessage}")
        }
    }

}