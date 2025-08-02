package com.example.todo_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.todo_app.di.TodoViewModelFactory
import com.example.todo_app.ui.TodoNavGraph
import com.example.todo_app.ui.TodoViewModel
import com.example.todo_app.ui.theme.TodoappTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TodoViewModel by viewModels { TodoViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoappTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    TodoNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}