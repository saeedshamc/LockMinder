package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import com.example.data.AppDatabase
import com.example.ui.blockoverlay.BlockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppBlockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var activePackages = setOf<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AccessibilityService", "Service connected")
        val db = AppDatabase.getDatabase(applicationContext)
        serviceScope.launch {
            db.lockRuleDao().getActiveRulesFlow().collectLatest { rules ->
                activePackages = rules.map { it.packageName }.toSet()
                Log.d("AccessibilityService", "Updated active locked packages: $activePackages")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Do not block ourselves or our own components
            if (packageName == applicationContext.packageName) return
            
            if (activePackages.contains(packageName)) {
                Log.d("AccessibilityService", "Package $packageName is locked. Opening overlay.")
                val intent = Intent(this, BlockOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("LOCKED_PACKAGE_NAME", packageName)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "Service interrupted")
    }
}
