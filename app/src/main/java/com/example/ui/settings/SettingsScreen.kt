package com.example.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.LockMinderDeviceAdminReceiver
import com.example.util.PermissionUtil
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Preferences
    val themeMode by viewModel.themeMode.collectAsState()
    val coolingOffMs by viewModel.coolingOffDurationMs.collectAsState()
    val isStrictMode by viewModel.isStrictMode.collectAsState()

    // Live permission states
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isUsageStatsGranted by remember { mutableStateOf(false) }
    var isOverlayGranted by remember { mutableStateOf(false) }
    var isDeviceAdminActive by remember { mutableStateOf(false) }
    var isBatteryOptimizationIgnored by remember { mutableStateOf(true) }
    var isNotificationGranted by remember { mutableStateOf(false) }

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            isNotificationGranted = isGranted
        }
    )

    // Backup & Restore Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.let { outputStream ->
                        viewModel.exportRulesToUri(outputStream) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to write file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.let { inputStream ->
                        viewModel.importRulesFromUri(inputStream) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Check permission states periodically when on Settings screen
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = PermissionUtil.isAccessibilityServiceEnabled(context)
            isUsageStatsGranted = PermissionUtil.isUsageStatsPermissionGranted(context)
            isOverlayGranted = PermissionUtil.isOverlayPermissionGranted(context)
            isDeviceAdminActive = PermissionUtil.isDeviceAdminActive(context)
            isNotificationGranted = PermissionUtil.isNotificationPermissionGranted(context)

            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            isBatteryOptimizationIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pm?.isIgnoringBatteryOptimizations(context.packageName) == true
            } else {
                true
            }

            delay(1000L)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings & Permissions", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // SECTION 1: System Permissions
            Text(
                text = "Special Access Permissions",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Accessibility card
            PermissionSettingCard(
                title = "Accessibility Shield Service",
                description = "Required to detect when a locked application is opened and display the lock overlay shield.",
                isGranted = isAccessibilityEnabled,
                onClickLaunch = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open Accessibility settings", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Usage Stats card
            PermissionSettingCard(
                title = "Usage Statistics Monitor",
                description = "Used as a backup mechanism to verify running apps if Accessibility is disabled.",
                isGranted = isUsageStatsGranted,
                onClickLaunch = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open Usage Access settings", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Overlay card
            PermissionSettingCard(
                title = "Draw Over Applications",
                description = "Required to block distracting apps by displaying a full-screen lockout interface overlay.",
                isGranted = isOverlayGranted,
                onClickLaunch = {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.startActivity(Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } else {
                            Toast.makeText(context, "Permission granted on this Android version", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        // Fallback overlay intent
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                        } catch (ex: Exception) {
                            Toast.makeText(context, "Cannot open Overlay settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            // Device Admin (Uninstall protection)
            PermissionSettingCard(
                title = "Device Administrator",
                description = "Prevents deliberate uninstalls of LockMinder while a lock shield is actively running.",
                isGranted = isDeviceAdminActive,
                onClickLaunch = {
                    try {
                        val dpmComponent = ComponentName(context, LockMinderDeviceAdminReceiver::class.java)
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, dpmComponent)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "LockMinder uses Device Admin to make uninstalling harder when rules are actively running.")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot request Device Admin", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Ignore Battery Optimizations
            PermissionSettingCard(
                title = "Ignore Battery Optimizations",
                description = "Allows the monitoring background service to run without being interrupted or halted by the Android system.",
                isGranted = isBatteryOptimizationIgnored,
                onClickLaunch = {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Optimization ignored on this Android version", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        } catch (ex: Exception) {
                            Toast.makeText(context, "Cannot open Battery settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            // Notifications card
            PermissionSettingCard(
                title = "System Notification Banner",
                description = "Required on Android 13+ to display the real-time active shield lock timer notification.",
                isGranted = isNotificationGranted,
                onClickLaunch = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Toast.makeText(context, "Permission automatically granted on this Android version", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SECTION 2: General Settings
            Text(
                text = "General Preferences",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Theme Setting Row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Application Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { option ->
                            val isSelected = themeMode == option.first
                            Button(
                                onClick = { viewModel.setTheme(option.first) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(option.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Cooling off duration Setting Row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cooling-Off Delay (For early unlock)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "How long to wait when you request an early unlock. Sets a countdown buffer to prevent impulse disabling.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "None" to 0L,
                            "5m" to (5 * 60 * 1000L),
                            "1h" to (60 * 60 * 1000L),
                            "24h" to (24 * 60 * 60 * 1000L)
                        ).forEach { option ->
                            val isSelected = coolingOffMs == option.second
                            Button(
                                onClick = { viewModel.setCoolingOffDuration(option.second) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(option.first, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Monotonic Mode (Clock Tamper Protection) Setting Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Monotonic Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Enforces on-device monotonic processor clocks. Disables calculating real-world time progression while off to ensure 100% immune to clock changes.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isStrictMode,
                        onCheckedChange = { viewModel.setStrictMode(it) },
                        modifier = Modifier.testTag("strict_mode_switch")
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // SECTION 3: Backup & Restore
            Text(
                text = "Backup & Restore",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Manage Commitment Rules", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Export active commitment shields as a JSON file, or restore previously exported files to move your settings between devices.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    exportLauncher.launch("lockminder_rules.json")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export init failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("export_rules_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    importLauncher.launch(arrayOf("application/json"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Import init failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("import_rules_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Import", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PermissionSettingCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClickLaunch: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isGranted) Color(0xFF00E676) else Color(0xFFFF4D4D),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onClickLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (isGranted) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isGranted) "Modify" else "Configure",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
