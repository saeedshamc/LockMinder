package com.example.ui.blockoverlay

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.LockRule
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lockedPackageName = intent.getStringExtra("LOCKED_PACKAGE_NAME") ?: ""
        
        setContent {
            ThemeManager.LockMinderTheme {
                BlockOverlayScreen(
                    packageName = lockedPackageName,
                    onGoHome = { goHome() }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Strict lockout policy: do nothing on back button press
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun BlockOverlayScreen(
    packageName: String,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    
    var rule by remember { mutableStateOf<LockRule?>(null) }
    var timeRemainingMs by remember { mutableStateOf(0L) }
    
    val appLabel = remember(rule) { rule?.appLabel ?: packageName.substringAfterLast('.') }
    
    val appIcon = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
    
    val appIconBitmap = remember(appIcon) {
        if (appIcon != null) {
            val bmp = Bitmap.createBitmap(appIcon.intrinsicWidth.coerceAtLeast(1), appIcon.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            appIcon.setBounds(0, 0, canvas.width, canvas.height)
            appIcon.draw(canvas)
            bmp.asImageBitmap()
        } else {
            null
        }
    }

    // Reactive subscription to target lock rule
    LaunchedEffect(packageName) {
        val activeRule = db.lockRuleDao().getActiveRuleForPackage(packageName)
        rule = activeRule
        if (activeRule != null) {
            timeRemainingMs = activeRule.remainingDurationMs
        }
    }

    // Live local ticking timer inside the block overlay UI
    LaunchedEffect(timeRemainingMs) {
        if (timeRemainingMs > 0) {
            delay(1000L)
            timeRemainingMs = (timeRemainingMs - 1000L).coerceAtLeast(0L)
            if (timeRemainingMs <= 0) {
                onGoHome()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.error, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Shield Locked",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "LockMinder Shield Activated",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Commitment Mode is holding this app locked. Bypasses are fully blocked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = "$appLabel Icon",
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White, CircleShape)
                                .padding(4.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = appLabel.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onSecondary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = appLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            val hours = TimeUnit.MILLISECONDS.toHours(timeRemainingMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemainingMs) % 60
            val countdownStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            
            Text(
                text = "REMAINING UNLOCK TIME",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = countdownStr,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
                    .testTag("return_home_button")
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Return to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
