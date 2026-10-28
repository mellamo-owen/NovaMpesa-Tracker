package com.novanest.mpesatracker
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.databinding.ActivitySubscriptionBinding
import com.novanest.mpesatracker.security.SecurityHelper
import kotlinx.coroutines.launch

class SubscriptionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        binding.tvDeviceId.text = deviceId

        binding.btnExit.setOnClickListener { finishAffinity() }

        binding.btnActivate.setOnClickListener {
            val code = binding.etActivationCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // REAL VERIFICATION
            val publicKey = getString(R.string.public_key)
            val expiryMillis = SecurityHelper.verifyCode(publicKey, code, deviceId)

            if (expiryMillis != null) {
                if (expiryMillis > System.currentTimeMillis()) {
                    lifecycleScope.launch {
                        val state = db.appStateDao().getState()
                        if (state != null) {
                            db.appStateDao().saveState(state.copy(subscriptionExpiry = expiryMillis))
                        }
                        Toast.makeText(this@SubscriptionActivity, "Activation Successful!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@SubscriptionActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Code has expired", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Invalid code. Signature failed.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
