package com.zimapp.zim

import android.app.Application
import com.zimapp.zim.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ExpressiveNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { androidContext(this@ExpressiveNotesApp); modules(AppModule) }
    }
}
