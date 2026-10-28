package com.novanest.mpesatracker
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(this)
        
        lifecycleScope.launch {
            val state = db.appStateDao().getOnce()
            
            // If the local database says they are signed in, let them in
            if (state == null || !state.isSignedIn) {
                startActivity(Intent(this@LauncherActivity, SignInActivity::class.java))
                finish()
                return@launch
            }

            val now = System.currentTimeMillis()
            val trialEnd = state.trialStart + (60L * 24 * 60 * 60 * 1000)
            val isSubscribed = state.subscriptionExpiry != null && state.subscriptionExpiry > now
            val isTrialActive = now < trialEnd

            if (isTrialActive || isSubscribed) {
                startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@LauncherActivity, SubscriptionActivity::class.java))
            }
            finish()
        }
    }
}
