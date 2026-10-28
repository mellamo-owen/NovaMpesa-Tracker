package com.novanest.mpesatracker

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import kotlinx.coroutines.launch

class MoreCategoriesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more_categories)

        val txnId = intent.getLongExtra("TXN_ID", -1L)
        val container = findViewById<LinearLayout>(R.id.llCategoriesContainer)
        val db = AppDatabase.get(this)

        lifecycleScope.launch {
            val categories = db.categoryDao().getAllOnce()
            for (cat in categories) {
                val btn = Button(this@MoreCategoriesActivity)
                btn.text = cat.name
                btn.setOnClickListener {
                    if (txnId != -1L) {
                        assignCategory(db, txnId, cat.id)
                    } else {
                        Toast.makeText(this@MoreCategoriesActivity, "No transaction found", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
                container.addView(btn)
            }
        }
    }

    private fun assignCategory(db: AppDatabase, txnId: Long, categoryId: Long) {
        lifecycleScope.launch {
            db.transactionDao().updateCategory(txnId, categoryId)
            Toast.makeText(this@MoreCategoriesActivity, "Expense categorized!", Toast.LENGTH_SHORT).show()
        }
    }
}
