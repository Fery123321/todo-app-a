package com.example.todo_app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Centralized NavGraph to keep MainActivity clean. Uses a single ViewModel shared across destinations.
 */
@Composable
fun TodoNavGraph(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.List.route,
        modifier = modifier
    ) {
        composable(Routes.List.route) {
            TodoListScreen(
                viewModel = viewModel,
                onAddClick = {
                    viewModel.startAdd()
                    navController.navigate(Routes.EditAdd.route)
                },
                onEditClick = { id ->
                    viewModel.startEdit(id)
                    navController.navigate(Routes.EditAdd.withId(id))
                }
            )
        }
        // Make optional id a String to truly allow null; parse to Long? inside
        composable(
            route = Routes.EditAdd.routeWithOptionalId,
            arguments = listOf(
                androidx.navigation.navArgument(Routes.EditAdd.argId) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val idParam: String? = backStackEntry.arguments?.getString(Routes.EditAdd.argId)
            val id: Long? = idParam?.toLongOrNull()
            if (id != null) {
                viewModel.startEdit(id)
            } else {
                viewModel.startAdd()
            }
            AddEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                isEditing = id != null
            )
        }
    }
}

object Routes {
    object List {
        const val route = "list"
    }
    object EditAdd {
        const val route = "editAdd"
        const val argId = "id"
        // optional id as String (nullable)
        const val routeWithOptionalId = "$route?$argId={$argId}"
        fun withId(id: Long) = "$route?$argId=$id"
    }
}