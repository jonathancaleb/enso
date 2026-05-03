package com.example.enso.data.repository

import com.example.enso.data.local.TransactionDao
import com.example.enso.data.local.TypeTotal
import com.example.enso.data.local.entity.TransactionEntity
import com.example.enso.data.sms.SmsImportService
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao,
    private val importService: SmsImportService
) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = dao.getAll()

    fun getTransactionsByProvider(provider: String): Flow<List<TransactionEntity>> =
        dao.getByProvider(provider)

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> =
        dao.getRecent(limit)

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        dao.getByDateRange(start, end)

    fun getTypeTotals(start: Long, end: Long): Flow<List<TypeTotal>> =
        dao.getTypeTotals(start, end)

    fun getTypeTotalsByProvider(start: Long, end: Long, provider: String): Flow<List<TypeTotal>> =
        dao.getTypeTotalsByProvider(start, end, provider)

    fun getTotalSpentSince(since: Long): Flow<Double> = dao.getTotalSpentSince(since)
    fun getTotalReceivedSince(since: Long): Flow<Double> = dao.getTotalReceivedSince(since)

    fun getTotalSpentByProvider(since: Long, provider: String): Flow<Double> =
        dao.getTotalSpentByProvider(since, provider)
    fun getTotalReceivedByProvider(since: Long, provider: String): Flow<Double> =
        dao.getTotalReceivedByProvider(since, provider)

    fun getTransactionCount(): Flow<Int> = dao.getCount()

    suspend fun triggerSmsSync(): Int = importService.runImport()

    suspend fun addManualTransaction(transaction: TransactionEntity): Long =
        dao.insert(transaction)

    suspend fun deleteTransaction(id: Long) = dao.deleteById(id)
}
