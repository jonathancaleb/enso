package com.example.enso.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object Provider {
    const val MTN = "MTN"
    const val AIRTEL = "AIRTEL"
    const val MANUAL = "MANUAL"
}

object TransactionType {
    const val WITHDRAWAL = "WITHDRAWAL"
    const val DEPOSIT = "DEPOSIT"
    const val TRANSFER_OUT = "TRANSFER_OUT"
    const val MERCHANT_PAYMENT = "MERCHANT_PAYMENT"
    const val BUNDLE_PURCHASE = "BUNDLE_PURCHASE"
    const val MOMO_ADVANCE = "MOMO_ADVANCE"
    const val OVERDRAFT_REPAY = "OVERDRAFT_REPAY"
    const val ELECTRICITY = "ELECTRICITY"
    const val LOAN_COLLECTION = "LOAN_COLLECTION"
    const val AIRTEL_SENT = "AIRTEL_SENT"
    const val AIRTEL_RECEIVED = "AIRTEL_RECEIVED"
    const val AIRTEL_PAYMENT = "AIRTEL_PAYMENT"
    const val AIRTEL_INTEREST = "AIRTEL_INTEREST"
    const val UNKNOWN = "UNKNOWN"
}

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["transactionId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: String,
    val type: String,
    val amount: Double,
    val fee: Double = 0.0,
    val balance: Double? = null,
    val date: Long,
    val description: String,
    val transactionId: String? = null,
    val rawMessage: String = ""
)
