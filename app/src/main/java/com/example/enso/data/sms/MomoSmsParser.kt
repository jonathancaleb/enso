package com.example.enso.data.sms

import android.util.Log
import com.example.enso.data.local.entity.Provider
import com.example.enso.data.local.entity.Transaction
import com.example.enso.data.local.entity.TransactionType
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

data class RawSms(
    val sender: String,
    val body: String,
    val date: Long
)

object MomoSmsParser {

    private const val TAG = "MomoSmsParser"

    private val SILENT_IGNORE = listOf(
        "Please enter the following code",
        "preapproval with identity",
        "WRONG PIN",
        "select My Approvals",
        "Your Current Balance is",
        "You've purchased Electricity",
        "Debit with Transaction with ID",
        "You have been debited",
        "Do NOT share your Mobile Money PIN",
        "all Airtel Money services have been",
        "Move money from your bank account"
    )

    private val FAILED_PATTERNS = listOf(
        "has failed at",
        "FAILED. Insufficient funds",
        "does not have sufficient money"
    )

    // yyyy-MM-dd HH:mm:ss
    private val mtnDateRegex = Regex("""\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}""")
    private val mtnDateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    // dd-MMM-yyyy HH:mm  (e.g. 02-May-2026 15:29)
    private val airtelDateRegex = Regex("""\d{2}-[A-Za-z]{3,9}-\d{4}\s+\d{2}:\d{2}""")
    private val airtelDateFmt = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.ENGLISH)

    fun parse(sms: RawSms): Transaction? {
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

        if (result == null) {
            Log.w(TAG, "Unparsed [${sender}]: ${body.take(250)}")
        }

        return result?.copy(rawSms = body, smsHash = hashSms(body, sms.date))
    }

    private fun resolveProvider(sender: String): Provider? {
        if (sender.contains("MTN", ignoreCase = true)) return Provider.MTN
        if (sender.equals("AirtelMoney", ignoreCase = true) ||
            sender.equals("Airtel", ignoreCase = true)) return Provider.AIRTEL
        return null
    }

    private fun shouldSilentIgnore(body: String): Boolean {
        if (body.startsWith("http", ignoreCase = true)) return true
        for (pattern in SILENT_IGNORE) {
            if (body.contains(pattern, ignoreCase = true)) {
                if (pattern == "Do NOT share your Mobile Money PIN") {
                    return !Regex("""UGX\s*[\d,]+|[\d,]+\s*UGX""", RegexOption.IGNORE_CASE).containsMatchIn(body)
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

    // ─── MTN ──────────────────────────────────────────────

    private fun parseMtn(body: String, ts: Long): Transaction? {
        return when {
            body.contains("You have withdrawn", ignoreCase = true) ->
                parseMtnWithdrawal(body, ts)

            body.contains("You have received", ignoreCase = true) ->
                parseMtnDeposit(body, ts)

            body.contains("You have sent", ignoreCase = true) ->
                parseMtnTransferOut(body, ts)

            body.contains("has deducted UGX", ignoreCase = true) && body.contains("Bundle Purchase", ignoreCase = true) ->
                parseMtnBundlePurchase(body, ts)

            body.contains("has deducted UGX", ignoreCase = true) && body.contains("Payment for services", ignoreCase = true) ->
                parseMtnMerchantPayment(body, ts)

            body.contains("from MOMOADVANCE", ignoreCase = true) ->
                parseMtnMomoAdvance(body, ts)

            body.contains("You have paid", ignoreCase = true) ->
                parseMtnMomoPay(body, ts)

            body.contains("has been used to repay your OVERDRAFT", ignoreCase = true) ->
                parseMtnOverdraftRepay(body, ts)

            body.contains("for UEDCL", ignoreCase = true) && body.contains("Account Number", ignoreCase = true) ->
                parseMtnElectricity(body, ts)

            else -> null
        }
    }

    private fun parseMtnWithdrawal(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "withdrawn UGX")
        val fee = extractAfter(body, "Fee: UGX") ?: extractAfter(body, "Fee:UGX") ?: 0.0
        val balance = extractAfter(body, "New balance: UGX") ?: extractAfter(body, "New balance:UGX")
        val date = parseMtnDate(body) ?: ts
        return Transaction(
            provider = Provider.MTN,
            type = TransactionType.WITHDRAWAL,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = date,
            description = "Cash withdrawal"
        )
    }

    private fun parseMtnDeposit(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "received UGX")
        val balance = extractAfter(body, "New balance: UGX") ?: extractAfter(body, "New balance:UGX")
        val date = parseMtnDate(body) ?: ts
        val reason = extractBetween(body, "Reason:", ".") ?: ""
        val desc = if (reason.isNotBlank()) "Received - ${reason.trim()}" else "Received"
        return Transaction(
            provider = Provider.MTN,
            type = TransactionType.DEPOSIT,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = date,
            description = desc
        )
    }

    private fun parseMtnTransferOut(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "sent UGX")
        val balance = extractAfter(body, "balance is now UGX") ?: extractAfter(body, "New balance: UGX")
        val fee = extractAfter(body, "Fee:UGX") ?: extractAfter(body, "Fee: UGX") ?: 0.0
        val recipient = extractBetween(body, "sent UGX", ".") ?.let { segment ->
            Regex("""to\s+(.+?)(?:\.|$)""", RegexOption.IGNORE_CASE).find(segment)?.groupValues?.get(1)?.trim()
        } ?: "Unknown"
        return Transaction(
            provider = Provider.MTN,
            type = TransactionType.TRANSFER_OUT,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = ts,
            description = "Sent to $recipient"
        )
    }

    private fun parseMtnMerchantPayment(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "deducted UGX")
        val balance = extractAfter(body, "New balance is:UGX") ?: extractAfter(body, "New balance is: UGX")
            ?: extractAfter(body, "New balance: UGX")
        val txnId = extractBetween(body, "Transaction ID:", ".") ?: extractBetween(body, "Transaction ID:", "\n")
        val merchant = extractBetween(body, "Y'ello.", "has deducted")?.trim() ?: "Merchant"
        return Transaction(
            provider = Provider.MTN,
            type = TransactionType.MERCHANT_PAYMENT,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = txnId?.trim(),
            description = "Payment - $merchant"
        )
    }

    private fun parseMtnBundlePurchase(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "deducted UGX")
        val balance = extractAfter(body, "New balance is:UGX") ?: extractAfter(body, "New balance is: UGX")
            ?: extractAfter(body, "New balance: UGX")
        val txnId = extractBetween(body, "Transaction ID:", ".") ?: extractBetween(body, "Transaction ID:", "\n")
        val merchant = extractBetween(body, "Y'ello.", "has deducted")?.trim() ?: "MTN"
        return Transaction(
            provider = Provider.MTN,
            type = TransactionType.BUNDLE_PURCHASE,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = txnId?.trim(),
            description = "Bundle purchase - $merchant"
        )
    }

    private fun parseMtnMomoAdvance(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "used UGX")
        val fee = extractAfter(body, "access fee UGX") ?: 0.0
        val balance = extractAfter(body, "MOMOADVANCE balance is UGX")
        val date = parseMtnDate(body) ?: ts
        val txnId = extractBetween(body, "Transaction Id:", ".") ?: extractBetween(body, "Transaction Id:", "\n")
            ?: extractBetween(body, "Transaction ID:", ".")
        return Transaction(
            provider = Provider.MTN,
            type = TransactionType.MOMO_ADVANCE,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = date,
            transactionId = txnId?.trim(),
            description = "MoMo Advance"
        )
    }

    private fun parseMtnMomoPay(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "paid UGX") ?: extractAfterFirst(body, "UGX")
        val balance = extractAfter(body, "New balance: UGX") ?: extractAfter(body, "New balance is:UGX")
            ?: extractAfter(body, "New balance is: UGX")
        val fee = extractAfter(body, "Fee: UGX") ?: extractAfter(body, "Fee:UGX") ?: 0.0
        val date = parseMtnDate(body) ?: ts
        val recipient = Regex("""paid\s+(?:UGX\s*[\d,]+\s+to\s+)?(.+?)(?:\.|New|Fee|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim()
            ?.replace(Regex("""UGX\s*[\d,]+\s*to\s*"""), "")?.trim()
            ?: "Unknown"
        return Transaction(
            provider = Provider.MTN,
            type = TransactionType.MERCHANT_PAYMENT,
            amount = amount ?: 0.0,
            fee = fee.toDouble(),
            balance = balance?.toDouble(),
            date = date,
            description = "Paid $recipient"
        )
    }

    private fun parseMtnOverdraftRepay(body: String, ts: Long): Transaction {
        val amount = Regex("""Y'ello\.\s*UGX\s*([\d,]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val date = parseMtnDate(body) ?: ts
        val txnId = extractBetween(body, "Transaction ID", ".") ?: extractBetween(body, "Transaction ID", "\n")
        return Transaction(
            provider = Provider.MTN,
            type = TransactionType.OVERDRAFT_REPAY,
            amount = amount ?: 0.0,
            date = date,
            transactionId = txnId?.trim()?.removePrefix(":"),
            description = "Overdraft repayment"
        )
    }

    private fun parseMtnElectricity(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "paid UGX")
        val fee = Regex("""fee\s+of\s+UGX\s*([\d,]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val balance = extractAfter(body, "New balance is UGX") ?: extractAfter(body, "New balance is:UGX")
        val txnId = Regex("""ID:\s*(\S+)""").find(body)?.groupValues?.get(1)
        val meter = Regex("""Account Number[:\s]*(\S+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1) ?: ""
        return Transaction(
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

    private fun parseAirtel(body: String, ts: Long): Transaction? {
        return when {
            body.contains("has collected UGX", ignoreCase = true) && body.contains("from your account", ignoreCase = true) ->
                parseAirtelLoanCollection(body, ts)

            body.contains("PAID.TID", ignoreCase = true) ->
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

    private fun parseAirtelPayment(body: String, ts: Long): Transaction {
        val tid = Regex("""PAID\.TID\s*(\S+?)[\.\s]""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
        val amount = extractAfterFirst(body, "UGX", afterTid = true)
        val fee = extractAfter(body, "Charge UGX") ?: 0.0
        val balance = extractAfter(body, "Bal UGX")
        val date = parseAirtelDate(body) ?: ts
        val payee = Regex("""to\s+(.+?)(?:\s+Charge|\s+Bal|\s+Date|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: "Merchant"
        val type = if (payee.contains("UEDCL", ignoreCase = true) || payee.contains("UMEME", ignoreCase = true))
            TransactionType.ELECTRICITY else TransactionType.AIRTEL_PAYMENT
        val desc = if (type == TransactionType.ELECTRICITY) "Electricity - $payee" else "Payment to $payee"
        return Transaction(
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

    private fun parseAirtelSentFormatA(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "SENT UGX")
        val fee = extractAfter(body, "Fee UGX") ?: 0.0
        val balance = extractAfter(body, "Bal UGX")
        val tid = Regex("""TID\s+(\S+)""").find(body)?.groupValues?.get(1)
        val recipient = Regex("""to\s+(.+?)(?:\s+Fee|\s+Bal|\s+TID|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: "Unknown"
        return Transaction(
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

    private fun parseAirtelSentFormatB(body: String, ts: Long): Transaction {
        val tid = Regex("""SENT\.TID\s*(\S+?)[\.\s]""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
        val amount = Regex("""UGX\s*([\d,]+)\s*to""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.amountToDouble()
        val fee = extractAfter(body, "Fee UGX") ?: 0.0
        val balance = extractAfter(body, "Bal UGX")
        val date = parseAirtelDateAfterKeyword(body, "Date") ?: ts
        val recipient = Regex("""to\s+(.+?)(?:\s+Fee|\s+Bal|\s+Date|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: "Unknown"
        return Transaction(
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

    private fun parseAirtelReceivedFormatA(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "RECEIVED UGX")
        val balance = extractAfter(body, "Bal UGX") ?: extractAfter(body, "Balance UGX")
        val tid = Regex("""(?:TID|Trans ID:?)\s*(\S+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
        val senderName = Regex("""from\s+(.+?)(?:\s+Bal|\s+Balance|\s+TID|\s+Trans|$)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: ""
        val desc = if (senderName.isNotBlank()) "Received from $senderName" else "Received"
        return Transaction(
            provider = Provider.AIRTEL,
            type = TransactionType.AIRTEL_RECEIVED,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = tid,
            description = desc
        )
    }

    private fun parseAirtelReceivedFormatB(body: String, ts: Long): Transaction {
        val tid = Regex("""(?:RECEIVED\.\s*TID|RECEIVED\.TID)\s*(\S+?)[\.\s]""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
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
        return Transaction(
            provider = Provider.AIRTEL,
            type = type,
            amount = amount ?: 0.0,
            balance = balance?.toDouble(),
            date = ts,
            transactionId = tid,
            description = desc
        )
    }

    private fun parseAirtelLoanCollection(body: String, ts: Long): Transaction {
        val amount = extractAfter(body, "collected UGX")
        val balance = extractAfter(body, "Bal UGX")
        val tid = Regex("""TxnID:\s*(\S+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)
        val date = parseAirtelDateAfterKeyword(body, "Date:") ?: ts
        val lender = Regex("""(.+?)\s+has collected""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim() ?: "Lender"
        return Transaction(
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
        return Regex("""^[\s:]*([\d,]+)""").find(after)?.groupValues?.get(1)?.amountToDouble()
    }

    private fun extractAfterFirst(body: String, prefix: String, afterTid: Boolean = false): Double? {
        val searchIn = if (afterTid) {
            val tidEnd = Regex("""TID\s*\S+""", RegexOption.IGNORE_CASE).find(body)?.range?.last
            if (tidEnd != null) body.substring(tidEnd) else body
        } else body
        val idx = searchIn.indexOf(prefix, ignoreCase = true)
        if (idx < 0) return null
        val after = searchIn.substring(idx + prefix.length).trimStart()
        return Regex("""^[\s:]*([\d,]+)""").find(after)?.groupValues?.get(1)?.amountToDouble()
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
        return try { mtnDateFmt.parse(match.value)?.time } catch (_: Exception) { null }
    }

    private fun parseAirtelDate(body: String): Long? {
        val match = airtelDateRegex.find(body) ?: return null
        return try { airtelDateFmt.parse(match.value)?.time } catch (_: Exception) { null }
    }

    private fun parseAirtelDateAfterKeyword(body: String, keyword: String): Long? {
        val idx = body.indexOf(keyword, ignoreCase = true)
        if (idx < 0) return null
        val after = body.substring(idx + keyword.length)
        val match = airtelDateRegex.find(after) ?: return null
        return try { airtelDateFmt.parse(match.value)?.time } catch (_: Exception) { null }
    }

    private fun String.amountToDouble(): Double? = replace(",", "").toDoubleOrNull()

    fun hashSms(body: String, date: Long): String {
        val input = "$body|$date"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
