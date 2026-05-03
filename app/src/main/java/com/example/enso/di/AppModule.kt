package com.example.enso.di

import android.content.Context
import com.example.enso.data.local.AppDatabase
import com.example.enso.data.local.TransactionDao
import com.example.enso.data.repository.TransactionRepository
import com.example.enso.data.sms.SmsReader

object AppModule {

    private var database: AppDatabase? = null
    private var repository: TransactionRepository? = null

    fun provideDatabase(context: Context): AppDatabase {
        return database ?: AppDatabase.getInstance(context).also { database = it }
    }

    fun provideDao(context: Context): TransactionDao {
        return provideDatabase(context).transactionDao()
    }

    fun provideSmsReader(context: Context): SmsReader {
        return SmsReader(context.contentResolver)
    }

    fun provideRepository(context: Context): TransactionRepository {
        return repository ?: TransactionRepository(
            dao = provideDao(context),
            smsReader = provideSmsReader(context)
        ).also { repository = it }
    }
}
