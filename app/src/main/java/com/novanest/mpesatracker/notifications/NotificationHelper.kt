package com.novanest.mpesatracker.notifications
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.novanest.mpesatracker.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "mpesa_txn_channel"
    private const val NOTIFICATION_ID = 1001

    fun showTransactionNotification(context: Context, txnId: Long, amount: Double, recipient: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "M-Pesa Transactions", NotificationManager.IMPORTANCE_HIGH)
            channel.setSound(null, null) // Mutes the default notification sound for instant silent alerts
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ksh $amount sent to $recipient")
            .setContentText("Tap a category below to save.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // Tells Android this is urgent
            .setAutoCancel(true)

        builder.addAction(0, "Food", getCategoryPendingIntent(context, txnId, 1))
        builder.addAction(0, "External Deposit", getCategoryPendingIntent(context, txnId, 999))

        val moreIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SHOW_MORE_CATEGORIES", true)
            putExtra("TXN_ID", txnId)
        }
        val morePendingIntent = PendingIntent.getActivity(context, 99, moreIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(0, "More...", morePendingIntent)

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun getCategoryPendingIntent(context: Context, txnId: Long, categoryId: Long): PendingIntent {
        val intent = Intent(context, CategoryActionReceiver::class.java).apply {
            action = "ASSIGN_CATEGORY"
            putExtra("TXN_ID", txnId)
            putExtra("CATEGORY_ID", categoryId)
        }
        return PendingIntent.getBroadcast(context, categoryId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
