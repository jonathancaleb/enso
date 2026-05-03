package com.example.enso.sms

import java.text.SimpleDateFormat
import java.util.Locale

enum class TransactionType {
    WITHDRAWAL,
    DEPOSIT,
    TRANSFER
}

data class ParsedTransaction(
    val type: TransactionType,
    val amount: Double,
    val fee: Double,
    val balance: Double?,
    val date: Long,
    val counterparty: String?
)

class MoMoSmsParser {

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        private const val AMT = """\d[\d,]*(?:\.\d+)?"""

        private val withdrawalPattern = Regex(
            """You have withdrawn UGX ($AMT) on (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})""",
            RegexOption.IGNORE_CASE
        )
        private val depositPattern = Regex(
            """You have received UGX ($AMT) from (.+?) on (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})""",
            RegexOption.IGNORE_CASE
        )
        private val transferPattern = Regex(
            """You have sent UGX ($AMT) to (\S+),\s*(.+?)\.?\s*Fee""",
            RegexOption.IGNORE_CASE
        )
        private val feePattern = Regex(
            """Fee[:\s]*UGX\s*($AMT)""",
            RegexOption.IGNORE_CASE
        )
        private val feeZeroPattern = Regex(
            """fee[:\s]*0(?:\.0+)?(?:\b|\.)""",
            RegexOption.IGNORE_CASE
        )
        private val balanceNewPattern = Regex(
            """New balance[:\s]*UGX\s*($AMT)""",
            RegexOption.IGNORE_CASE
        )
        private val balanceNowPattern = Regex(
            """balance is now UGX\s*($AMT)""",
            RegexOption.IGNORE_CASE
        )

        fun parse(body: String, smsTimestamp: Long): ParsedTransaction? {
            return parseWithdrawal(body, smsTimestamp)
                ?: parseDeposit(body, smsTimestamp)
                ?: parseTransfer(body, smsTimestamp)
        }

        private fun parseWithdrawal(body: String, smsTimestamp: Long): ParsedTransaction? {
            val match = withdrawalPattern.find(body) ?: return null
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val date = parseDate(match.groupValues[2]) ?: smsTimestamp
            return ParsedTransaction(
                type = TransactionType.WITHDRAWAL,
                amount = amount,
                fee = parseFee(body),
                balance = parseBalance(body),
                date = date,
                counterparty = null
            )
        }

        private fun parseDeposit(body: String, smsTimestamp: Long): ParsedTransaction? {
            val match = depositPattern.find(body) ?: return null
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val counterparty = match.groupValues[2].trim()
            val date = parseDate(match.groupValues[3]) ?: smsTimestamp
            return ParsedTransaction(
                type = TransactionType.DEPOSIT,
                amount = amount,
                fee = parseFee(body),
                balance = parseBalance(body),
                date = date,
                counterparty = counterparty
            )
        }

        private fun parseTransfer(body: String, smsTimestamp: Long): ParsedTransaction? {
            val match = transferPattern.find(body) ?: return null
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val number = match.groupValues[2].trim()
            val name = match.groupValues[3].trim().trimEnd('.')
            val counterparty = "$number $name"
            return ParsedTransaction(
                type = TransactionType.TRANSFER,
                amount = amount,
                fee = parseFee(body),
                balance = parseBalance(body),
                date = smsTimestamp,
                counterparty = counterparty
            )
        }

        private fun parseAmount(raw: String): Double? {
            return raw.replace(",", "").toDoubleOrNull()
        }

        private fun parseFee(body: String): Double {
            if (feeZeroPattern.containsMatchIn(body)) return 0.0
            val match = feePattern.find(body) ?: return 0.0
            return parseAmount(match.groupValues[1]) ?: 0.0
        }

        private fun parseBalance(body: String): Double? {
            val match = balanceNewPattern.find(body) ?: balanceNowPattern.find(body) ?: return null
            return parseAmount(match.groupValues[1])
        }

        private fun parseDate(raw: String): Long? {
            return try {
                dateFormat.parse(raw)?.time
            } catch (_: Exception) {
                null
            }
        }
    }
}
