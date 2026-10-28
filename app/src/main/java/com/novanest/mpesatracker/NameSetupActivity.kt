package com.novanest.mpesatracker
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.AppState
import com.novanest.mpesatracker.databinding.ActivityNamesetupBinding
import kotlinx.coroutines.launch

class NameSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNamesetupBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNamesetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        db = AppDatabase.get(this)

        binding.btnSaveName.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSaveName.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Create new state with TODAY as the trial start
                    val newState = AppState(
                        userName = name,
                        trialStart = System.currentTimeMillis(),
                        subscriptionExpiry = null
                    )
                    db.appStateDao().saveState(newState)
                    
                    val intent = Intent(this@NameSetupActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@NameSetupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSaveName.isEnabled = true
                }
            }
        }
    }
}
