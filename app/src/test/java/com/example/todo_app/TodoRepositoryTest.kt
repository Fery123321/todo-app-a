package com.example.todo_app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.todo_app.data.TodoDao
import com.example.todo_app.data.TodoDatabase
import com.example.todo_app.data.TodoRepository
import com.example.todo_app.data.TodoRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoRepositoryTest {

    private lateinit var db: TodoDatabase
    private lateinit var dao: TodoDao
    private lateinit var repo: TodoRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TodoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.todoDao()
        repo = TodoRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun add_and_list_items() = runTest {
        assertTrue(repo.observeTodos().first().isEmpty())
        val id = repo.add("Title 1", "Desc 1")
        assertTrue(id > 0)
        val list = repo.observeTodos().first()
        assertEquals(1, list.size)
        assertEquals("Title 1", list[0].title)
    }

    @Test
    fun toggle_complete_and_update() = runTest {
        val id = repo.add("Task", "Desc")
        repo.toggleComplete(id, true)
        var item = repo.getById(id)
        assertNotNull(item)
        assertTrue(item!!.isCompleted)

        val updated = item.copy(title = "Task Updated")
        repo.update(updated)
        item = repo.getById(id)
        assertEquals("Task Updated", item!!.title)
        assertTrue(item.isCompleted)
    }

    @Test
    fun delete_item() = runTest {
        val id = repo.add("Task", "Desc")
        assertNotNull(repo.getById(id))
        repo.delete(id)
        val after = repo.getById(id)
        assertEquals(null, after)
        assertFalse(repo.observeTodos().first().any { it.id == id })
    }
}