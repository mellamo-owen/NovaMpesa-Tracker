package com.novanest.mpesatracker.sms
data class ParsedSms(val amount: Double, val recipient: String, val timestamp: Long, val transactionCost: Double)

object MpesaParser {
    private val amountRegex = Regex("Ksh\\s*([0-9,]+\\.?[0-9]*)", RegexOption.IGNORE_CASE)
    // Matches "Transaction cost, Ksh7.00" or "Transaction cost, Ksh 7.00"
    private val costRegex = Regex("Transaction cost,?\\s*Ksh\\s*([0-9,]+\\.?[0-9]*)", RegexOption.IGNORE_CASE)

    fun parse(smsText: String): ParsedSms? {
        if (!smsText.contains("Confirmed", ignoreCase = true)) return null
        if (smsText.contains("received", ignoreCase = true)) return null
        if (smsText.contains("insufficient", ignoreCase = true) || smsText.contains("failed", ignoreCase = true)) return null

        val isSent = smsText.contains("sent to", ignoreCase = true) || smsText.contains("paid to", ignoreCase = true)
        if (!isSent) return null

        // Extract Amount
        val amountMatch = amountRegex.find(smsText)
        val amountString = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: return null
        val amount = amountString.toDoubleOrNull() ?: return null

        // Extract Transaction Cost
        val costMatch = costRegex.find(smsText)
        val costString = costMatch?.groupValues?.get(1)?.replace(",", "") ?: "0.0"
        val cost = costString.toDoubleOrNull() ?: 0.0

        // Extract Recipient (Cleans up double spaces)
        val recipientRegex = Regex("(?:paid to|sent to)\\s+(.+?)\\s+on\\s", RegexOption.IGNORE_CASE)
        val recipientMatch = recipientRegex.find(smsText)
        var rawRecipient = recipientMatch?.groupValues?.get(1) ?: "Unknown"
        // Remove double spaces
        rawRecipient = rawRecipient.replace(Regex("\\s+"), " ").trim()

        return ParsedSms(amount, rawRecipient, System.currentTimeMillis(), cost)
    }
}
