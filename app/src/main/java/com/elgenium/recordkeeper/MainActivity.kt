package com.elgenium.recordkeeper

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.elgenium.recordkeeper.adapters.RecordNameAdapter
import com.elgenium.recordkeeper.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private lateinit var recordNameAdapter: RecordNameAdapter
    private val recordNames = mutableListOf<String>()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference.child("records")

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Fetch user's name from Firebase
            fetchUserName(currentUser.uid)
        }

        // Initialize RecyclerView
        recordNameAdapter = RecordNameAdapter(recordNames) { recordName ->
            // Navigate to AddRecordActivity with the selected record name
            val intent = Intent(this, AddRecordActivity::class.java).apply {
                putExtra("RECORD_NAME", recordName)
            }
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recordNameAdapter
        }

        // Fetch record names from Firebase
        loadRecordNames()

        binding.fab.setOnClickListener {
            // Generate the record name
            val currentDate = Date()
            val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
            val dateString = dateFormat.format(currentDate)
            val recordName = "$dateString Pasil Records"

            // Check if the record name already exists before adding it
            checkAndAddRecordName(recordName)
        }

        binding.userImageButton.setOnClickListener {
            startActivity(Intent(this, UserInfoActivity::class.java))
            finish()
        }


        // Add TextWatcher to TextInputEditText
        binding.textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRecords(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterRecords(query: String) {
        val filteredRecords = recordNames.filter { it.contains(query, ignoreCase = true) }
        recordNameAdapter.updateRecords(filteredRecords)
    }

    private fun fetchUserName(userId: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("user").child(userId).child("name")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.getValue(String::class.java)
                if (userName != null) {
                    binding.userNameTextView.text = String.format("Howdy, %s", userName)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load user name", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRecordNames() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recordNames.clear()
                for (recordSnapshot in snapshot.children) {
                    val recordName = recordSnapshot.key
                    if (recordName != null) {
                        recordNames.add(recordName)
                    }
                }
                recordNameAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error loading records", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkAndAddRecordName(recordName: String) {
        database.child(recordName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Record name already exists
                    Toast.makeText(this@MainActivity, "Record already exists", Toast.LENGTH_SHORT).show()
                } else {
                    // Record name does not exist, add it
                    addRecordName(recordName) {
                        // After adding the record name, navigate to AddRecordActivity
                        val intent = Intent(this@MainActivity, AddRecordActivity::class.java).apply {
                            putExtra("RECORD_NAME", recordName)
                        }
                        startActivity(intent)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
                Toast.makeText(this@MainActivity, "Failed to check record name", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addRecordName(recordName: String, onSuccess: () -> Unit) {
        database.child(recordName).setValue(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                Toast.makeText(this, "Failed to add record name", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
