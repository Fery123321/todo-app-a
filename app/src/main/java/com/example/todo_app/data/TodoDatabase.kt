package com.example.todo_app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the TODO app.
 * Version 1 with no migrations yet. Add Migration objects when schema evolves.
 */
@Database(
    entities = [TodoEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun get(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): TodoDatabase {
            return Room.databaseBuilder(
                appContext,
                TodoDatabase::class.java,
                "todo-db"
            )
                // .addMigrations(MIGRATION_1_2, ...)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}