package com.flux740.app.model

import android.content.Context
import com.flux740.app.constant.IntentAction
import com.flux740.app.service.DownloadService
import com.flux740.app.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}