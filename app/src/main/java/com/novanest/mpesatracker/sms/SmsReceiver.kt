package com.novanest.mpesatracker.sms
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.Transaction
import com.novanest.mpesatracker.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            val sender = msg.originatingAddress ?: ""
            val body = msg.messageBody ?: ""

            // Only process M-Pesa
            if (!sender.contains("MPESA", ignoreCase = true)) continue
            
            val parsed = MpesaParser.parse(body) ?: continue

            // We show the notification IMMEDIATELY before saving to the database
            // This makes the notification pop up instantly when the SMS arrives
            NotificationHelper.showTransactionNotification(context, 0L, parsed.amount, parsed.recipient)

            // Then we save to the database in the background
            val db = AppDatabase.get(context)
            CoroutineScope(Dispatchers.IO).launch {
                db.transactionDao().insert(
                    Transaction(
                        amount = parsed.amount,
                        recipient = parsed.recipient,
                        categoryId = null,
                        timestamp = parsed.timestamp,
                        isLoan = false,
                        transactionCost = parsed.transactionCost
                    )
                )
            }
        }
    }
}
