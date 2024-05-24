package com.elgenium.recordkeeper

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elgenium.recordkeeper.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater) // Initialize ViewBinding
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set OnClickListener for the "Sign In" button
        binding.signInButton.setOnClickListener {
            signIn(binding.emailEditText.text.toString(), binding.passwordEditText.text.toString())
        }

        // Set OnClickListener for the "Forgot Password" text
        binding.forgotPasswordTextView.setOnClickListener {
            // Implement your forgot password logic here
            // For example, you can navigate to a "ForgotPasswordActivity"
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.signUpTextView.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Check if the user's email is verified
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        // Navigate to the next activity, for example, MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Prompt the user to verify their email
                        Toast.makeText(this, "Please verify your email address", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
