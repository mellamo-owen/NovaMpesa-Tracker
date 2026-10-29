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

    fun showTransactionNotification(context: Context, txnId: Long, amount: Double, recipient: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "M-Pesa Transactions", NotificationManager.IMPORTANCE_HIGH)
            channel.setSound(null, null)
            manager.createNotificationChannel(channel)
        }

        // Use the transaction ID as the notification ID so they don't overwrite each other
        val notificationId = txnId.toInt()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Ksh $amount sent to $recipient")
            .setContentText("Tap a category below to save.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        // Pass the unique notificationId so the receiver can cancel the correct one
        builder.addAction(0, "Food", getCategoryPendingIntent(context, txnId, 1, notificationId))
        builder.addAction(0, "External Deposit", getCategoryPendingIntent(context, txnId, 999, notificationId))

        val moreIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SHOW_MORE_CATEGORIES", true)
            putExtra("TXN_ID", txnId)
        }
        val morePendingIntent = PendingIntent.getActivity(context, notificationId + 9999, moreIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(0, "More...", morePendingIntent)

        manager.notify(notificationId, builder.build())
    }

    private fun getCategoryPendingIntent(context: Context, txnId: Long, categoryId: Long, notificationId: Int): PendingIntent {
        val intent = Intent(context, CategoryActionReceiver::class.java).apply {
            action = "ASSIGN_CATEGORY"
            putExtra("TXN_ID", txnId)
            putExtra("CATEGORY_ID", categoryId)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        return PendingIntent.getBroadcast(context, (txnId * 100 + categoryId).toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
