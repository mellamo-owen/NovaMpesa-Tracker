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
            val body = msg.messageBody ?: continue
            val parsed = MpesaParser.parse(body) ?: continue

            val db = AppDatabase.get(context)
            CoroutineScope(Dispatchers.IO).launch {
                val txnId = db.transactionDao().insert(
                    Transaction(
                        amount = parsed.amount,
                        recipient = parsed.recipient,
                        categoryId = null,
                        timestamp = parsed.timestamp,
                        isExternalDeposit = false,
                        transactionCost = parsed.transactionCost
                    )
                )
                NotificationHelper.showTransactionNotification(context, txnId, parsed.amount, parsed.recipient)
            }
        }
    }
}
