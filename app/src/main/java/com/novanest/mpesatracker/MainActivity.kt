package com.novanest.mpesatracker
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.get(this)
        lifecycleScope.launch {
            val state = db.appStateDao().getState()
            if (state != null) {
                val now = System.currentTimeMillis()
                val trialEnd = state.trialStart + TimeUnit.DAYS.toMillis(60)
                val isSubscribed = state.subscriptionExpiry != null && state.subscriptionExpiry > now
                
                // ONLY LOCK IF 60 DAYS HAVE PASSED AND THEY HAVEN'T PAID
                if (now >= trialEnd && !isSubscribed) {
                    startActivity(Intent(this@MainActivity, SubscriptionActivity::class.java))
                    finish()
                    return@launch
                }
            }
            
            // If trial is active (Day 1 to 60), load the Home screen
            loadFragment(HomeFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_transactions -> { loadFragment(TransactionsFragment()); true }
                R.id.nav_budgets -> { loadFragment(BudgetsFragment()); true }
                R.id.nav_reports -> { loadFragment(ReportsFragment()); true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
