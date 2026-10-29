package com.novanest.mpesatracker
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MoreCategoriesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_categories)
        val txnId = intent.getLongExtra("TXN_ID", -1L)
        val container = findViewById<LinearLayout>(R.id.llCategoriesContainer)
        val db = AppDatabase.get(this)

        // Cancel the notification immediately when this screen opens
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(txnId.toInt())

        lifecycleScope.launch {
            val categories = db.categoryDao().getAll().first()
            for (cat in categories) {
                if (cat.name == "External Deposit") continue
                
                val btn = Button(this@MoreCategoriesActivity)
                btn.text = cat.name
                btn.setOnClickListener {
                    lifecycleScope.launch {
                        db.transactionDao().updateCategory(txnId, cat.id)
                        Toast.makeText(this@MoreCategoriesActivity, "Categorized as ${cat.name}!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                container.addView(btn)
            }
        }
    }
}
