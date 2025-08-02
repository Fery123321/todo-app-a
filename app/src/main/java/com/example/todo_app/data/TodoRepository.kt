package com.example.todo_app.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository contract that abstracts persistence and operations for Todo items.
 *
 * Purpose
 * - Decouple UI/ViewModel from the concrete data source (Room).
 * - Provide a reactive stream (Flow) for list observation and suspend APIs for CRUD.
 *
 * Notes
 * - The list stream is cold; Compose screens collect it in a lifecycle-aware manner.
 * - Validation (e.g., non-empty title) occurs in the ViewModel before calling add/update.
 *
 * Testing
 * - ViewModel tests can use a Fake implementation of this interface to avoid Room.
 */
interface TodoRepository {
    /**
     * Observe the full list of todos. Emits on insert/update/delete/flag changes.
     */
    fun observeTodos(): Flow<List<TodoEntity>>

    /**
     * Get a single todo by its id, or null if missing.
     */
    suspend fun getById(id: Long): TodoEntity?

    /**
     * Insert a new todo and return its generated id.
     * The repository will timestamp createdAt/updatedAt.
     */
    suspend fun add(title: String, description: String): Long

    /**
     * Update an existing todo. The repository updates updatedAt internally.
     */
    suspend fun update(entity: TodoEntity)

    /**
     * Delete a todo by id.
     */
    suspend fun delete(id: Long)

    /**
     * Set completion flag for a todo by id.
     */
    suspend fun toggleComplete(id: Long, completed: Boolean)
}

/**
 * Room-backed implementation of [TodoRepository].
 *
 * Implementation details
 * - Timestamps are managed here to keep DAO simple.
 * - Strings are trimmed on insert to avoid storing accidental whitespace.
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
        dao.update(
            entity.copy(
                title = entity.title.trim(),
                description = entity.description.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun toggleComplete(id: Long, completed: Boolean) {
        dao.setCompleted(id, completed, System.currentTimeMillis())
    }
}