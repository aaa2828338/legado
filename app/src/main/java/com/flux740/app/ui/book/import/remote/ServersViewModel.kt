package com.flux740.app.ui.book.import.remote

import android.app.Application
import com.flux740.app.base.BaseViewModel
import com.flux740.app.data.appDb
import com.flux740.app.data.entities.Server

class ServersViewModel(application: Application): BaseViewModel(application) {


    fun delete(server: Server) {
        execute {
            appDb.serverDao.delete(server)
        }
    }

}