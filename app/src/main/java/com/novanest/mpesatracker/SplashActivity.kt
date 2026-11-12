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
            try {
                val db = AppDatabase.get(this)
                lifecycleScope.launch {
                    val state = db.appStateDao().getState()
                    
                    // If they have no name, they need to set it up first
                    if (state == null || state.userName.isEmpty()) {
                        startActivity(Intent(this@SplashActivity, NameSetupActivity::class.java))
                    } else {
                        // Check Trial Status
                        val now = System.currentTimeMillis()
                        val trialEnd = state.trialStart + (60L * 24 * 60 * 60 * 1000)
                        val isSubscribed = state.subscriptionExpiry != null && state.subscriptionExpiry > now
                        
                        if (now < trialEnd || isSubscribed) {
                            // Trial active: Go to Welcome Screen EVERY time
                            startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
                        } else {
                            // Trial expired: Go to Subscription
                            startActivity(Intent(this@SplashActivity, SubscriptionActivity::class.java))
                        }
                    }
                    finish()
                }
            } catch (e: Exception) {
                startActivity(Intent(this@SplashActivity, NameSetupActivity::class.java))
                finish()
            }
        }, 2000)
    }
}
