package com.example.enso.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Provider(val dbValue: String) {
    MTN("MTN"),
    AIRTEL("AIRTEL"),
    MANUAL("MANUAL"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromDbValue(value: String): Provider {
            return entries.firstOrNull { it.dbValue == value } ?: UNKNOWN
        }
    }
}

enum class TransactionType(val dbValue: String) {
    WITHDRAWAL("WITHDRAWAL"),
    DEPOSIT("DEPOSIT"),
    TRANSFER_OUT("TRANSFER_OUT"),
    MERCHANT_PAYMENT("MERCHANT_PAYMENT"),
    BUNDLE_PURCHASE("BUNDLE_PURCHASE"),
    MOMO_ADVANCE("MOMO_ADVANCE"),
    OVERDRAFT_REPAY("OVERDRAFT_REPAY"),
    ELECTRICITY("ELECTRICITY"),
    LOAN_COLLECTION("LOAN_COLLECTION"),
    AIRTEL_SENT("AIRTEL_SENT"),
    AIRTEL_RECEIVED("AIRTEL_RECEIVED"),
    AIRTEL_PAYMENT("AIRTEL_PAYMENT"),
    AIRTEL_INTEREST("AIRTEL_INTEREST"),
    UNKNOWN("UNKNOWN");

    val displayName: String
        get() = dbValue.replace("_", " ")

    companion object {
        val incomingTypes = listOf(DEPOSIT, AIRTEL_RECEIVED, AIRTEL_INTEREST)
        val zeroAmountAllowedTypes = setOf(AIRTEL_INTEREST)

        fun fromDbValue(value: String): TransactionType {
            return entries.firstOrNull { it.dbValue == value } ?: UNKNOWN
        }
    }
}

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["transactionId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: Provider,
    val type: TransactionType,
    val amount: Double,
    val fee: Double = 0.0,
    val balance: Double? = null,
    val date: Long,
    val description: String,
    val transactionId: String? = null,
    val rawMessage: String = ""
)
