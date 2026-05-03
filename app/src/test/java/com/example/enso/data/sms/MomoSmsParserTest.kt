package com.example.enso.data.sms

import com.example.enso.data.local.entity.Provider
import com.example.enso.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class MomoSmsParserTest {

    private val fallbackTimestamp = 1_000_000L
    private val mtnDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
    private val airtelDateFormat = SimpleDateFormat("dd-MMMM-yyyy HH:mm", Locale.ENGLISH)

    @Test
    fun `parses airtel cash deposit`() {
        val sms = raw(
            sender = "AirtelMoney",
            body = "CASH DEPOSIT of UGX 5,000 from  TEST USER. Bal UGX 5,975. TID 146300121498. 01-May-2026 16:50"
        )

        val result = MomoSmsParser.parse(sms)

        assertNotNull(result)
        result!!
        assertEquals(Provider.AIRTEL, result.provider)
        assertEquals(TransactionType.AIRTEL_RECEIVED, result.type)
        assertEquals(5000.0, result.amount, 0.001)
        assertEquals(5975.0, result.balance!!, 0.001)
        assertEquals("146300121498", result.transactionId)
        assertEquals(airtelDateFormat.parse("01-May-2026 16:50")!!.time, result.date)
    }

    @Test
    fun `parses airtel withdrawal and ignores initiation sms`() {
        val withdrawal = raw(
            sender = "AirtelMoney",
            body = "WITHDRAWN. TID 146295530958. UGX55,000 with Agent ID: 180539.Fee UGX 1,500.Tax UGX 275.Bal UGX 18,975. 01-May-2026 15:46."
        )
        val initiation = raw(
            sender = "AirtelMoney",
            body = "Withdrawal of UGX 55000 initiated. Secret Code: 123456. Expires on 01-May-2026 15:48."
        )

        val result = MomoSmsParser.parse(withdrawal)

        assertNotNull(result)
        result!!
        assertEquals(TransactionType.WITHDRAWAL, result.type)
        assertEquals(55000.0, result.amount, 0.001)
        assertEquals(1500.0, result.fee, 0.001)
        assertEquals(18975.0, result.balance!!, 0.001)
        assertEquals("146295530958", result.transactionId)
        assertNull(MomoSmsParser.parse(initiation))
    }

    @Test
    fun `parses airtel quickloan as incoming`() {
        val sms = raw(
            sender = "AirtelMoney",
            body = "Success! Quickloan UGX 22,451 deposited into your Airtel money account. Please dial *185# to complete your intended transaction. TID 146303591313"
        )

        val result = MomoSmsParser.parse(sms)

        assertNotNull(result)
        result!!
        assertEquals(TransactionType.AIRTEL_RECEIVED, result.type)
        assertEquals(22451.0, result.amount, 0.001)
        assertEquals("146303591313", result.transactionId)
    }

    @Test
    fun `parses airtel paid format without paid tid prefix`() {
        val sms = raw(
            sender = "AirtelMoney",
            body = "PAID UGX 5,000 to TEST MERCHANT Charge UGX 0, TID 145891386684. Bal UGX 1,750 Date: 26-April-2026 10:13."
        )

        val result = MomoSmsParser.parse(sms)

        assertNotNull(result)
        result!!
        assertEquals(TransactionType.AIRTEL_PAYMENT, result.type)
        assertEquals(5000.0, result.amount, 0.001)
        assertEquals(0.0, result.fee, 0.001)
        assertEquals(1750.0, result.balance!!, 0.001)
        assertEquals("145891386684", result.transactionId)
        assertEquals(airtelDateFormat.parse("26-April-2026 10:13")!!.time, result.date)
    }

    @Test
    fun `parses mtn generic merchant deduction`() {
        val sms = raw(
            sender = "MTNMobMoney",
            body = "Y'ello. MTN MOMO VIRTUAL CARD has deducted UGX 5000 at a fee of UGX 0 Transaction ID: 40239242705 from your mobile money account. Message: Card top up. New balance is:UGX 76399. Your Bonus was: 0. Be safe. Do NOT share your Mobile Money PIN."
        )

        val result = MomoSmsParser.parse(sms)

        assertNotNull(result)
        result!!
        assertEquals(Provider.MTN, result.provider)
        assertEquals(TransactionType.MERCHANT_PAYMENT, result.type)
        assertEquals(5000.0, result.amount, 0.001)
        assertEquals(0.0, result.fee, 0.001)
        assertEquals(76399.0, result.balance!!, 0.001)
        assertEquals("40239242705", result.transactionId)
    }

    @Test
    fun `parses mtn deposited format`() {
        val sms = raw(
            sender = "MTNMobMoney",
            body = "You have deposited UGX 52000 from TEST USER on 2026-03-28 15:12:25. New balance: UGX 48938. ID: 39540053824. Do NOT share your Mobile Money PIN."
        )

        val result = MomoSmsParser.parse(sms)

        assertNotNull(result)
        result!!
        assertEquals(TransactionType.DEPOSIT, result.type)
        assertEquals(52000.0, result.amount, 0.001)
        assertEquals(48938.0, result.balance!!, 0.001)
        assertEquals("39540053824", result.transactionId)
        assertEquals(mtnDateFormat.parse("2026-03-28 15:12:25")!!.time, result.date)
    }

    @Test
    fun `parses mtn transfer with repeated currency label`() {
        val sms = raw(
            sender = "MTNMobMoney",
            body = "Y'ello. You have transferred UGX UGX 25,000 to TEST BANK LIMITED. TX Charge  UGX 1500.00. Your new balance: UGX UGX 6,017.74. Transaction ID:35298248926."
        )

        val result = MomoSmsParser.parse(sms)

        assertNotNull(result)
        result!!
        assertEquals(TransactionType.TRANSFER_OUT, result.type)
        assertEquals(25000.0, result.amount, 0.001)
        assertEquals(1500.0, result.fee, 0.001)
        assertEquals(6017.74, result.balance!!, 0.001)
        assertEquals("35298248926", result.transactionId)
    }

    @Test
    fun `ignores failed mtn transaction`() {
        val sms = raw(
            sender = "MTNMobMoney",
            body = "Y'ello, the transaction with amount 2580 UGX for MTN UGANDA LIMITED with message: Test failed at 2026-04-08 19:43:02, Reason: Expired. Financial Transaction Id: 39818522229."
        )

        assertNull(MomoSmsParser.parse(sms))
    }

    private fun raw(sender: String, body: String): RawSms {
        return RawSms(sender = sender, body = body, date = fallbackTimestamp)
    }
}
