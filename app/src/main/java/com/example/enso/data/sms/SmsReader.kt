package com.example.enso.data.sms

import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony

data class DebugSms(
    val sender: String,
    val body: String,
    val date: Long
)

class SmsReader(private val contentResolver: ContentResolver) {

    companion object {
        private val WANTED_SENDERS = listOf("MTNMobMoney", "AirtelMoney", "Airtel")

        private val IGNORED_SENDERS = setOf(
            "AIRTEL", "MTNInternet", "MTNKirabo", "MTNBundles", "MTN-For-You",
            "MTNNotice", "MTNYinvesta", "MoMoAdvance", "PayWithMoMo", "MoSente",
            "AM_Loans", "Yinvesta", "BetSport", "StanbicBank"
        )
    }

    fun readDebugMessages(): List<DebugSms> {
        return queryMessages { sender, body, date ->
            DebugSms(sender = sender, body = body, date = date)
        }
    }

    fun readMomoMessages(): List<RawSms> {
        return queryMessages { sender, body, date ->
            RawSms(sender = sender, body = body, date = date)
        }
    }

    private fun <T> queryMessages(map: (sender: String, body: String, date: Long) -> T): List<T> {
        val messages = mutableListOf<T>()
        val uri: Uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = WANTED_SENDERS.joinToString(" OR ") {
            "UPPER(${Telephony.Sms.ADDRESS}) = UPPER(?)"
        }
        val selectionArgs = WANTED_SENDERS.toTypedArray()

        val cursor = contentResolver.query(
            uri, projection, selection, selectionArgs, "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            val addrIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (it.moveToNext()) {
                val sender = it.getString(addrIdx) ?: "null"
                if (IGNORED_SENDERS.any { ignored -> ignored.equals(sender, ignoreCase = true) }) continue
                val body = it.getString(bodyIdx) ?: continue
                val date = it.getLong(dateIdx)
                messages.add(map(sender, body, date))
            }
        }

        return messages
    }
}
