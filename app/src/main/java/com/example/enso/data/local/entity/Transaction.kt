package com.example.enso.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Provider { MTN, AIRTEL, MANUAL }

enum class TransactionType {
    WITHDRAWAL,
    DEPOSIT,
    TRANSFER_OUT,
    MERCHANT_PAYMENT,
    BUNDLE_PURCHASE,
    MOMO_ADVANCE,
    OVERDRAFT_REPAY,
    ELECTRICITY,
    LOAN_COLLECTION,
    AIRTEL_SENT,
    AIRTEL_RECEIVED,
    AIRTEL_PAYMENT,
    AIRTEL_INTEREST,
    UNKNOWN
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: Provider = Provider.MANUAL,
    val type: TransactionType,
    val amount: Double,
    val fee: Double = 0.0,
    val balance: Double? = null,
    val description: String,
    val transactionId: String? = null,
    val date: Long,
    val rawSms: String? = null,
    val isManual: Boolean = false,
    val smsHash: String? = null
)
