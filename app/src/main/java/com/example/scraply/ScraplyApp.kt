package com.example.scraply

import android.app.Application
import com.example.scraply.data.ScraplyContainer

class ScraplyApp : Application() {
    lateinit var container: ScraplyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = ScraplyContainer(this)
    }

    companion object {
        lateinit var instance: ScraplyApp
            private set
    }
}
