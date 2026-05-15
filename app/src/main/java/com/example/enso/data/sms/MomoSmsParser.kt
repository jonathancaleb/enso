package com.example.enso.data.sms

import android.util.Log
import com.example.enso.data.local.entity.Provider
import com.example.enso.data.local.entity.TransactionEntity
import com.example.enso.data.local.entity.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class RawSms(
    val sender: String,
    val body: String,
    val date: Long
)

object MomoSmsParser {

    private const val TAG = "MomoSmsParser"
    private const val AMOUNT = """\d[\d,]*(?:\.\d+)?"""
    private const val MAX_UNPARSED_LOGS = 25
    private val unparsedLogCount = AtomicInteger(0)

    private val SILENT_IGNORE = listOf(
        "Please enter the following code",
        "preapproval with identity",
        "WRONG PIN",
        "select My Approvals",
        "Your Current Balance is",
        "You've purchased Electricity",
        "Debit with Transaction with ID",
        "You have been debited",
        "You have received Airtime Topup",
        "Do NOT share your Mobile Money PIN",
        "all Airtel Money services have been",
        "Move money from your bank account",
        "Heading to the UMA Trade Fair"
    )

    private val FAILED_PATTERNS = listOf(
        "failed at",
        "has failed at",
        "FAILED. Insufficient funds",
        "does not have sufficient money",
        "Reason: Expired"
    )

