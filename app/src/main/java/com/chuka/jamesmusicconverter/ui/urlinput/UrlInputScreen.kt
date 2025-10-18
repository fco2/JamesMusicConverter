package com.chuka.jamesmusicconverter.ui.urlinput

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.chuka.jamesmusicconverter.navigation.DownloadMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlInputScreen(
    onNavigateToProgress: (String, String?, String?, String?, DownloadMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UrlInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingAuthData by remember { mutableStateOf<Triple<String, AuthData?, DownloadMode>?>(null) }

    val clipboardManager = LocalClipboard.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Clear URL when screen is first shown (e.g., after completing a conversion)
    LaunchedEffect(Unit) {
        viewModel.clearUrl()
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted, proceed with conversion
            pendingAuthData?.let { (url, authData, downloadMode) ->
                onNavigateToProgress(url, authData?.username, authData?.password, authData?.browser, downloadMode)
                pendingAuthData = null
            }
        } else {
            // Permissions denied
            showPermissionDialog = true
        }
    }

    // Function to check and request permissions
    fun checkAndRequestPermissions(url: String, authData: AuthData?, downloadMode: DownloadMode) {
        val permissionsToRequest = mutableListOf<String>()

        // For Android 13+ (API 33+), need INTERNET only (INTERNET is not runtime permission)
        // For Android 10-12 (API 29-32), need READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: We're using app-specific directories, no permissions needed
            onNavigateToProgress(url, authData?.username, authData?.password, authData?.browser, downloadMode)
            return
        }

        // Android 10-12: Check READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isEmpty()) {
            // All permissions already granted
            onNavigateToProgress(url, authData?.username, authData?.password, authData?.browser, downloadMode)
        } else {
            // Request permissions
            pendingAuthData = Triple(url, authData, downloadMode)
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissions Required") },
            text = {
                Text(
                    "Storage permissions are needed to save converted audio files. " +
                            "Please grant the permissions in app settings."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "James Music Converter",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App Icon
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Title
            Text(
                text = when (uiState.downloadMode) {
                    DownloadMode.AUDIO -> "Convert Videos to MP3"
                    DownloadMode.VIDEO -> "Download Videos"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Description
            Text(
                text = when (uiState.downloadMode) {
                    DownloadMode.AUDIO -> "Enter a video URL to convert it to MP3 format"
                    DownloadMode.VIDEO -> "Enter a video URL to download it as MP4"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Download Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Mode Button
                FilterChip(
                    selected = uiState.downloadMode == DownloadMode.AUDIO,
                    onClick = {
                        if (uiState.downloadMode != DownloadMode.AUDIO) {
                            viewModel.updateDownloadMode(DownloadMode.AUDIO)
                        }
                    },
                    label = { Text("Audio (MP3)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Audio mode",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // Video Mode Button
                FilterChip(
                    selected = uiState.downloadMode == DownloadMode.VIDEO,
                    onClick = {
                        if (uiState.downloadMode != DownloadMode.VIDEO) {
                            viewModel.updateDownloadMode(DownloadMode.VIDEO)
                        }
                    },
                    label = { Text("Video (MP4)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Video mode",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // URL Input Field
            OutlinedTextField(
                value = uiState.urlTextFieldValue,
                onValueChange = viewModel::updateUrl,
                label = { Text("Video URL") },
                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.isError,
                supportingText = if (uiState.isError) {
                    { Text("Please enter a valid URL") }
                } else null,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                clipboardManager.getClipEntry()?.clipData?.let { clipData ->
                                    if (clipData.itemCount > 0) {
                                        val pastedText = clipData.getItemAt(0)?.text?.toString()
                                        if (pastedText != null) {
                                            viewModel.pasteFromClipboard(pastedText)
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste from clipboard"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                ),
                singleLine = true
            )

            // Advanced Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAdvancedOptions() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Advanced Options (for Vimeo, etc.)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (uiState.showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (uiState.showAdvancedOptions) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.showAdvancedOptions) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "For password-protected or login-required videos:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Username field
                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = viewModel::updateUsername,
                            label = { Text("Username (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Password field
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::updatePassword,
                            label = { Text("Password (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(
                                        imageVector = if (uiState.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (uiState.passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Or use cookies from your browser:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Browser cookies checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = uiState.useBrowserCookies,
                                onCheckedChange = viewModel::updateUseBrowserCookies
                            )
                            Text(
                                text = "Extract cookies from browser",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (uiState.useBrowserCookies) {
                            // Browser selection
                            OutlinedTextField(
                                value = uiState.selectedBrowser,
                                onValueChange = viewModel::updateSelectedBrowser,
                                label = { Text("Browser name") },
                                placeholder = { Text("chrome, firefox, edge, safari") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Convert/Download Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.validateAndGetAuthData()?.let { (url, authData, _) ->
                        checkAndRequestPermissions(url, authData, uiState.downloadMode)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = uiState.urlTextFieldValue.text.isNotBlank()
            ) {
                Text(
                    text = when (uiState.downloadMode) {
                        DownloadMode.AUDIO -> "Convert to MP3"
                        DownloadMode.VIDEO -> "Download Video"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Supported Platforms:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "• YouTube, Dailymotion\n• Instagram, TikTok\n• Twitter/X, Facebook\n• And 1000+ more platforms...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
