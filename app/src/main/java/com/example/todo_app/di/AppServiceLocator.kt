package com.example.todo_app.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.todo_app.data.TodoDatabase
import com.example.todo_app.data.TodoRepository
import com.example.todo_app.data.TodoRepositoryImpl
import com.example.todo_app.ui.TodoViewModel

/**
 * Simple Application-scoped service locator to provide singletons.
 */
class AppServiceLocator private constructor(app: Application) {

    private val database by lazy { TodoDatabase.get(app) }
    val repository: TodoRepository by lazy { TodoRepositoryImpl(database.todoDao()) }

    companion object {
        @Volatile
        private var INSTANCE: AppServiceLocator? = null

        fun init(app: Application): AppServiceLocator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppServiceLocator(app).also { INSTANCE = it }
            }
        }

        fun get(): AppServiceLocator {
            return INSTANCE ?: error("AppServiceLocator not initialized. Call App.onCreate first.")
        }
    }
}

/**
 * Factory to create ViewModels with dependencies from ServiceLocator.
 */
class TodoViewModelFactory(
    private val locator: AppServiceLocator = AppServiceLocator.get()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            return TodoViewModel(locator.repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}