    // yyyy-MM-dd HH:mm:ss
    private val mtnDateRegex = Regex("""\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}""")
    private val mtnDateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    // dd-MMM-yyyy HH:mm  (e.g. 02-May-2026 15:29)
    private val airtelDateRegex = Regex("""\d{2}-[A-Za-z]{3,9}-\d{4}\s+\d{2}:\d{2}""")
    private val airtelDateFmt = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH)

    fun parse(sms: RawSms): TransactionEntity? {
        val body = sms.body.trim()
        val sender = sms.sender

        val provider = resolveProvider(sender) ?: return null

        if (shouldSilentIgnore(body)) return null
        if (isFailedTransaction(body)) return null

        val result = when (provider) {
            Provider.MTN -> parseMtn(body, sms.date)
            Provider.AIRTEL -> parseAirtel(body, sms.date)
            else -> null
        }

        if (result == null) logUnparsed(sender, body, sms.date)

        return result?.copy(rawMessage = body)
    }

    private fun resolveProvider(sender: String): Provider? {
        if (sender.contains("MTN", ignoreCase = true)) return Provider.MTN
        if (sender.equals("AirtelMoney", ignoreCase = true) ||
            sender.equals("Airtel", ignoreCase = true)) return Provider.AIRTEL
        return null
    }

    private fun shouldSilentIgnore(body: String): Boolean {
        if (body.startsWith("http", ignoreCase = true)) return true
        if (body.contains("Secret Code", ignoreCase = true) &&
            body.contains("initiated", ignoreCase = true)) return true
        if (Regex("""^\s*\d+\.\d{2}-\d{2}-\d{2}\s+(DEBIT|PYBL)\b""", RegexOption.IGNORE_CASE)
                .containsMatchIn(body)) return true
        for (pattern in SILENT_IGNORE) {
            if (body.contains(pattern, ignoreCase = true)) {
                if (pattern == "Do NOT share your Mobile Money PIN") {
                    return !Regex("""UGX\s*$AMOUNT|$AMOUNT\s*UGX""", RegexOption.IGNORE_CASE).containsMatchIn(body)
                }
                return true
            }
        }
        return false
    }

    private fun isFailedTransaction(body: String): Boolean {
        if (body.startsWith("FAILED. TID", ignoreCase = true)) return true
        return FAILED_PATTERNS.any { body.contains(it, ignoreCase = true) }
    }

    private fun logUnparsed(sender: String, body: String, date: Long) {
        val count = unparsedLogCount.incrementAndGet()
        when {
            count <= MAX_UNPARSED_LOGS -> {
                Log.w(TAG, "Unparsed [$sender], length=${body.length}, date=$date")
            }
            count == MAX_UNPARSED_LOGS + 1 -> {
                Log.w(TAG, "Further unparsed SMS logs suppressed")
            }
        }
    }

    // ─── MTN ──────────────────────────────────────────────

    private fun parseMtn(body: String, ts: Long): TransactionEntity? {
        return when {
            body.contains("You have withdrawn", ignoreCase = true) ->
                parseMtnWithdrawal(body, ts)

            body.contains("You have received", ignoreCase = true) ||
                body.contains("You have deposited", ignoreCase = true) ->
                parseMtnDeposit(body, ts)

            body.contains("You have sent", ignoreCase = true) ||
                body.contains("You have transferred", ignoreCase = true) ->
                parseMtnTransferOut(body, ts)

            body.contains("has deducted UGX", ignoreCase = true) && body.contains("Bundle Purchase", ignoreCase = true) ->
                parseMtnBundlePurchase(body, ts)

            body.contains("has deducted UGX", ignoreCase = true) && body.contains("Payment for services", ignoreCase = true) ->
                parseMtnMerchantPayment(body, ts)

            body.contains("Bundle loaded worth", ignoreCase = true) ->
                parseMtnBundleLoaded(body, ts)

            body.contains("You have bought UGX", ignoreCase = true) &&
                body.contains("airtime", ignoreCase = true) ->
                parseMtnAirtimePurchase(body, ts)

            body.contains("from MOMOADVANCE", ignoreCase = true) ->
                parseMtnMomoAdvance(body, ts)

            body.contains("has deducted UGX", ignoreCase = true) ->
                parseMtnMerchantPayment(body, ts)

            body.contains("You have paid", ignoreCase = true) ->
                parseMtnMomoPay(body, ts)

            body.contains("has been used to repay your OVERDRAFT", ignoreCase = true) ->
                parseMtnOverdraftRepay(body, ts)

            body.contains("for UEDCL", ignoreCase = true) && body.contains("Account Number", ignoreCase = true) ->
                parseMtnElectricity(body, ts)

            else -> null
        }
    }

    private fun parseMtnWithdrawal(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "withdrawn UGX")
        val fee = extractAfter(body, "Fee: UGX") ?: extractAfter(body, "Fee:UGX") ?: 0.0
        val balance = extractAfter(body, "New balance: UGX") ?: extractAfter(body, "New balance:UGX")
        val date = parseMtnDate(body) ?: ts
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.WITHDRAWAL,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = date,
            description = "Cash withdrawal"
        )
    }

    private fun parseMtnDeposit(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "received UGX") ?: extractAfter(body, "deposited UGX")
        val balance = extractAfter(body, "New balance: UGX") ?: extractAfter(body, "New balance:UGX")
        val date = parseMtnDate(body) ?: ts
        val txnId = extractTransactionId(body)
        val counterparty = Regex("""from\s+(.+?)\s+on\s+\d{4}-\d{2}-\d{2}""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim()
            ?: ""
        val reason = extractBetween(body, "Reason:", ".") ?: ""
        val desc = when {
            reason.isNotBlank() -> "Received - ${reason.trim()}"
            counterparty.isNotBlank() -> "Deposit from $counterparty"
            body.contains("deposited", ignoreCase = true) -> "Deposit"
            else -> "Received"
        }
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.DEPOSIT,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = date,
            transactionId = txnId,
            description = desc
        )
    }

    private fun parseMtnTransferOut(body: String, ts: Long): TransactionEntity {
        val amount = Regex(
            """(?:sent|transferred)\s+(?:(?:UGX\s*){1,2}($AMOUNT)|($AMOUNT)\s*UGX)""",
            RegexOption.IGNORE_CASE
        ).find(body)?.let { match ->
            match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.amountToDouble()
        }
        val balance = extractAfter(body, "balance is now UGX")
            ?: extractAfter(body, "Your new balance: UGX")
            ?: extractAfter(body, "New balance: UGX")
        val fee = extractFee(body)
        val date = parseMtnDate(body) ?: ts
        val txnId = extractTransactionId(body)
        val recipient = Regex(
            """(?:sent|transferred)\s+(?:(?:UGX\s*){1,2}$AMOUNT|$AMOUNT\s*UGX)\s+to\s+(.+?)(?:\s+from\b|\s+at\b|,?\s*Fee\b|\.|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()?.trimEnd(',') ?: "Unknown"
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.TRANSFER_OUT,
            amount = amount ?: 0.0,
            fee = fee,
            balance = balance?.toDouble(),
            date = date,
            transactionId = txnId,
            description = "Sent to $recipient"
        )
    }

    private fun parseMtnMerchantPayment(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "deducted UGX")
        val balance = extractAfter(body, "New balance is:UGX") ?: extractAfter(body, "New balance is: UGX")
            ?: extractAfter(body, "New balance: UGX")
        val fee = extractFee(body)
        val txnId = extractTransactionId(body)
        val merchant = extractBetween(body, "Y'ello.", "has deducted")
            ?.trim()
            ?.trimEnd('.')
            ?.ifBlank { null }
            ?: "Merchant"
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.MERCHANT_PAYMENT,
            amount = amount ?: 0.0,
            fee = fee,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = txnId,
            description = "Payment - $merchant"
        )
    }

    private fun parseMtnBundlePurchase(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "deducted UGX")
        val balance = extractAfter(body, "New balance is:UGX") ?: extractAfter(body, "New balance is: UGX")
            ?: extractAfter(body, "New balance: UGX")
        val fee = extractFee(body)
        val txnId = extractTransactionId(body)
        val merchant = extractBetween(body, "Y'ello.", "has deducted")?.trim() ?: "MTN"
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.BUNDLE_PURCHASE,
            amount = amount ?: 0.0,
            fee = fee,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = txnId,
            description = "Bundle purchase - $merchant"
        )
    }

    private fun parseMtnBundleLoaded(body: String, ts: Long): TransactionEntity {
        val amount = Regex("""Bundle loaded worth\s+UGX\s*($AMOUNT)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val balance = extractAfter(body, "New balance: UGX")
        val txnId = extractTransactionId(body)
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.BUNDLE_PURCHASE,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = txnId,
            description = "Bundle purchase"
        )
    }

    private fun parseMtnAirtimePurchase(body: String, ts: Long): TransactionEntity {
        val amount = Regex("""bought\s+UGX\s*($AMOUNT)\s+airtime""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val balance = extractAfter(body, "New MoMo balance: UGX")
        val txnId = extractTransactionId(body)
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.BUNDLE_PURCHASE,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = txnId,
            description = "Airtime purchase"
        )
    }

    private fun parseMtnMomoAdvance(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "used UGX")
        val fee = extractAfter(body, "access fee UGX") ?: 0.0
        val balance = extractAfter(body, "MOMOADVANCE balance is UGX")
        val date = parseMtnDate(body) ?: ts
        val txnId = extractTransactionId(body)
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.MOMO_ADVANCE,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = date,
            transactionId = txnId,
            description = "MoMo Advance"
        )
    }

    private fun parseMtnMomoPay(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "paid UGX") ?: extractAfterFirst(body, "UGX")
        val balance = extractAfter(body, "New balance: UGX") ?: extractAfter(body, "New balance is:UGX")
            ?: extractAfter(body, "New balance is: UGX")
        val fee = extractFee(body)
        val date = parseMtnDate(body) ?: ts
        val txnId = extractTransactionId(body)
        val recipient = Regex("""paid\s+(?:UGX\s*[\d,]+\s+to\s+)?(.+?)(?:\.|New|Fee|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim()
            ?.replace(Regex("""UGX\s*[\d,]+\s*to\s*"""), "")?.trim()
            ?: "Unknown"
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.MERCHANT_PAYMENT,
            amount = amount ?: 0.0,
            fee = fee,
            balance = balance?.toDouble(),
            date = date,
            transactionId = txnId,
            description = "Paid $recipient"
        )
    }

    private fun parseMtnOverdraftRepay(body: String, ts: Long): TransactionEntity {
        val amount = Regex("""Y'ello\.\s*UGX\s*([\d,]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val date = parseMtnDate(body) ?: ts
        val txnId = extractTransactionId(body)
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.OVERDRAFT_REPAY,
            amount = amount ?: 0.0,
            date = date,
            transactionId = txnId,
            description = "Overdraft repayment"
        )
    }

    private fun parseMtnElectricity(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "paid UGX")
        val fee = Regex("""fee\s+of\s+UGX\s*([\d,]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val balance = extractAfter(body, "New balance is UGX") ?: extractAfter(body, "New balance is:UGX")
        val txnId = Regex("""ID:\s*(\S+)""").find(body)?.groupValues?.get(1)
        val meter = Regex("""Account Number[:\s]*(\S+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1) ?: ""
        return TransactionEntity(
            provider = Provider.MTN,
            type = TransactionType.ELECTRICITY,
            amount = amount ?: 0.0,
            fee = fee ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = txnId,
            description = "Electricity - UEDCL meter $meter".trim()
        )
    }

    // ─── AIRTEL ───────────────────────────────────────────

    private fun parseAirtel(body: String, ts: Long): TransactionEntity? {
        return when {
            body.contains("Quickloan", ignoreCase = true) &&
                body.contains("deposited into your Airtel money account", ignoreCase = true) ->
                parseAirtelQuickloan(body, ts)

            body.startsWith("CASH DEPOSIT", ignoreCase = true) ->
                parseAirtelCashDeposit(body, ts)

            body.startsWith("WITHDRAWN", ignoreCase = true) ->
                parseAirtelWithdrawal(body, ts)

            body.startsWith("Topup successful", ignoreCase = true) ->
                parseAirtelTopup(body, ts)

            body.contains("has collected UGX", ignoreCase = true) && body.contains("from your account", ignoreCase = true) ->
                parseAirtelLoanCollection(body, ts)

            body.contains("PAID.TID", ignoreCase = true) ||
                body.startsWith("PAID UGX", ignoreCase = true) ->
                parseAirtelPayment(body, ts)

            body.startsWith("SENT.TID", ignoreCase = true) ->
                parseAirtelSentFormatB(body, ts)

            body.contains("SENT UGX", ignoreCase = true) && body.contains("TID", ignoreCase = true) ->
                parseAirtelSentFormatA(body, ts)

            body.startsWith("RECEIVED. TID", ignoreCase = true) || body.startsWith("RECEIVED.TID", ignoreCase = true) ->
                parseAirtelReceivedFormatB(body, ts)

            body.startsWith("RECEIVED UGX", ignoreCase = true) ->
                parseAirtelReceivedFormatA(body, ts)

            else -> null
        }
    }

    private fun parseAirtelQuickloan(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "Quickloan UGX")
        val tid = extractTransactionId(body)
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = TransactionType.AIRTEL_RECEIVED,
            amount = amount ?: 0.0,
            date = ts,
            transactionId = tid,
            description = "Airtel Quickloan"
        )
    }

    private fun parseAirtelCashDeposit(body: String, ts: Long): TransactionEntity {
        val amount = Regex("""CASH DEPOSIT\s+of\s+UGX\s*($AMOUNT)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val balance = extractAfter(body, "Bal UGX")
        val tid = extractTransactionId(body)
        val date = parseAirtelDate(body) ?: ts
        val senderName = Regex("""from\s+(.+?)(?:\.\s+Bal|\s+Bal|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim()?.replace(Regex("""\s+"""), " ") ?: ""
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = TransactionType.AIRTEL_RECEIVED,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = date,
            transactionId = tid,
            description = if (senderName.isNotBlank()) "Cash deposit from $senderName" else "Cash deposit"
        )
    }

    private fun parseAirtelWithdrawal(body: String, ts: Long): TransactionEntity {
        val amount = Regex("""WITHDRAWN\.\s*TID\s*[A-Za-z0-9-]+\.\s*UGX\s*($AMOUNT)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
            ?: extractAfterFirst(body, "UGX")
        val fee = extractFee(body)
        val balance = extractAfter(body, "Bal UGX")
        val tid = extractTransactionId(body)
        val date = parseAirtelDate(body) ?: ts
        val agent = Regex("""Agent ID:\s*([A-Za-z0-9-]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = TransactionType.WITHDRAWAL,
            amount = amount ?: 0.0,
            fee = fee,
            balance = balance?.toDouble(),
            date = date,
            transactionId = tid,
            description = if (agent != null) "Cash withdrawal - agent $agent" else "Cash withdrawal"
        )
    }

    private fun parseAirtelTopup(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "Topup successful: UGX")
        val balance = extractAfter(body, "Bal: UGX")
        val phone = Regex("""for\s+([+\d]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = TransactionType.BUNDLE_PURCHASE,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            description = if (phone != null) "Airtime topup - $phone" else "Airtime topup"
        )
    }

    private fun parseAirtelPayment(body: String, ts: Long): TransactionEntity {
        val tid = extractTransactionId(body)
        val amount = if (body.startsWith("PAID UGX", ignoreCase = true)) {
            extractAfter(body, "PAID UGX")
        } else {
            extractAfterFirst(body, "UGX", afterTid = true)
        }
        val fee = extractAfter(body, "Charge UGX") ?: 0.0
        val balance = extractAfter(body, "Bal UGX")
        val date = parseAirtelDateAfterKeyword(body, "Date:") ?: parseAirtelDate(body) ?: ts
        val payee = Regex("""to\s+(.+?)(?:\s+Charge|\s+Bal|\s+Date|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: "Merchant"
        val type = if (payee.contains("UEDCL", ignoreCase = true) || payee.contains("UMEME", ignoreCase = true))
            TransactionType.ELECTRICITY else TransactionType.AIRTEL_PAYMENT
        val desc = if (type == TransactionType.ELECTRICITY) "Electricity - $payee" else "Payment to $payee"
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = type,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = date,
            transactionId = tid,
            description = desc
        )
    }

    private fun parseAirtelSentFormatA(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "SENT UGX")
        val fee = extractAfter(body, "Fee UGX") ?: 0.0
        val balance = extractAfter(body, "Bal UGX")
        val tid = extractTransactionId(body)
        val recipient = Regex("""to\s+(.+?)(?:\s+Fee|\s+Bal|\s+TID|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: "Unknown"
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = TransactionType.AIRTEL_SENT,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = ts,
            transactionId = tid,
            description = "Sent to $recipient"
        )
    }

    private fun parseAirtelSentFormatB(body: String, ts: Long): TransactionEntity {
        val tid = extractTransactionId(body)
        val amount = Regex("""UGX\s*([\d,]+)\s*to""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val fee = extractAfter(body, "Fee UGX") ?: 0.0
        val balance = extractAfter(body, "Bal UGX")
        val date = parseAirtelDateAfterKeyword(body, "Date") ?: ts
        val recipient = Regex("""to\s+(.+?)(?:\s+Fee|\s+Bal|\s+Date|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: "Unknown"
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = TransactionType.AIRTEL_SENT,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = date,
            transactionId = tid,
            description = "Sent to $recipient"
        )
    }

    private fun parseAirtelReceivedFormatA(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "RECEIVED UGX")
        val balance = extractAfter(body, "Bal UGX") ?: extractAfter(body, "Balance UGX")
        val tid = extractTransactionId(body)
        val senderName = Regex("""from\s+(.+?)(?:\s+Bal|\s+Balance|\s+TID|\s+Trans|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: ""
        val desc = if (senderName.isNotBlank()) "Received from $senderName" else "Received"
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = TransactionType.AIRTEL_RECEIVED,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = tid,
            description = desc
        )
    }

    private fun parseAirtelReceivedFormatB(body: String, ts: Long): TransactionEntity {
        val tid = extractTransactionId(body)
        val amount = extractAfterFirst(body, "UGX", afterTid = true)
        val balance = extractAfter(body, "Bal UGX")
        val senderInfo = Regex("""from\s+(.+?)(?:\s+Bal|\s+Date|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: ""
        val desc = if (senderInfo.contains("AIRTEL MONEY AIRTEL MONEY", ignoreCase = true))
            "Airtel Interest"
        else if (senderInfo.isNotBlank()) "Received from $senderInfo"
        else "Received"
        val type = if (desc == "Airtel Interest") TransactionType.AIRTEL_INTEREST
            else TransactionType.AIRTEL_RECEIVED
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = type,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = tid,
            description = desc
        )
    }

    private fun parseAirtelLoanCollection(body: String, ts: Long): TransactionEntity {
        val amount = extractAfter(body, "collected UGX")
        val balance = extractAfter(body, "Bal UGX")
        val tid = extractTransactionId(body)
        val date = parseAirtelDateAfterKeyword(body, "Date:") ?: ts
        val lender = Regex("""(.+?)\s+has collected""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: "Lender"
        return TransactionEntity(
            provider = Provider.AIRTEL,
            type = TransactionType.LOAN_COLLECTION,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = date,
            transactionId = tid,
            description = "$lender loan collection"
        )
    }

    // ─── Helpers ──────────────────────────────────────────

    private fun extractAfter(body: String, prefix: String): Double? {
        val idx = body.indexOf(prefix, ignoreCase = true)
        if (idx < 0) return null
        val after = body.substring(idx + prefix.length).trimStart()
        return Regex("""^[\s:]*(?:UGX\s*)?($AMOUNT)""", RegexOption.IGNORE_CASE)
            .find(after)?.groupValues?.get(1)?.amountToDouble()
    }

    private fun extractAfterFirst(body: String, prefix: String, afterTid: Boolean = false): Double? {
        val searchIn = if (afterTid) {
            val tidEnd = Regex("""TID\s*\S+""", RegexOption.IGNORE_CASE).find(body)?.range?.last
            if (tidEnd != null) body.substring(tidEnd) else body
        } else body
        val idx = searchIn.indexOf(prefix, ignoreCase = true)
        if (idx < 0) return null
        val after = searchIn.substring(idx + prefix.length).trimStart()
        return Regex("""^[\s:]*(?:UGX\s*)?($AMOUNT)""", RegexOption.IGNORE_CASE)
            .find(after)?.groupValues?.get(1)?.amountToDouble()
    }

    private fun extractFee(body: String): Double {
        val fee = Regex(
            """(?:Fee[:\s]*UGX|Fee\s+UGX|TX\s+Charge\s+UGX|Charge\s+UGX|fee\s+of\s+UGX|at\s+a\s+fee\s+of\s+UGX)\s*($AMOUNT)""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.amountToDouble()
        return fee ?: 0.0
    }

    private fun extractTransactionId(body: String): String? {
        return Regex(
            """(?:Financial Transaction Id|Transaction ID|Transaction Id|TxnID|Trans ID|TID|ID):?\s*([A-Za-z0-9-]+)""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
    }

    private fun extractBetween(body: String, start: String, end: String): String? {
        val startIdx = body.indexOf(start, ignoreCase = true)
        if (startIdx < 0) return null
        val from = startIdx + start.length
        val endIdx = body.indexOf(end, from, ignoreCase = true)
        return if (endIdx > from) body.substring(from, endIdx).trim() else body.substring(from).trim()
    }

    private fun parseMtnDate(body: String): Long? {
        val match = mtnDateRegex.find(body) ?: return null
        return try { synchronized(mtnDateFmt) { mtnDateFmt.parse(match.value)?.time } } catch (_: Exception) { null }
    }

    private fun parseAirtelDate(body: String): Long? {
        val match = airtelDateRegex.find(body) ?: return null
        return try { synchronized(airtelDateFmt) { airtelDateFmt.parse(match.value)?.time } } catch (_: Exception) { null }
    }

    private fun parseAirtelDateAfterKeyword(body: String, keyword: String): Long? {
        val idx = body.indexOf(keyword, ignoreCase = true)
        if (idx < 0) return null
        val after = body.substring(idx + keyword.length)
        val match = airtelDateRegex.find(after) ?: return null
        return try { synchronized(airtelDateFmt) { airtelDateFmt.parse(match.value)?.time } } catch (_: Exception) { null }
    }

    private fun String.amountToDouble(): Double? = replace(",", "").toDoubleOrNull()
}
