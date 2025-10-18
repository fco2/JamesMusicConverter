package com.chuka.jamesmusicconverter.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global snackbar controller for showing messages from anywhere in the app
 */
@Singleton
class SnackbarController @Inject constructor() {

    private var snackbarHostState: SnackbarHostState? = null
    private var coroutineScope: CoroutineScope? = null

    fun initialize(hostState: SnackbarHostState, scope: CoroutineScope) {
        snackbarHostState = hostState
        coroutineScope = scope
    }

    fun showMessage(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        actionLabel: String? = null
    ) {
        coroutineScope?.launch {
            snackbarHostState?.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration
            )
        }
    }

    fun showSuccess(message: String) {
        showMessage(message, SnackbarDuration.Short)
    }

    fun showError(message: String) {
        showMessage(message, SnackbarDuration.Long)
    }
}
