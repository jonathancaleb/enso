package com.example.enso.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.enso.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val wantedSenders = setOf("mtnmobmoney", "airtelmoney", "airtel")

        val relevant = messages.any { msg ->
            val sender = msg.displayOriginatingAddress?.lowercase() ?: ""
            wantedSenders.any { sender.equals(it, ignoreCase = true) }
        }

        if (relevant) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = AppModule.provideRepository(context)
                    repo.syncFromSms()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
