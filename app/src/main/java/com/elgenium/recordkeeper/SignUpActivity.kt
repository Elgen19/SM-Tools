package com.elgenium.recordkeeper

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elgenium.recordkeeper.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // Initialize Firebase Realtime Database
        database = FirebaseDatabase.getInstance()

        binding.signUpButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Please enter a valid email address"
                binding.emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.confirmPasswordEditText.error = "Passwords do not match"
                binding.confirmPasswordEditText.requestFocus()
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                binding.passwordEditText.error = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, and one symbol"
                binding.passwordEditText.requestFocus()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign up success
                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                // Verification email sent successfully
                                Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
                                // Continue with your sign-up flow or UI update
                            }
                            ?.addOnFailureListener { e ->
                                // Verification email could not be sent
                                Toast.makeText(this, "Failed to send verification email: ${e.message}", Toast.LENGTH_SHORT).show()
                                // Continue with your sign-up flow or UI update
                            }

                        // Add name, email, and phone to the Realtime Database
                        val userId = user?.uid
                        if (userId != null) {
                            val userReference = database.reference.child("user").child(userId)
                            val userData = HashMap<String, Any>()
                            userData["name"] = name
                            userData["email"] = email
                            userData["phone"] = phone
                            userReference.setValue(userData)
                        }

                        // Continue with your sign-up flow or UI update
                        startActivity(Intent(this, SignInActivity::class.java))
                        finish()
                    } else {
                        // If sign up fails, display a message to the user.
                        Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

        }
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = Regex(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#\$%^&*()\\-_=+|;:,.<>?]).{8,}\$"
        )
        return passwordPattern.matches(password)
    }
}
