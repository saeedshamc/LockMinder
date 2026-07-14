package com.example.ui.addlock

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppInfo
import com.example.viewmodel.AppListState
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLockScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val installedAppsState by viewModel.installedAppsState.collectAsState()
    val activeRules by viewModel.activeRules.collectAsState()
    val historyRules by viewModel.lockHistory.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    
    // Duration
    var hoursInput by remember { mutableStateOf("1") }
    var minutesInput by remember { mutableStateOf("0") }
    
    var showWarningDialog by remember { mutableStateOf(false) }

    val isFirstLock = remember(activeRules, historyRules) {
        activeRules.isEmpty() && historyRules.isEmpty()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configure Lock Shield", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // STEP 1: App Selection or Selected App Summary
            if (selectedApp == null) {
                Text(
                    text = "1. Select App to Lock",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed applications...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("app_search_input")
                )

                // List of apps
                Box(modifier = Modifier.weight(1f)) {
                    when (val state = installedAppsState) {
                        is AppListState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is AppListState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is AppListState.Success -> {
                            val filteredApps = state.apps.filter {
                                it.appLabel.contains(searchQuery, ignoreCase = true) ||
                                it.packageName.contains(searchQuery, ignoreCase = true)
                            }

                            if (filteredApps.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No matching apps found", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredApps, key = { it.packageName }) { app ->
                                        AppPickerItem(
                                            app = app,
                                            isSelected = false,
                                            onSelect = { selectedApp = app }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // App is selected! Show App Header and duration fields
                val app = selectedApp!!
                
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val appIcon = remember(app.packageName) {
                            try {
                                context.packageManager.getApplicationIcon(app.packageName)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        val appIconBitmap = remember(appIcon) {
                            if (appIcon != null) {
                                val bmp = Bitmap.createBitmap(
                                    appIcon.intrinsicWidth.coerceAtLeast(1),
                                    appIcon.intrinsicHeight.coerceAtLeast(1),
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = android.graphics.Canvas(bmp)
                                appIcon.setBounds(0, 0, canvas.width, canvas.height)
                                appIcon.draw(canvas)
                                bmp.asImageBitmap()
                            } else {
                                null
                            }
                        }

                        if (appIconBitmap != null) {
                            Image(
                                bitmap = appIconBitmap,
                                contentDescription = "${app.appLabel} icon",
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White, CircleShape)
                                    .padding(4.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.appLabel.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appLabel, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(app.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }

                        TextButton(onClick = { selectedApp = null }) {
                            Text("Change")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // STEP 2: Duration Configuration
                Text(
                    text = "2. Set Lock Duration",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = { hoursInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hours_input")
                    )

                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { minutesInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("minutes_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Quick Grid
                Text(
                    text = "Quick Presets",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "15 Min" to (15 * 60 * 1000L),
                        "1 Hour" to (60 * 60 * 1000L),
                        "4 Hours" to (4 * 60 * 60 * 1000L),
                        "24 Hours" to (24 * 60 * 60 * 1000L)
                    ).forEach { preset ->
                        Button(
                            onClick = {
                                val hr = preset.second / (60 * 60 * 1000)
                                val min = (preset.second % (60 * 60 * 1000)) / (60 * 1000)
                                hoursInput = hr.toString()
                                minutesInput = min.toString()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(preset.first, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Confirm Shield Button
                Button(
                    onClick = {
                        val hrs = hoursInput.toLongOrNull() ?: 0L
                        val mins = minutesInput.toLongOrNull() ?: 0L
                        val totalMs = (hrs * 60 * 60 * 1000L) + (mins * 60 * 1000L)
                        
                        if (totalMs <= 0L) {
                            Toast.makeText(context, "Please select a duration greater than zero", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (isFirstLock) {
                            showWarningDialog = true
                        } else {
                            // Already locked apps before, standard flow
                            viewModel.createLockRule(app.packageName, app.appLabel, totalMs) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    onNavigateBack()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 12.dp)
                        .testTag("activate_shield_button")
                ) {
                    Icon(Icons.Outlined.Timer, contentDescription = "Lock")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activate Lock Shield", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // First-time warning modal
    if (showWarningDialog && selectedApp != null) {
        val app = selectedApp!!
        val hrs = hoursInput.toLongOrNull() ?: 0L
        val mins = minutesInput.toLongOrNull() ?: 0L
        val totalMs = (hrs * 60 * 60 * 1000L) + (mins * 60 * 1000L)

        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            icon = { Icon(Icons.Outlined.LockOpen, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Crucial Commitment Info", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Text(
                    text = "You are activating your first lock shield on ${app.appLabel}.\n\n" +
                           "Once activated, this app will be COMPLETELY locked. " +
                           "There are NO pathways, shortcuts, or options to unlock it or bypass the timer early.\n\n" +
                           "Even uninstalling LockMinder is disabled if you configure Device Admin. Do you understand and wish to commit knowingly?",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWarningDialog = false
                        viewModel.createLockRule(app.packageName, app.appLabel, totalMs) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                onNavigateBack()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("I Commit. Lock It!", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun AppPickerItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) {
            null
        }
    }
    val appIconBitmap = remember(appIcon) {
        if (appIcon != null) {
            val bmp = Bitmap.createBitmap(
                appIcon.intrinsicWidth.coerceAtLeast(1),
                appIcon.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bmp)
            appIcon.setBounds(0, 0, canvas.width, canvas.height)
            appIcon.draw(canvas)
            bmp.asImageBitmap()
        } else {
            null
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("app_picker_item_${app.packageName}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap,
                    contentDescription = "${app.appLabel} icon",
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .padding(4.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appLabel.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.appLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
