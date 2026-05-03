package com.example.enso.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.enso.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: return
        if (!isMomoSender(sender)) return

        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val date = messages[0].timestampMillis
        val raw = RawSms(sender = sender, body = body, date = date)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AppModule.provideSmsImportService(context).handleIncoming(raw)
            } finally {
                pending.finish()
            }
        }
    }

    private fun isMomoSender(sender: String): Boolean {
        return sender.equals("MTNMobMoney", ignoreCase = true) ||
            sender.equals("AirtelMoney", ignoreCase = true) ||
            sender.equals("Airtel", ignoreCase = true)
    }
}
