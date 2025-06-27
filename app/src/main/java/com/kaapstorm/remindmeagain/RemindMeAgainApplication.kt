package com.kaapstorm.remindmeagain

import android.app.Application
import com.kaapstorm.remindmeagain.di.dataModule
import com.kaapstorm.remindmeagain.di.domainModule
import com.kaapstorm.remindmeagain.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RemindMeAgainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@RemindMeAgainApplication)
            modules(
                dataModule,
                domainModule,
                uiModule
            )
        }
    }
}