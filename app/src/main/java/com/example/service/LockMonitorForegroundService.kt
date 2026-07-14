package com.example.service

import android.app.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.data.LockHistory
import com.example.data.LockRule
import com.example.data.SettingsManager
import com.example.ui.blockoverlay.BlockOverlayActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class LockMonitorForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private lateinit var database: AppDatabase
    private lateinit var settingsManager: SettingsManager
    
    private var activeRules = listOf<LockRule>()
    private var isStrictMode = false
    private var monitorJob: Job? = null

    companion object {
        const val CHANNEL_ID = "lockminder_monitor_channel"
        const val NOTIFICATION_ID = 4125
        
        fun startService(context: Context) {
            val intent = Intent(context, LockMonitorForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LockMonitorForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("LockMonitorService", "Service created")
        database = AppDatabase.getDatabase(applicationContext)
        settingsManager = SettingsManager(applicationContext)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Active monitoring enabled"))
        
        // Align active rule timers on service launch
        serviceScope.launch {
            alignTimersOnStart()
            startMonitoringLoop()
        }
    }

    private suspend fun alignTimersOnStart() {
        val rules = database.lockRuleDao().getActiveRules()
        val nowRealtime = SystemClock.elapsedRealtime()
        val nowSystem = System.currentTimeMillis()
        
        rules.forEach { rule ->
            val updated = rule.copy(
                lastActiveRealtime = nowRealtime,
                lastActiveSystemTime = nowSystem
            )
            database.lockRuleDao().insertRule(updated)
        }
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Fetch latest settings
                    isStrictMode = settingsManager.strictModeFlow.first()
                    
                    // Fetch rules
                    val rules = database.lockRuleDao().getActiveRules()
                    activeRules = rules
                    
                    if (rules.isEmpty()) {
                        updateNotification("No active locks")
                    } else {
                        val nowRealtime = SystemClock.elapsedRealtime()
                        val nowSystem = System.currentTimeMillis()
                        
                        val updatedRules = mutableListOf<LockRule>()
                        
                        rules.forEach { rule ->
                            val systemDelta = nowSystem - rule.lastActiveSystemTime
                            val realtimeDelta = nowRealtime - rule.lastActiveRealtime
                            
                            val deltaToUse = if (isStrictMode) {
                                if (realtimeDelta >= 0) realtimeDelta else 0L
                            } else {
                                if (realtimeDelta >= 0 && systemDelta >= 0) {
                                    if (systemDelta > realtimeDelta) systemDelta else realtimeDelta
                                } else if (realtimeDelta < 0) {
                                    if (systemDelta > 0) systemDelta else 0L
                                } else {
                                    if (realtimeDelta >= 0) realtimeDelta else 0L
                                }
                            }
                            
                            val newRemaining = (rule.remainingDurationMs - deltaToUse).coerceAtLeast(0L)
                            
                            if (newRemaining <= 0) {
                                // Rule expired! Update state and log history
                                val expiredRule = rule.copy(
                                    remainingDurationMs = 0,
                                    isActive = false,
                                    lastActiveRealtime = nowRealtime,
                                    lastActiveSystemTime = nowSystem
                                )
                                database.lockRuleDao().updateRule(expiredRule)
                                
                                // Insert to history
                                database.lockRuleDao().insertHistory(
                                    LockHistory(
                                        packageName = rule.packageName,
                                        appLabel = rule.appLabel,
                                        durationMs = rule.totalDurationMs,
                                        lockedAt = rule.createdAt,
                                        unlockedAt = System.currentTimeMillis(),
                                        wasCompleted = true
                                    )
                                )
                                Log.d("LockMonitorService", "Rule expired for ${rule.appLabel}")
                            } else {
                                val updatedRule = rule.copy(
                                    remainingDurationMs = newRemaining,
                                    lastActiveRealtime = nowRealtime,
                                    lastActiveSystemTime = nowSystem
                                )
                                database.lockRuleDao().updateRule(updatedRule)
                                updatedRules.add(updatedRule)
                            }
                        }
                        
                        activeRules = updatedRules
                        
                        // Update status notification
                        if (updatedRules.isNotEmpty()) {
                            val statusText = updatedRules.joinToString(", ") { rule ->
                                val hours = TimeUnit.MILLISECONDS.toHours(rule.remainingDurationMs)
                                val minutes = TimeUnit.MILLISECONDS.toMinutes(rule.remainingDurationMs) % 60
                                val seconds = TimeUnit.MILLISECONDS.toSeconds(rule.remainingDurationMs) % 60
                                "${rule.appLabel} (${String.format("%02d:%02d:%02d", hours, minutes, seconds)})"
                            }
                            updateNotification("Locked: $statusText")
                        } else {
                            updateNotification("No active locks")
                        }
                    }
                    
                    // Backup locking check via UsageStatsManager
                    checkTopPackageWithUsageStats()
                    
                } catch (e: Exception) {
                    Log.e("LockMonitorService", "Error in monitor loop", e)
                }
                
                delay(1000L) // Precision updates every 1 second
            }
        }
    }

    private fun checkTopPackageWithUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)
        if (!stats.isNullOrEmpty()) {
            var topActivity: UsageStats? = null
            for (stat in stats) {
                if (topActivity == null || stat.lastTimeUsed > topActivity.lastTimeUsed) {
                    topActivity = stat
                }
            }
            val topPackage = topActivity?.packageName
            if (topPackage != null && topPackage != packageName) {
                val isLocked = activeRules.any { it.packageName == topPackage }
                if (isLocked) {
                    Log.d("LockMonitorService", "Backup blocker: $topPackage is active. Opening overlay.")
                    val intent = Intent(this, BlockOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("LOCKED_PACKAGE_NAME", topPackage)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "LockMinder Active Lock Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps LockMinder background locks active and prevents bypasses"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        // PendingIntent to launch MainActivity
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LockMinder Shield Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LockMonitorService", "Service started command")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LockMonitorService", "Service destroyed")
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
