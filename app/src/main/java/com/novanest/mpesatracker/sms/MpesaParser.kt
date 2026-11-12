package com.novanest.mpesatracker.sms

data class ParsedSms(
    val amount: Double,
    val recipient: String,
    val timestamp: Long,
    val transactionCost: Double,
    val isMoneyIn: Boolean
)

object MpesaParser {
    private val amountRegex = Regex("Ksh\\s*([0-9,]+\\.?[0-9]*)", RegexOption.IGNORE_CASE)
    private val costRegex = Regex("Transaction cost,?\\s*Ksh\\s*([0-9,]+\\.?[0-9]*)", RegexOption.IGNORE_CASE)

    fun parse(smsText: String): ParsedSms? {
        if (!smsText.contains("Confirmed", ignoreCase = true)) return null
        if (smsText.contains("insufficient", ignoreCase = true) || smsText.contains("failed", ignoreCase = true)) return null

        val amountMatch = amountRegex.find(smsText)
        val amountString = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: return null
        val amount = amountString.toDoubleOrNull() ?: return null

        // 1. MONEY IN: Received Money
        if (smsText.contains("received", ignoreCase = true)) {
            val senderRegex = Regex("from\\s+(.+?)\\s+on\\s", RegexOption.IGNORE_CASE)
            val sender = senderRegex.find(smsText)?.groupValues?.get(1)?.replace(Regex("\\s+"), " ")?.trim() ?: "Unknown"
            return ParsedSms(amount, sender, System.currentTimeMillis(), 0.0, true)
        }

        // 2. MONEY IN: Shop Deposit ("Give Ksh X to [Name]")
        if (smsText.contains("Give", ignoreCase = true)) {
            val agentRegex = Regex("Give\\s+Ksh\\s*[0-9,]+\\s+to\\s+(.+?)\\s+", RegexOption.IGNORE_CASE)
            val agent = agentRegex.find(smsText)?.groupValues?.get(1)?.replace(Regex("\\s+"), " ")?.trim() ?: "M-Pesa Agent"
            return ParsedSms(amount, agent, System.currentTimeMillis(), 0.0, true)
        }

        // 3. MONEY OUT: Sent or Paid
        val isSent = smsText.contains("sent to", ignoreCase = true) || smsText.contains("paid to", ignoreCase = true)
        if (!isSent && !smsText.contains("Confirmed", ignoreCase = true)) return null

        val costMatch = costRegex.find(smsText)
        val costString = costMatch?.groupValues?.get(1)?.replace(",", "") ?: "0.0"
        val cost = costString.toDoubleOrNull() ?: 0.0

        val recipientRegex = Regex("(?:paid to|sent to)\\s+(.+?)\\s+on\\s", RegexOption.IGNORE_CASE)
        val recipientMatch = recipientRegex.find(smsText)
        var rawRecipient = recipientMatch?.groupValues?.get(1) ?: "Unknown"
        rawRecipient = rawRecipient.replace(Regex("\\s+"), " ").trim()

        return ParsedSms(amount, rawRecipient, System.currentTimeMillis(), cost, false)
    }
}
