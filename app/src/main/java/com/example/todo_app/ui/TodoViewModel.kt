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
 * UI contract/state for the Add/Edit screen.
 *
 * Fields
 * - id: null when creating a new item; set when editing an existing one.
 * - title/description: user-editable text inputs.
 * - isEditing: true when editing an existing item; false when adding.
 * - isSaving: transient flag to prevent duplicate actions while persisting.
 * - titleError: validation message for title (null when valid).
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
 *
 * These are consumed by the UI and are not part of persistent screen state.
 */
sealed interface UiEvent {
    /** Request to show a transient message (e.g., snackbar). */
    data class ShowMessage(val message: String) : UiEvent
    /** Request to navigate back to the previous screen. */
    data object NavigateBack : UiEvent
}

/**
 * MVVM ViewModel coordinating TODO feature UI state and actions.
 *
 * Responsibilities
 * - Expose a StateFlow list of todos for the list screen.
 * - Maintain Add/Edit screen state, validate input, and persist via repository.
 * - Emit one-off UI events for feedback and navigation.
 *
 * Coroutines/Flow
 * - Uses viewModelScope for structured concurrency.
 * - Collects repository.observeTodos() and exposes it as StateFlow with stateIn.
 */
class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    // List screen: stream of todos (hot StateFlow derived from repository Flow)
    val todos: StateFlow<List<TodoEntity>> =
        repository.observeTodos()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Add/Edit screen state
    private val _addEdit = MutableStateFlow(AddEditUiState())
    /** Public, immutable state for Add/Edit screen collected by Compose. */
    val addEdit: StateFlow<AddEditUiState> = _addEdit.asStateFlow()

    // One-off UI events
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    /** Stream of one-off events such as snackbars and navigation. */
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    /** Initialize state for creating a new item. */
    fun startAdd() {
        _addEdit.value = AddEditUiState()
    }

    /**
     * Load an existing item for editing.
     * Emits a message and requests navigation back if the item is not found.
     */
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

    /** Update title as user types; clears any existing title error. */
    fun onTitleChange(newTitle: String) {
        _addEdit.value = _addEdit.value.copy(title = newTitle, titleError = null)
    }

    /** Update description as user types. */
    fun onDescriptionChange(newDesc: String) {
        _addEdit.value = _addEdit.value.copy(description = newDesc)
    }

    /**
     * Validate and persist current Add/Edit state.
     * - Enforces non-empty trimmed title.
     * - Emits success/failure messages and navigates back on success.
     */
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

    /** Delete the given item by id; emits feedback via events. */
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

    /** Toggle completion state for the given item id; emits error message on failure. */
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