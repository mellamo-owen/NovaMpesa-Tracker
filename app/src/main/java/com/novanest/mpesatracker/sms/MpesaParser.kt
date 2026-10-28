package com.novanest.mpesatracker.sms
data class ParsedSms(val amount: Double, val recipient: String, val timestamp: Long)

object MpesaParser {
    private val amountRegex = Regex("Ksh\\s*([0-9,]+\\.?[0-9]*)", RegexOption.IGNORE_CASE)

    fun parse(smsText: String): ParsedSms? {
        if (!smsText.contains("Confirmed", ignoreCase = true)) return null
        if (smsText.contains("received", ignoreCase = true)) return null
        if (smsText.contains("insufficient", ignoreCase = true) || smsText.contains("failed", ignoreCase = true)) return null

        val isSent = smsText.contains("sent to", ignoreCase = true) || smsText.contains("paid to", ignoreCase = true)
        if (!isSent) return null

        val amountMatch = amountRegex.find(smsText)
        val amountString = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: return null
        val amount = amountString.toDoubleOrNull() ?: return null

        val recipientRegex = Regex("(?:paid to|sent to)\\s+(.+?)\\s+on\\s", RegexOption.IGNORE_CASE)
        val recipientMatch = recipientRegex.find(smsText)
        val rawRecipient = recipientMatch?.groupValues?.get(1)?.replace("\\s+".toRegex(), " ") ?: "Unknown"

        return ParsedSms(amount, rawRecipient.trim(), System.currentTimeMillis())
    }
}
