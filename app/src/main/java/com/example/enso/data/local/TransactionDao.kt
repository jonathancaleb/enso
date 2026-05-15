package com.example.enso.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.enso.data.local.entity.Provider
import com.example.enso.data.local.entity.TransactionEntity
import com.example.enso.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date >= :start AND date < :end ORDER BY date DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE provider = :provider ORDER BY date DESC")
    fun getByProvider(provider: Provider): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE provider = :provider ORDER BY date DESC LIMIT :limit")
    fun getRecentByProvider(provider: Provider, limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getCount(): Flow<Int>

    @Query(
        "SELECT type, SUM(amount) as total FROM transactions " +
        "WHERE date >= :start AND date < :end GROUP BY type"
    )
    fun getTypeTotals(start: Long, end: Long): Flow<List<TypeTotal>>

    @Query(
        "SELECT type, SUM(amount) as total FROM transactions " +
        "WHERE date >= :start AND date < :end AND provider = :provider GROUP BY type"
    )
    fun getTypeTotalsByProvider(start: Long, end: Long, provider: Provider): Flow<List<TypeTotal>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :start AND date < :end AND type NOT IN (:incomingTypes)")
    fun getTotalSpent(start: Long, end: Long, incomingTypes: List<TransactionType>): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :start AND date < :end AND type IN (:incomingTypes)")
    fun getTotalReceived(start: Long, end: Long, incomingTypes: List<TransactionType>): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :start AND date < :end AND provider = :provider AND type NOT IN (:incomingTypes)")
    fun getTotalSpentByProvider(
        start: Long,
        end: Long,
        provider: Provider,
        incomingTypes: List<TransactionType>
    ): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date >= :start AND date < :end AND provider = :provider AND type IN (:incomingTypes)")
    fun getTotalReceivedByProvider(
        start: Long,
        end: Long,
        provider: Provider,
        incomingTypes: List<TransactionType>
    ): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE transactionId IS NULL AND date = :date AND amount = :amount AND type = :type)")
    suspend fun existsByComposite(date: Long, amount: Double, type: TransactionType): Boolean

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class TypeTotal(
    val type: TransactionType,
    val total: Double
)
