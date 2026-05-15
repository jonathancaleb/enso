package com.example.enso.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.enso.data.local.entity.TransactionEntity

/*
 * Current checked-in schema version is 3. This repo did not previously export
 * Room schemas, so the historical 1->2 and 2->3 changes must be verified
 * against any production APK/database before release. They are registered as
 * no-op migrations to prevent destructive fallback from wiping user data.
 */
@Database(entities = [TransactionEntity::class], version = 3, exportSchema = true)
@TypeConverters(TransactionConverters::class)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: TransactionDatabase? = null

        fun getInstance(context: Context): TransactionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "enso_transactions.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No exported v1/v2 schema exists in this repo; preserve data.
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No exported v2/v3 schema exists in this repo; preserve data.
            }
        }
    }
}
