package com.example.expressivenotes

import android.app.Application
import com.example.expressivenotes.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ExpressiveNotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin { androidContext(this@ExpressiveNotesApp); modules(AppModule) }
    }
}
