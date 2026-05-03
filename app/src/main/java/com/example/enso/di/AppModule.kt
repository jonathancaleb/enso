package com.example.enso.di

import android.content.Context
import com.example.enso.data.local.TransactionDao
import com.example.enso.data.local.TransactionDatabase
import com.example.enso.data.repository.TransactionRepository
import com.example.enso.data.sms.SmsImportService
import com.example.enso.data.sms.SmsReader
import com.example.enso.data.sms.ensoDataStore

object AppModule {

    @Volatile private var database: TransactionDatabase? = null
    @Volatile private var repository: TransactionRepository? = null
    @Volatile private var importService: SmsImportService? = null

    fun provideDatabase(context: Context): TransactionDatabase {
        return database ?: synchronized(this) {
            database ?: TransactionDatabase.getInstance(context).also { database = it }
        }
    }

    fun provideDao(context: Context): TransactionDao {
        return provideDatabase(context).transactionDao()
    }

    fun provideSmsReader(context: Context): SmsReader {
        return SmsReader(context.contentResolver)
    }

    fun provideSmsImportService(context: Context): SmsImportService {
        return importService ?: synchronized(this) {
            importService ?: SmsImportService(
                dao = provideDao(context),
                smsReader = provideSmsReader(context),
                dataStore = context.applicationContext.ensoDataStore
            ).also { importService = it }
        }
    }

    fun provideRepository(context: Context): TransactionRepository {
        return repository ?: synchronized(this) {
            repository ?: TransactionRepository(
                dao = provideDao(context),
                importService = provideSmsImportService(context)
            ).also { repository = it }
        }
    }
}
