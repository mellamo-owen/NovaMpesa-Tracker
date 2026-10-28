package com.novanest.mpesatracker
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val db = AppDatabase.get(this)
            lifecycleScope.launch {
                val state = db.appStateDao().getState()
                
                if (state == null || state.userName.isEmpty()) {
                    startActivity(Intent(this@SplashActivity, NameSetupActivity::class.java))
                } else {
                    val now = System.currentTimeMillis()
                    val trialEnd = state.trialStart + (60L * 24 * 60 * 60 * 1000)
                    val isSubscribed = state.subscriptionExpiry != null && state.subscriptionExpiry > now
                    
                    if (now < trialEnd || isSubscribed) {
                                    // End of Month Notification Check
            val cal = java.util.Calendar.getInstance()
            if (cal.get(java.util.Calendar.DAY_OF_MONTH) == 1) {
                val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val channelId = "monthly_report_channel"
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    notificationManager.createNotificationChannel(android.app.NotificationChannel(channelId, "Monthly Reports", android.app.NotificationManager.IMPORTANCE_HIGH))
                }
                val notification = androidx.core.app.NotificationCompat.Builder(this@SplashActivity, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Your Monthly Report is Ready!")
                    .setContentText("Open NovaMpesa and go to Analytics to download your report.")
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(3001, notification)
            }
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@SplashActivity, SubscriptionActivity::class.java))
                    }
                }
                finish()
            }
        }, 2000)
    }
}

