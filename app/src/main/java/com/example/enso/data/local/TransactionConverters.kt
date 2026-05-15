package com.example.enso.data.local

import androidx.room.TypeConverter
import com.example.enso.data.local.entity.Provider
import com.example.enso.data.local.entity.TransactionType

class TransactionConverters {

    @TypeConverter
    fun providerToString(provider: Provider): String = provider.dbValue

    @TypeConverter
    fun stringToProvider(value: String): Provider = Provider.fromDbValue(value)

    @TypeConverter
    fun transactionTypeToString(type: TransactionType): String = type.dbValue

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.fromDbValue(value)
}
