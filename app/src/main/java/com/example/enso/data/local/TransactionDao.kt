package com.example.enso.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.enso.data.local.entity.Transaction
import com.example.enso.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE provider = :provider ORDER BY date DESC")
    fun getTransactionsByProvider(provider: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query(
        "SELECT type, SUM(amount) as total FROM transactions " +
        "WHERE date BETWEEN :startDate AND :endDate GROUP BY type"
    )
    fun getTypeTotals(startDate: Long, endDate: Long): Flow<List<TypeTotal>>

    @Query(
        "SELECT type, SUM(amount) as total FROM transactions " +
        "WHERE date BETWEEN :startDate AND :endDate AND provider = :provider GROUP BY type"
    )
    fun getTypeTotalsByProvider(startDate: Long, endDate: Long, provider: String): Flow<List<TypeTotal>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :since AND type NOT IN ('DEPOSIT', 'AIRTEL_RECEIVED', 'AIRTEL_INTEREST')")
    fun getTotalSpentSince(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :since AND type IN ('DEPOSIT', 'AIRTEL_RECEIVED', 'AIRTEL_INTEREST')")
    fun getTotalReceivedSince(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :since AND provider = :provider AND type NOT IN ('DEPOSIT', 'AIRTEL_RECEIVED', 'AIRTEL_INTEREST')")
    fun getTotalSpentByProvider(since: Long, provider: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :since AND provider = :provider AND type IN ('DEPOSIT', 'AIRTEL_RECEIVED', 'AIRTEL_INTEREST')")
    fun getTotalReceivedByProvider(since: Long, provider: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT smsHash FROM transactions WHERE smsHash IS NOT NULL")
    suspend fun getAllSmsHashes(): List<String>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>
}

data class TypeTotal(
    val type: TransactionType,
    val total: Double
)
