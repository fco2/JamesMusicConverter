package com.chuka.jamesmusicconverter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Custom snackbar with music icon
 */
@Composable
fun MusicSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.MusicNote
) {
    Snackbar(
        modifier = modifier.padding(12.dp),
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionContentColor = MaterialTheme.colorScheme.inversePrimary,
        action = if (snackbarData.visuals.actionLabel != null) {
            {
                TextButton(
                    onClick = { snackbarData.performAction() }
                ) {
                    Text(snackbarData.visuals.actionLabel!!)
                }
            }
        } else {
            null
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inversePrimary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
