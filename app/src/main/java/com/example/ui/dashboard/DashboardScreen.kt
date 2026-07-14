package com.example.ui.dashboard

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LockHistory
import com.example.data.LockRule
import com.example.util.PermissionUtil
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAddLock: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val activeRules by viewModel.activeRules.collectAsState()
    val historyRules by viewModel.lockHistory.collectAsState()

    val chartData = remember(historyRules) {
        val dailyHours = mutableMapOf<String, Double>()
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        val daysList = mutableListOf<String>()
        
        for (i in 29 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dayStr = sdf.format(c.time)
            daysList.add(dayStr)
            dailyHours[dayStr] = 0.0
        }
        
        for (history in historyRules) {
            val lockedDateStr = sdf.format(Date(history.lockedAt))
            if (dailyHours.containsKey(lockedDateStr)) {
                val hours = history.durationMs / (1000.0 * 60.0 * 60.0)
                dailyHours[lockedDateStr] = (dailyHours[lockedDateStr] ?: 0.0) + hours
            }
        }
        
        daysList.map { day -> day to (dailyHours[day] ?: 0.0) }
    }

    var showPermissionsWarning by remember { mutableStateOf(false) }

    // Check permissions periodically when screen is visible
    LaunchedEffect(Unit) {
        while (true) {
            showPermissionsWarning = !PermissionUtil.hasAllPermissions(context)
            delay(2000L)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "LockMinder Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddLock,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Lock") },
                text = { Text("Lock Distraction") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_lock_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permissions warning banner
            if (showPermissionsWarning) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("permissions_warning_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permissions Required",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Accessibility, overlay, and usage permissions are required to block distracting apps.",
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Fix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Focus Analytics Chart
            item {
                FocusAnalyticsChart(data = chartData)
            }

            // Dashboard Title / Stats
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Your Commitment Shields",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "These apps are locked and cannot be bypassed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Empty state illustration
            if (activeRules.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_locks),
                                contentDescription = "No locked apps",
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Zero Distractions Locked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Awesome job! You currently have full access to all applications. Use the '+' button to lock distractions when you need to focus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            } else {
                items(activeRules, key = { it.id }) { rule ->
                    ActiveLockItem(
                        rule = rule,
                        onRequestDelete = { viewModel.requestDeleteRule(rule) { _, _ -> } },
                        onCancelDelete = { viewModel.cancelPendingDeletion(rule) }
                    )
                }
            }

            // History Header
            if (historyRules.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Completed Focus Shifts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                items(historyRules.take(8)) { history ->
                    HistoryItem(history = history)
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ActiveLockItem(
    rule: LockRule,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit
) {
    val context = LocalContext.current
    var tickingRemainingMs by remember(rule.remainingDurationMs) { mutableStateOf(rule.remainingDurationMs) }

    // local ticks in the layout
    LaunchedEffect(rule.id, tickingRemainingMs) {
        if (tickingRemainingMs > 0) {
            delay(1000L)
            tickingRemainingMs = (tickingRemainingMs - 1000L).coerceAtLeast(0L)
        }
    }

    val appLabel = rule.appLabel
    val appIcon = remember(rule.packageName) {
        try {
            context.packageManager.getApplicationIcon(rule.packageName)
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

    val hours = TimeUnit.MILLISECONDS.toHours(tickingRemainingMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(tickingRemainingMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(tickingRemainingMs) % 60
    val countdownStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_lock_item_${rule.packageName}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = "$appLabel Icon",
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, CircleShape)
                            .padding(4.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appLabel.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rule.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = countdownStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "REMAINING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            // Pending Deletion (Cooling-off info)
            rule.pendingDeleteTimestamp?.let { timestamp ->
                val remainingDeleteMs = (timestamp - System.currentTimeMillis()).coerceAtLeast(0L)
                if (remainingDeleteMs > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LockReset,
                                contentDescription = "Pending Unlock",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val dHours = TimeUnit.MILLISECONDS.toHours(remainingDeleteMs)
                                val dMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingDeleteMs) % 60
                                val delayStr = if (dHours > 0) "$dHours hr $dMinutes min" else "$dMinutes min"
                                Text(
                                    text = "Cooling-Off Period Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Will unlock in $delayStr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            TextButton(
                                onClick = onCancelDelete,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delete request action
            if (rule.pendingDeleteTimestamp == null) {
                OutlinedButton(
                    onClick = onRequestDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Unlock", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlock Early (Starts Cooling-Off)", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun HistoryItem(history: LockHistory) {
    val dateStr = remember(history.lockedAt) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(history.lockedAt))
    }

    val minutesLocked = TimeUnit.MILLISECONDS.toMinutes(history.durationMs)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.appLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$dateStr • Locked for $minutesLockedm",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "${minutesLocked}m",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Fix potential typo in string concat for minutesLocked
private val minutesLockedm = "min"

@Composable
fun FocusAnalyticsChart(
    data: List<Pair<String, Double>>
) {
    val totalHours = data.sumOf { it.second }
    val maxHours = (data.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_analytics_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Focus Shield Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total commitment hours over the last 30 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.1f hrs", totalHours),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TOTAL FOCUS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // The Scrollable Bar Chart
            val scrollState = rememberScrollState()
            
            // Set scroll state to end so the user sees the most recent days first!
            LaunchedEffect(data) {
                scrollState.scrollTo(scrollState.maxValue)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Background grid lines (e.g. 0%, 50%, 100%)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val label1 = String.format("%.1f h", maxHours)
                    val label2 = String.format("%.1f h", maxHours / 2)
                    listOf(label1, label2, "0.0 h").forEachIndexed { index, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.width(36.dp)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Bars Row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 38.dp, bottom = 4.dp)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEach { item ->
                        val dayLabel = item.first
                        val hours = item.second
                        val barHeightFactor = (hours / maxHours).toFloat().coerceIn(0.01f, 1f)
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(32.dp)
                        ) {
                            if (hours > 0) {
                                Text(
                                    text = String.format("%.1f", hours),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.7f * barHeightFactor)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (hours > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = dayLabel,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
