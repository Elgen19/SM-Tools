package com.elgenium.recordkeeper

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.elgenium.recordkeeper.adapters.RecordNameAdapter
import com.elgenium.recordkeeper.databinding.ActivityMainBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference.child("records")

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
                // Handle possible errors.
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
