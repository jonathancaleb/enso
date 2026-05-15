package com.example.enso.data.sms

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.enso.data.local.TransactionDao
import com.example.enso.data.local.entity.TransactionEntity
import com.example.enso.data.local.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val TAG = "SmsImportService"

val Context.ensoDataStore: DataStore<Preferences> by preferencesDataStore(name = "enso_prefs")

private val SMS_IMPORTED_KEY = booleanPreferencesKey("sms_imported")
private val ZERO_AMOUNT_ALLOWED_TYPES = TransactionType.zeroAmountAllowedTypes

class SmsImportService(
    private val dao: TransactionDao,
    private val smsReader: SmsReader,
    private val dataStore: DataStore<Preferences>
) {

    suspend fun maybeRunInitialImport(): Int = withContext(Dispatchers.IO) {
        val alreadyDone = dataStore.data.first()[SMS_IMPORTED_KEY] == true
        if (alreadyDone) return@withContext 0
        val inserted = runImport()
        dataStore.edit { it[SMS_IMPORTED_KEY] = true }
        Log.i(TAG, "Initial SMS import complete: $inserted new transactions")
        inserted
    }

    suspend fun runImport(): Int = withContext(Dispatchers.IO) {
        val raw = smsReader.readMomoMessages()
        var inserted = 0
        for (sms in raw) {
            val parsed = MomoSmsParser.parse(sms) ?: continue
            if (insertIfNew(parsed)) inserted++
        }
        inserted
    }

    suspend fun handleIncoming(sms: RawSms): Boolean = withContext(Dispatchers.IO) {
        val parsed = MomoSmsParser.parse(sms) ?: return@withContext false
        insertIfNew(parsed)
    }

    private suspend fun insertIfNew(entity: TransactionEntity): Boolean {
        if (entity.amount <= 0.0 && entity.type !in ZERO_AMOUNT_ALLOWED_TYPES) {
            Log.w(
                TAG,
                "Skipping parsed SMS with non-positive amount. provider=${entity.provider}, " +
                    "type=${entity.type}, raw=${entity.rawMessage}"
            )
            return false
        }

        if (entity.transactionId != null) {
            return dao.insert(entity) > 0
        }
        if (dao.existsByComposite(entity.date, entity.amount, entity.type)) return false
        return dao.insert(entity) > 0
    }
}
