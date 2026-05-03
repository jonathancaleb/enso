package com.example.enso.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.enso.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE provider = :provider ORDER BY date DESC")
    fun getByProvider(provider: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getCount(): Flow<Int>

    @Query(
        "SELECT type, SUM(amount) as total FROM transactions " +
        "WHERE date BETWEEN :start AND :end GROUP BY type"
    )
    fun getTypeTotals(start: Long, end: Long): Flow<List<TypeTotal>>

    @Query(
        "SELECT type, SUM(amount) as total FROM transactions " +
        "WHERE date BETWEEN :start AND :end AND provider = :provider GROUP BY type"
    )
    fun getTypeTotalsByProvider(start: Long, end: Long, provider: String): Flow<List<TypeTotal>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :since AND type NOT IN ('DEPOSIT', 'AIRTEL_RECEIVED', 'AIRTEL_INTEREST')")
    fun getTotalSpentSince(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :since AND type IN ('DEPOSIT', 'AIRTEL_RECEIVED', 'AIRTEL_INTEREST')")
    fun getTotalReceivedSince(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :since AND provider = :provider AND type NOT IN ('DEPOSIT', 'AIRTEL_RECEIVED', 'AIRTEL_INTEREST')")
    fun getTotalSpentByProvider(since: Long, provider: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :since AND provider = :provider AND type IN ('DEPOSIT', 'AIRTEL_RECEIVED', 'AIRTEL_INTEREST')")
    fun getTotalReceivedByProvider(since: Long, provider: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE transactionId IS NULL AND date = :date AND amount = :amount AND type = :type)")
    suspend fun existsByComposite(date: Long, amount: Double, type: String): Boolean

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class TypeTotal(
    val type: String,
    val total: Double
)
