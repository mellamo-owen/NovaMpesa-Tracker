package com.novanest.mpesatracker
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.novanest.mpesatracker.data.AppDatabase
import com.novanest.mpesatracker.data.AppState
import com.novanest.mpesatracker.databinding.ActivitySigninBinding
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySigninBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        val db = AppDatabase.get(this)

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Enter a valid email and password (6+ chars)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Please wait..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        saveUserAndLaunch(db, email)
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { createTask ->
                                if (createTask.isSuccessful) {
                                    saveUserAndLaunch(db, email)
                                } else {
                                    binding.btnSignIn.isEnabled = true
                                    binding.btnSignIn.text = "Sign In"
                                    Toast.makeText(this, "Auth Failed: ${createTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                }
        }
    }

    private fun saveUserAndLaunch(db: AppDatabase, email: String) {
        lifecycleScope.launch {
            var state = db.appStateDao().getOnce()
            if (state == null) state = AppState()
            
            db.appStateDao().save(
                state.copy(
                    userName = email.substringBefore("@"),
                    userPhone = email,
                    isSignedIn = true,
                    trialStart = if (state.trialStart == 0L) System.currentTimeMillis() else state.trialStart
                )
            )
            
            startActivity(Intent(this@SignInActivity, SplashActivity::class.java))
            finish()
        }
    }
}
