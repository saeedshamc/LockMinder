package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.LockMonitorForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class AppInfo(
    val packageName: String,
    val appLabel: String
)

sealed interface AppListState {
    object Loading : AppListState
    data class Success(val apps: List<AppInfo>) : AppListState
    data class Error(val message: String) : AppListState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = LockRepository(database.lockRuleDao())
    private val settingsManager = SettingsManager(context)

    // Exposed States
    val activeRules: StateFlow<List<LockRule>> = repository.activeRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockHistory: StateFlow<List<LockHistory>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<String> = settingsManager.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val coolingOffDurationMs: StateFlow<Long> = settingsManager.coolingOffFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5 * 60 * 1000L)

    val isStrictMode: StateFlow<Boolean> = settingsManager.strictModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _installedAppsState = MutableStateFlow<AppListState>(AppListState.Loading)
    val installedAppsState: StateFlow<AppListState> = _installedAppsState.asStateFlow()

    init {
        loadInstalledApps()
        // Ensure service is running if active locks exist
        viewModelScope.launch {
            activeRules.collectLatest { rules ->
                if (rules.isNotEmpty()) {
                    LockMonitorForegroundService.startService(context)
                }
            }
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _installedAppsState.value = AppListState.Loading
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val apps = resolveInfos.mapNotNull { info ->
                    val packageName = info.activityInfo.packageName
                    if (packageName == context.packageName) return@mapNotNull null
                    val appLabel = info.loadLabel(pm).toString()
                    AppInfo(packageName, appLabel)
                }.distinctBy { it.packageName }.sortedBy { it.appLabel }
                _installedAppsState.value = AppListState.Success(apps)
            } catch (e: Exception) {
                _installedAppsState.value = AppListState.Error(e.localizedMessage ?: "Failed to list apps")
            }
        }
    }

    fun createLockRule(packageName: String, appLabel: String, durationMs: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if already active
                val existing = repository.getActiveRuleForPackage(packageName)
                if (existing != null) {
                    onResult(false, "$appLabel is already locked!")
                    return@launch
                }

                val nowRealtime = SystemClock.elapsedRealtime()
                val nowSystem = System.currentTimeMillis()

                val newRule = LockRule(
                    packageName = packageName,
                    appLabel = appLabel,
                    totalDurationMs = durationMs,
                    remainingDurationMs = durationMs,
                    lastActiveRealtime = nowRealtime,
                    lastActiveSystemTime = nowSystem
                )

                repository.insertRule(newRule)
                LockMonitorForegroundService.startService(context)
                onResult(true, "Successfully locked $appLabel!")
            } catch (e: Exception) {
                onResult(false, "Failed to create lock: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Delete or request delete for a lock rule.
     * If a cooling-off period is active, this schedules deletion in the future.
     * Otherwise, deletes immediately if allowed or if the rule isn't active.
     */
    fun requestDeleteRule(rule: LockRule, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!rule.isActive) {
                    // Already inactive or expired, delete immediately
                    repository.deleteRule(rule)
                    onResult(true, "Rule removed from dashboard")
                    return@launch
                }

                val coolingOffMs = coolingOffDurationMs.value
                if (coolingOffMs > 0) {
                    // Cooling off is enabled: mark the rule with pending delete timestamp
                    val targetTime = System.currentTimeMillis() + coolingOffMs
                    val updated = rule.copy(pendingDeleteTimestamp = targetTime)
                    repository.updateRule(updated)
                    
                    val hours = TimeUnit.MILLISECONDS.toHours(coolingOffMs)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(coolingOffMs) % 60
                    val delayStr = if (hours > 0) "$hours hr $minutes min" else "$minutes min"
                    onResult(true, "Cooling-off initiated. Rule will unlock in $delayStr.")
                } else {
                    // No cooling off: standard deletion
                    repository.deleteRule(rule)
                    onResult(true, "Rule unlocked immediately")
                }
            } catch (e: Exception) {
                onResult(false, "Failed to remove lock: ${e.localizedMessage}")
            }
        }
    }

    // Cancel pending cooling off deletion
    fun cancelPendingDeletion(rule: LockRule) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = rule.copy(pendingDeleteTimestamp = null)
            repository.updateRule(updated)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsManager.setTheme(theme)
        }
    }

    fun setCoolingOffDuration(durationMs: Long) {
        viewModelScope.launch {
            settingsManager.setCoolingOffDuration(durationMs)
        }
    }

    fun setStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setStrictMode(enabled)
        }
    }

    fun exportRulesToUri(outputStream: java.io.OutputStream, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rules = repository.getActiveRules()
                val jsonArray = org.json.JSONArray()
                for (rule in rules) {
                    val jsonObj = org.json.JSONObject().apply {
                        put("packageName", rule.packageName)
                        put("appLabel", rule.appLabel)
                        put("totalDurationMs", rule.totalDurationMs)
                        put("remainingDurationMs", rule.remainingDurationMs)
                        put("lastActiveRealtime", rule.lastActiveRealtime)
                        put("lastActiveSystemTime", rule.lastActiveSystemTime)
                        put("createdAt", rule.createdAt)
                        put("isActive", rule.isActive)
                        if (rule.pendingDeleteTimestamp != null) {
                            put("pendingDeleteTimestamp", rule.pendingDeleteTimestamp)
                        } else {
                            put("pendingDeleteTimestamp", org.json.JSONObject.NULL)
                        }
                    }
                    jsonArray.put(jsonObj)
                }
                outputStream.use { stream ->
                    stream.write(jsonArray.toString(4).toByteArray(Charsets.UTF_8))
                }
                onResult(true, "Settings exported successfully!")
            } catch (e: Exception) {
                onResult(false, "Export failed: ${e.localizedMessage}")
            }
        }
    }

    fun importRulesFromUri(inputStream: java.io.InputStream, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = inputStream.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                val jsonArray = org.json.JSONArray(jsonString)
                var importedCount = 0
                var skippedCount = 0
                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val packageName = jsonObj.getString("packageName")
                    val appLabel = jsonObj.getString("appLabel")
                    val totalDurationMs = jsonObj.getLong("totalDurationMs")
                    val remainingDurationMs = jsonObj.getLong("remainingDurationMs")
                    val lastActiveRealtime = jsonObj.getLong("lastActiveRealtime")
                    val lastActiveSystemTime = jsonObj.getLong("lastActiveSystemTime")
                    val createdAt = jsonObj.optLong("createdAt", System.currentTimeMillis())
                    val isActive = jsonObj.optBoolean("isActive", true)
                    val pendingDeleteTimestamp = if (jsonObj.isNull("pendingDeleteTimestamp")) null else jsonObj.getLong("pendingDeleteTimestamp")

                    // Check if already active
                    val existing = repository.getActiveRuleForPackage(packageName)
                    if (existing == null) {
                        val rule = LockRule(
                            packageName = packageName,
                            appLabel = appLabel,
                            totalDurationMs = totalDurationMs,
                            remainingDurationMs = remainingDurationMs,
                            lastActiveRealtime = lastActiveRealtime,
                            lastActiveSystemTime = lastActiveSystemTime,
                            createdAt = createdAt,
                            isActive = isActive,
                            pendingDeleteTimestamp = pendingDeleteTimestamp
                        )
                        repository.insertRule(rule)
                        importedCount++
                    } else {
                        skippedCount++
                    }
                }
                if (importedCount > 0) {
                    LockMonitorForegroundService.startService(context)
                }
                onResult(true, "Imported $importedCount rules ($skippedCount skipped).")
            } catch (e: Exception) {
                onResult(false, "Import failed: ${e.localizedMessage}")
            }
        }
    }
}
