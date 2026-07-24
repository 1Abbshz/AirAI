package com.zen.airai

import android.app.Application
import com.zen.airai.data.db.AppDatabase
import com.zen.airai.data.preferences.PreferencesManager
import com.zen.airai.di.AppContainer

class AirAI : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
