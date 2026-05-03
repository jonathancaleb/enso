package com.example.enso.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class MoMoSmsParserTest {

    private val fallbackTimestamp = 1000000L
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @Test
    fun `parse withdrawal`() {
        val body = "You have withdrawn UGX 10,000 on 2026-04-26 19:58:18. Fee: UGX 700, Tax: UGX 50. New balance: UGX 57,049.23."
        val result = MoMoSmsParser.parse(body, fallbackTimestamp)

        assertNotNull(result)
        result!!
        assertEquals(TransactionType.WITHDRAWAL, result.type)
        assertEquals(10000.0, result.amount, 0.001)
        assertEquals(700.0, result.fee, 0.001)
        assertEquals(57049.23, result.balance!!, 0.001)
        assertEquals(dateFormat.parse("2026-04-26 19:58:18")!!.time, result.date)
        assertNull(result.counterparty)
    }

    @Test
    fun `parse deposit`() {
        val body = "You have received UGX 27000 from Airtel Money on 2026-04-26 13:19:02. fee:0. Reason: SOPHIE CHELIMO. New balance: UGX 67799."
        val result = MoMoSmsParser.parse(body, fallbackTimestamp)

        assertNotNull(result)
        result!!
        assertEquals(TransactionType.DEPOSIT, result.type)
        assertEquals(27000.0, result.amount, 0.001)
        assertEquals(0.0, result.fee, 0.001)
        assertEquals(67799.0, result.balance!!, 0.001)
        assertEquals(dateFormat.parse("2026-04-26 13:19:02")!!.time, result.date)
        assertEquals("Airtel Money", result.counterparty)
    }

    @Test
    fun `parse transfer`() {
        val body = "Y'ello. You have sent UGX 3,000 to 256750681731, SOPHIE,CHELIMO. Fee:UGX 100.00. Transaction ID:40265187290. Your Mobile Money balance is now UGX 53,949.23."
        val result = MoMoSmsParser.parse(body, fallbackTimestamp)

        assertNotNull(result)
        result!!
        assertEquals(TransactionType.TRANSFER, result.type)
        assertEquals(3000.0, result.amount, 0.001)
        assertEquals(100.0, result.fee, 0.001)
        assertEquals(53949.23, result.balance!!, 0.001)
        assertEquals(fallbackTimestamp, result.date)
        assertEquals("256750681731 SOPHIE,CHELIMO", result.counterparty)
    }

    @Test
    fun `unrecognized message returns null`() {
        val body = "Your airtime balance is UGX 500."
        val result = MoMoSmsParser.parse(body, fallbackTimestamp)
        assertNull(result)
    }

    @Test
    fun `withdrawal uses fallback timestamp when date missing`() {
        val body = "You have withdrawn UGX 5,000 on INVALID_DATE. Fee: UGX 200. New balance: UGX 10,000."
        val result = MoMoSmsParser.parse(body, fallbackTimestamp)
        assertNull(result)
    }
}
