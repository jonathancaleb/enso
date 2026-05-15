package com.example.enso.data.repository

import com.example.enso.data.local.TransactionDao
import com.example.enso.data.local.TypeTotal
import com.example.enso.data.local.entity.Provider
import com.example.enso.data.local.entity.TransactionEntity
import com.example.enso.data.local.entity.TransactionType
import com.example.enso.data.sms.SmsImportService
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao,
    private val importService: SmsImportService
) {
    private val incomingTypes = TransactionType.incomingTypes

    fun getAllTransactions(): Flow<List<TransactionEntity>> = dao.getAll()

    fun getTransactionsByProvider(provider: Provider): Flow<List<TransactionEntity>> =
        dao.getByProvider(provider)

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> =
        dao.getRecent(limit)

    fun getRecentTransactionsByProvider(provider: Provider, limit: Int = 5): Flow<List<TransactionEntity>> =
        dao.getRecentByProvider(provider, limit)

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        dao.getByDateRange(start, end)

    fun getTypeTotals(start: Long, end: Long): Flow<List<TypeTotal>> =
        dao.getTypeTotals(start, end)

    fun getTypeTotalsByProvider(start: Long, end: Long, provider: Provider): Flow<List<TypeTotal>> =
        dao.getTypeTotalsByProvider(start, end, provider)

    fun getTotalSpent(start: Long, end: Long): Flow<Double> =
        dao.getTotalSpent(start, end, incomingTypes)
    fun getTotalReceived(start: Long, end: Long): Flow<Double> =
        dao.getTotalReceived(start, end, incomingTypes)

    fun getTotalSpentByProvider(start: Long, end: Long, provider: Provider): Flow<Double> =
        dao.getTotalSpentByProvider(start, end, provider, incomingTypes)
    fun getTotalReceivedByProvider(start: Long, end: Long, provider: Provider): Flow<Double> =
        dao.getTotalReceivedByProvider(start, end, provider, incomingTypes)

    fun getTransactionCount(): Flow<Int> = dao.getCount()

    suspend fun triggerSmsSync(): Int = importService.runImport()

    suspend fun addManualTransaction(transaction: TransactionEntity): Long =
        dao.insert(transaction)

    suspend fun deleteTransaction(id: Long) = dao.deleteById(id)
}
