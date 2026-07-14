package com.example.data

import kotlinx.coroutines.flow.Flow

class LockRepository(private val lockRuleDao: LockRuleDao) {
    val activeRules: Flow<List<LockRule>> = lockRuleDao.getActiveRulesFlow()
    val history: Flow<List<LockHistory>> = lockRuleDao.getHistoryFlow()

    suspend fun getActiveRules(): List<LockRule> = lockRuleDao.getActiveRules()

    suspend fun getActiveRuleForPackage(packageName: String): LockRule? =
        lockRuleDao.getActiveRuleForPackage(packageName)

    suspend fun getRuleById(id: Int): LockRule? =
        lockRuleDao.getRuleById(id)

    suspend fun insertRule(rule: LockRule): Long =
        lockRuleDao.insertRule(rule)

    suspend fun updateRule(rule: LockRule) =
        lockRuleDao.updateRule(rule)

    suspend fun deleteRule(rule: LockRule) =
        lockRuleDao.deleteRule(rule)

    suspend fun insertHistory(history: LockHistory) =
        lockRuleDao.insertHistory(history)
}
