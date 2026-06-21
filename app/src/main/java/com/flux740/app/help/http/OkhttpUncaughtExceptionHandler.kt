package com.flux740.app.help.http

import com.flux740.app.constant.AppLog

object OkhttpUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        AppLog.put("Okhttp Dispatcher中的线程执行出错\n${e.localizedMessage}", e)
    }

}
