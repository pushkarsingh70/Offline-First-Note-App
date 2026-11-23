package com.offlinefirst.notes

import android.app.Application
import com.offlinefirst.notes.di.AppContainer
import com.offlinefirst.notes.di.DefaultAppContainer

class NotesApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
