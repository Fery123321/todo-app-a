package com.example.todo_app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo_app.data.TodoEntity
import com.example.todo_app.data.TodoRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI contract/state for add/edit screen.
 */
data class AddEditUiState(
    val id: Long? = null,
    val title: String = "",
    val description: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val titleError: String? = null
)

/**
 * One-off UI events for snackbars/navigation.
 */
sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data object NavigateBack : UiEvent
}

/**
 * ViewModel for the TODO app.
 * - Exposes a flow of todo list from repository
 * - Handles add, edit, delete, toggle complete
 */
class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    // List screen: stream of todos
    val todos: StateFlow<List<TodoEntity>> =
        repository.observeTodos()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Add/Edit screen state
    private val _addEdit = MutableStateFlow(AddEditUiState())
    val addEdit: StateFlow<AddEditUiState> = _addEdit.asStateFlow()

    // One-off UI events
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    fun startAdd() {
        _addEdit.value = AddEditUiState()
    }

    fun startEdit(id: Long) {
        viewModelScope.launch {
            val entity = repository.getById(id)
            if (entity == null) {
                _events.send(UiEvent.ShowMessage("Item not found"))
                _events.send(UiEvent.NavigateBack)
            } else {
                _addEdit.value = AddEditUiState(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    isEditing = true
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _addEdit.value = _addEdit.value.copy(title = newTitle, titleError = null)
    }

    fun onDescriptionChange(newDesc: String) {
        _addEdit.value = _addEdit.value.copy(description = newDesc)
    }

    fun save() {
        val current = _addEdit.value
        val title = current.title.trim()
        if (title.isEmpty()) {
            _addEdit.value = current.copy(titleError = "Title cannot be empty")
            return
        }
        _addEdit.value = current.copy(isSaving = true)
        viewModelScope.launch {
            try {
                if (current.isEditing && current.id != null) {
                    val updated = TodoEntity(
                        id = current.id,
                        title = title,
                        description = current.description.trim(),
                        isCompleted = todos.value.firstOrNull { it.id == current.id }?.isCompleted
                            ?: false,
                        createdAt = todos.value.firstOrNull { it.id == current.id }?.createdAt
                            ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.update(updated)
                    _events.send(UiEvent.ShowMessage("Task updated"))
                } else {
                    repository.add(title, current.description)
                    _events.send(UiEvent.ShowMessage("Task added"))
                }
                _events.send(UiEvent.NavigateBack)
            } catch (t: Throwable) {
                _events.send(UiEvent.ShowMessage("Failed to save: ${t.message}"))
            } finally {
                _addEdit.value = _addEdit.value.copy(isSaving = false)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                _events.send(UiEvent.ShowMessage("Task deleted"))
            } catch (t: Throwable) {
                _events.send(UiEvent.ShowMessage("Failed to delete: ${t.message}"))
            }
        }
    }

    fun toggleCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleComplete(id, completed)
            } catch (t: Throwable) {
                _events.send(UiEvent.ShowMessage("Failed to update: ${t.message}"))
            }
        }
    }
}