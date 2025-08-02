package com.example.todo_app

import com.example.todo_app.data.TodoEntity
import com.example.todo_app.data.TodoRepository
import com.example.todo_app.ui.TodoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeRepo : TodoRepository {
    private val items = mutableListOf<TodoEntity>()
    private val flow = MutableStateFlow<List<TodoEntity>>(emptyList())
    private var nextId = 1L

    override fun observeTodos(): Flow<List<TodoEntity>> = flow

    override suspend fun getById(id: Long): TodoEntity? = items.find { it.id == id }

    override suspend fun add(title: String, description: String): Long {
        val now = System.currentTimeMillis()
        val entity = TodoEntity(
            id = nextId++,
            title = title,
            description = description,
            isCompleted = false,
            createdAt = now,
            updatedAt = now
        )
        items.add(entity)
        flow.value = items.toList()
        return entity.id
    }

    override suspend fun update(entity: TodoEntity) {
        val idx = items.indexOfFirst { it.id == entity.id }
        if (idx >= 0) {
            items[idx] = entity.copy(updatedAt = System.currentTimeMillis())
            flow.value = items.toList()
        }
    }

    override suspend fun delete(id: Long) {
        items.removeAll { it.id == id }
        flow.value = items.toList()
    }

    override suspend fun toggleComplete(id: Long, completed: Boolean) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val e = items[idx]
            items[idx] = e.copy(isCompleted = completed, updatedAt = System.currentTimeMillis())
            flow.value = items.toList()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    @Test
    fun add_flow_updates_and_validation() = runTest {
        val repo = FakeRepo()
        val vm = TodoViewModel(repo)

        // Initially empty
        assertTrue(vm.todos.first().isEmpty())

        // Try saving with empty title -> should not add
        vm.startAdd()
        vm.save()
        assertTrue(vm.todos.first().isEmpty())

        // Provide title and save
        vm.onTitleChange("Task A")
        vm.onDescriptionChange("Desc A")
        vm.save()
        val list = vm.todos.first()
        assertEquals(1, list.size)
        assertEquals("Task A", list.first().title)
        assertEquals("Desc A", list.first().description)
    }

    @Test
    fun toggle_and_delete() = runTest {
        val repo = FakeRepo()
        val vm = TodoViewModel(repo)

        vm.startAdd()
        vm.onTitleChange("Task B")
        vm.save()
        val id = vm.todos.first().first().id

        // Toggle complete
        vm.toggleCompleted(id, true)
        assertTrue(vm.todos.first().first().isCompleted)

        // Delete
        vm.delete(id)
        assertTrue(vm.todos.first().isEmpty())
    }

    @Test
    fun edit_existing_item() = runTest {
        val repo = FakeRepo()
        val vm = TodoViewModel(repo)

        vm.startAdd()
        vm.onTitleChange("Old Title")
        vm.onDescriptionChange("Old Desc")
        vm.save()
        val existing = vm.todos.first().first()

        // Enter edit mode and update
        vm.startEdit(existing.id)
        vm.onTitleChange("New Title")
        vm.onDescriptionChange("New Desc")
        vm.save()

        val updated = vm.todos.first().first()
        assertEquals("New Title", updated.title)
        assertEquals("New Desc", updated.description)
    }
}