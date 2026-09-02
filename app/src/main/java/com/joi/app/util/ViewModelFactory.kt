package com.joi.app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Every screen's ViewModel takes plain use-cases in its constructor (manual DI, see
 * AppContainer) — this is the one-liner that turns "here's how to construct it" into the
 * `ViewModelProvider.Factory` Compose's `viewModel()` wants, without a DI framework.
 */
inline fun <reified VM : ViewModel> viewModelFactoryOf(crossinline create: () -> VM): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { create() }
    }
