package com.novanest.mpesatracker
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.novanest.mpesatracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme BEFORE super.onCreate
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("is_dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(HomeFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_transactions -> { loadFragment(TransactionsFragment()); true }
                R.id.nav_budgets -> { loadFragment(BudgetsFragment()); true }
                R.id.nav_reports -> { loadFragment(ReportsFragment()); true }
                else -> false
            }
        }
        
        if (intent.getBooleanExtra("SHOW_MORE_CATEGORIES", false)) {
            val txnId = intent.getLongExtra("TXN_ID", -1L)
            val intentToMore = Intent(this, MoreCategoriesActivity::class.java)
            intentToMore.putExtra("TXN_ID", txnId)
            startActivity(intentToMore)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
