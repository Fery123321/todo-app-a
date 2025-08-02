package com.example.todo_app.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository abstraction for TODOs to decouple ViewModel from Room.
 */
interface TodoRepository {
    fun observeTodos(): Flow<List<TodoEntity>>
    suspend fun getById(id: Long): TodoEntity?
    suspend fun add(title: String, description: String): Long
    suspend fun update(entity: TodoEntity)
    suspend fun delete(id: Long)
    suspend fun toggleComplete(id: Long, completed: Boolean)
}

/**
 * Room-backed implementation of [TodoRepository].
 */
class TodoRepositoryImpl(
    private val dao: TodoDao
) : TodoRepository {

    override fun observeTodos(): Flow<List<TodoEntity>> = dao.observeTodos()

    override suspend fun getById(id: Long): TodoEntity? = dao.getById(id)

    override suspend fun add(title: String, description: String): Long {
        val now = System.currentTimeMillis()
        val entity = TodoEntity(
            title = title.trim(),
            description = description.trim(),
            isCompleted = false,
            createdAt = now,
            updatedAt = now
        )
        return dao.insert(entity)
    }

    override suspend fun update(entity: TodoEntity) {
        dao.update(entity.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun toggleComplete(id: Long, completed: Boolean) {
        dao.setCompleted(id, completed, System.currentTimeMillis())
    }
}