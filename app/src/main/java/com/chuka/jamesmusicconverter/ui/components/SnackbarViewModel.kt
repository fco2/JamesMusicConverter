package com.chuka.jamesmusicconverter.ui.components

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel that provides access to the singleton SnackbarController
 */
@HiltViewModel
class SnackbarViewModel @Inject constructor(
    val snackbarController: SnackbarController
) : ViewModel()
