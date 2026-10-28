package com.novanest.mpesatracker.sms

data class ParsedSms(
    val amount: Double,
    val recipient: String,
    val txnCode: String?,
    val balance: Double?,
    val timestamp: Long
)

object MpesaParser {
    private val codeRegex = Regex("^([A-Z0-9]{10}) Confirmed", RegexOption.IGNORE_CASE)
    private val amountRegex = Regex("Ksh\\s*([0-9,]+\\.?[0-9]*)", RegexOption.IGNORE_CASE)
    private val balanceRegex = Regex("balance is Ksh\\s*([0-9,]+\\.?[0-9]*)", RegexOption.IGNORE_CASE)

    fun parse(smsText: String): ParsedSms? {
        if (!smsText.contains("Confirmed", ignoreCase = true) && !smsText.contains("M-PESA", ignoreCase = true)) {
            return null
        }

        val codeMatch = codeRegex.find(smsText.trim())
        val txnCode = codeMatch?.groupValues?.get(1)

        val amountMatch = amountRegex.find(smsText)
        val amountString = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: return null
        val amount = amountString.toDoubleOrNull() ?: return null

        val recipientRegex = Regex("(?:paid to|sent to)\\s+(.+?)\\s+on\\s", RegexOption.IGNORE_CASE)
        val recipientMatch = recipientRegex.find(smsText)
        val rawRecipient = recipientMatch?.groupValues?.get(1)?.replace("\\s+".toRegex(), " ") ?: "Unknown"
        val recipient = rawRecipient.trim()

        val balanceMatch = balanceRegex.find(smsText)
        val balance = balanceMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        return ParsedSms(amount, recipient, txnCode, balance, System.currentTimeMillis())
    }
}
