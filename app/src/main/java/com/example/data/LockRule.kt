package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lock_rules")
data class LockRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appLabel: String,
    val totalDurationMs: Long,
    val remainingDurationMs: Long,
    val lastActiveRealtime: Long,
    val lastActiveSystemTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val pendingDeleteTimestamp: Long? = null
)

@Entity(tableName = "lock_history")
data class LockHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appLabel: String,
    val durationMs: Long,
    val lockedAt: Long,
    val unlockedAt: Long? = null,
    val wasCompleted: Boolean = true
)
