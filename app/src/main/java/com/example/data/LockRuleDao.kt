package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LockRuleDao {
    @Query("SELECT * FROM lock_rules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveRulesFlow(): Flow<List<LockRule>>

    @Query("SELECT * FROM lock_rules WHERE isActive = 1")
    suspend fun getActiveRules(): List<LockRule>

    @Query("SELECT * FROM lock_rules WHERE packageName = :packageName AND isActive = 1 LIMIT 1")
    suspend fun getActiveRuleForPackage(packageName: String): LockRule?

    @Query("SELECT * FROM lock_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Int): LockRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: LockRule): Long

    @Update
    suspend fun updateRule(rule: LockRule)

    @Delete
    suspend fun deleteRule(rule: LockRule)

    @Query("SELECT * FROM lock_history ORDER BY lockedAt DESC")
    fun getHistoryFlow(): Flow<List<LockHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: LockHistory)
}
