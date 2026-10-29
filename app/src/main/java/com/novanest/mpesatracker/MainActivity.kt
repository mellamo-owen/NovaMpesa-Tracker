package com.novanest.mpesatracker
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.novanest.mpesatracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getBooleanExtra("SHOW_MORE_CATEGORIES", false)) {
            val txnId = intent.getLongExtra("TXN_ID", -1L)
            val intentToMore = Intent(this, MoreCategoriesActivity::class.java)
            intentToMore.putExtra("TXN_ID", txnId)
            startActivity(intentToMore)
            finish()
            return
        }

        loadFragment(HomeFragment())
        askForBatteryOptimization()

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

    private fun askForBatteryOptimization() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        // Apply theme on page change
        binding.root.post {
            ThemeManager.applyTheme(this, binding.root)
        }
    }
}
