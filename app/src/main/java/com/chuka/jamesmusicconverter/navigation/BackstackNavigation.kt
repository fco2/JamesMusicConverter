package com.chuka.jamesmusicconverter.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler

/**
 * Proper navigation backstack that follows NavKey patterns
 */
class NavBackStack(
    initialDestination: NavKey
) {
    private val _backstack = mutableStateListOf<NavKey>(initialDestination)
    val backstack: List<NavKey> get() = _backstack

    val currentDestination: NavKey get() = _backstack.last()

    val canGoBack: Boolean get() = _backstack.size > 1

    fun navigate(destination: NavKey) {
        _backstack.add(destination)
    }

    fun navigateUp(): Boolean {
        return if (canGoBack) {
            _backstack.removeAt(_backstack.lastIndex)
            true
        } else {
            false
        }
    }

    fun popTo(destination: NavKey): Boolean {
        val index = _backstack.lastIndexOf(destination)
        return if (index >= 0) {
            // Remove all items after the target destination
            repeat(_backstack.size - index - 1) {
                _backstack.removeAt(_backstack.lastIndex)
            }
            true
        } else {
            false
        }
    }

    fun popToRoot(): Boolean {
        return if (_backstack.size > 1) {
            val root = _backstack.first()
            _backstack.clear()
            _backstack.add(root)
            true
        } else {
            false
        }
    }

    fun replace(destination: NavKey) {
        if (_backstack.isNotEmpty()) {
            _backstack.removeAt(_backstack.lastIndex)
        }
        _backstack.add(destination)
    }

    fun clearAndNavigate(destination: NavKey) {
        _backstack.clear()
        _backstack.add(destination)
    }
}

/**
 * Proper rememberNavBackStack that follows navigation patterns
 */
@Composable
fun rememberNavBackStack(
    initialDestination: NavKey
): NavBackStack {
    return rememberSaveable(
        saver = NavBackStackSaver
    ) {
        NavBackStack(initialDestination)
    }
}

/**
 * Navigation display using proper NavKey backstack with back press handling and animations
 */
@Composable
fun NavDisplay(
    navBackStack: NavBackStack,
    content: @Composable (destination: NavKey, navBackStack: NavBackStack) -> Unit
) {
    // Handle system back button press
    BackHandler(enabled = navBackStack.canGoBack) {
        navBackStack.navigateUp()
    }

    // Track previous backstack size to determine navigation direction
    val previousSize = remember { mutableIntStateOf(navBackStack.backstack.size) }
    val isNavigatingForward = navBackStack.backstack.size > previousSize.intValue

    // Update previous size after determining direction
    LaunchedEffect(navBackStack.backstack.size) {
        previousSize.intValue = navBackStack.backstack.size
    }

    AnimatedContent(
        targetState = navBackStack.currentDestination,
        transitionSpec = {
            if (isNavigatingForward) {
                // Forward navigation: slide in from right
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
            } else {
                // Back navigation: slide in from left
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
            }
        },
        label = "NavDisplay"
    ) { destination ->
        content(destination, navBackStack)
    }
}

/**
 * Advanced NavDisplay with custom back press handling and animations
 */
@Composable
fun NavDisplay(
    navBackStack: NavBackStack,
    handleBackPress: Boolean = true,
    onBackPressed: (() -> Boolean)? = null,
    content: @Composable (destination: NavKey, navBackStack: NavBackStack) -> Unit
) {
    // Handle system back button press with customization
    if (handleBackPress) {
        BackHandler(enabled = navBackStack.canGoBack) {
            // Custom back press handler can intercept and return true if handled
            val handled = onBackPressed?.invoke() ?: false
            if (!handled) {
                navBackStack.navigateUp()
            }
        }
    }

    // Track previous backstack size to determine navigation direction
    val previousSize = remember { mutableIntStateOf(navBackStack.backstack.size) }
    val isNavigatingForward = navBackStack.backstack.size > previousSize.intValue

    // Update previous size after determining direction
    LaunchedEffect(navBackStack.backstack.size) {
        previousSize.intValue = navBackStack.backstack.size
    }

    AnimatedContent(
        targetState = navBackStack.currentDestination,
        transitionSpec = {
            if (isNavigatingForward) {
                // Forward navigation: slide in from right
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
            } else {
                // Back navigation: slide in from left
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
            }
        },
        label = "NavDisplay"
    ) { destination ->
        content(destination, navBackStack)
    }
}

/**
 * Saver for NavBackStack state preservation
 */
private val NavBackStackSaver = androidx.compose.runtime.saveable.Saver<NavBackStack, List<NavKey>>(
    save = { it.backstack },
    restore = { savedList ->
        NavBackStack(savedList.first()).apply {
            // Restore the full backstack
            savedList.drop(1).forEach { destination ->
                navigate(destination)
            }
        }
    }
)
