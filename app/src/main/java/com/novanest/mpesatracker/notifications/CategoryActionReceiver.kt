package com.novanest.mpesatracker.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.novanest.mpesatracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val txnId = intent.getLongExtra("TXN_ID", -1L)
        val categoryId = intent.getLongExtra("CATEGORY_ID", -1L)

        if (txnId != -1L && categoryId != -1L) {
            val db = AppDatabase.get(context)
            CoroutineScope(Dispatchers.IO).launch {
                db.transactionDao().updateCategory(txnId, categoryId)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(1001)
        }
    }
}
