package com.example.enso.data.repository

import com.example.enso.data.local.TransactionDao
import com.example.enso.data.local.TypeTotal
import com.example.enso.data.local.entity.Transaction
import com.example.enso.data.sms.MomoSmsParser
import com.example.enso.data.sms.SmsReader
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao,
    private val smsReader: SmsReader
) {

    fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions()

    fun getTransactionsByProvider(provider: String): Flow<List<Transaction>> =
        dao.getTransactionsByProvider(provider)

    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>> =
        dao.getRecentTransactions(limit)

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>> =
        dao.getTransactionsBetween(start, end)

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

    fun getTransactionCount(): Flow<Int> = dao.getTransactionCount()

    suspend fun syncFromSms(): Int {
        val existingHashes = dao.getAllSmsHashes().toSet()
        val rawMessages = smsReader.readMomoMessages()
        val newTransactions = rawMessages
            .mapNotNull { MomoSmsParser.parse(it) }
            .filter { it.smsHash != null && it.smsHash !in existingHashes }
        if (newTransactions.isNotEmpty()) {
            dao.insertAll(newTransactions)
        }
        return newTransactions.size
    }

    suspend fun addManualTransaction(transaction: Transaction): Long =
        dao.insert(transaction)

    suspend fun deleteTransaction(id: Long) = dao.deleteById(id)
}
