package com.elgenium.recordkeeper

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elgenium.recordkeeper.databinding.ActivityUserInfoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserInfoBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get the current user
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // If user is not logged in, redirect to sign in activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference.child("user").child(currentUser.uid)

        // Fetch and display user information
        fetchUserInfo()

        // Set logout button click listener
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun fetchUserInfo() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val phone = snapshot.child("phone").getValue(String::class.java)

                binding.nameTextView.text = String.format("Name: %s", name)
                binding.emailTextView.text = String.format("Email: %s", email)
                binding.phoneTextView.text = String.format("Phone: %s", phone)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserInfoActivity, "Failed to load user info", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
