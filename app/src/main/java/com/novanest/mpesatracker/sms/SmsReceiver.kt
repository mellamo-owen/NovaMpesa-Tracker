package com.novanest.mpesatracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.Transaction
import com.novanest.mpesatracker.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SmsReceiver : BroadcastReceiver() {
    private val dbMutex = Mutex()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (smsMessage in messages) {
            val sender = smsMessage.originatingAddress ?: continue
            val messageBody = smsMessage.messageBody ?: continue

            Log.d("SmsReceiver", "SMS Received from: $sender")

            if (!sender.contains("MPESA", ignoreCase = true)) continue

            val parsedData = MpesaParser.parse(messageBody)

            if (parsedData != null) {
                Log.d("SmsReceiver", "Parsed successfully: Amount=${parsedData.amount}, Recipient=${parsedData.recipient}")
                val db = AppDatabase.get(context)
                CoroutineScope(Dispatchers.IO).launch {
                    dbMutex.withLock {
                        if (parsedData.txnCode != null && db.transactionDao().findByTxnCode(parsedData.txnCode) != null) return@withLock
                        
                        val newTransaction = Transaction(
                            amount = parsedData.amount, recipient = parsedData.recipient,
                            balance = parsedData.balance, txnCode = parsedData.txnCode,
                            categoryId = null, timestamp = parsedData.timestamp,
                            rawSms = messageBody, isManual = false
                        )
                        val id = db.transactionDao().insert(newTransaction)
                        NotificationHelper.showTransactionNotification(context, id, newTransaction.amount, newTransaction.recipient)
                    }
                }
            } else {
                Log.e("SmsReceiver", "Failed to parse SMS body.")
            }
        }
    }
}
