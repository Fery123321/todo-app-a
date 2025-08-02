package com.example.todo_app

import android.app.Application
import com.example.todo_app.di.AppServiceLocator

/**
 * Custom Application to initialize the ServiceLocator.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppServiceLocator.init(this)
    }
}