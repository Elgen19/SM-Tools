package com.elgenium.recordkeeper

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.elgenium.recordkeeper.adapters.TallyRecordAdapter
import com.elgenium.recordkeeper.databinding.ActivityAddRecordBinding
import com.elgenium.recordkeeper.databinding.DialogAddTallyBinding
import com.elgenium.recordkeeper.firebase.models.TallyRecord
import com.google.firebase.database.*


class AddRecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecordBinding
    private lateinit var database: DatabaseReference
    private lateinit var tallyRecordAdapter: TallyRecordAdapter
    private val tallyRecords = mutableListOf<TallyRecord>()
    private var originalTallyRecords = mutableListOf<TallyRecord>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference


        // Retrieve the record name from the intent
        val recordName = intent.getStringExtra("RECORD_NAME")

        // Initialize RecyclerView
        tallyRecordAdapter = TallyRecordAdapter(tallyRecords) { tallyRecord ->
            val intent = Intent(this, DisplayRecordActivity::class.java)
            // Pass individual properties
            intent.putExtra("BUYER", tallyRecord.buyer)
            intent.putExtra("KILOS", tallyRecord.kilos)
            intent.putExtra("PRICE", tallyRecord.price)
            intent.putExtra("MERCHANDISE_TYPE", tallyRecord.merchandiseType)
            intent.putExtra("PAYMENT_STATUS", tallyRecord.paymentStatus)
            intent.putExtra("TOTAL_AMOUNT", tallyRecord.totalAmount)
            intent.putExtra("RECORD_NAME", tallyRecord.recordName)
            startActivity(intent)
        }

        // Initialize RecyclerView
        binding.recyclerViewAddRecord.apply {
            layoutManager = LinearLayoutManager(this@AddRecordActivity)
            adapter = tallyRecordAdapter
        }

        // Load existing records from Firebase
        if (recordName != null) {
            loadTallyRecords(recordName)
        }

        // Handle "Add a tally" button click
        binding.buttonAddTally.setOnClickListener {
            if (recordName != null) {
                showAddTallyDialog(recordName)
            }
        }



        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_tally_record -> {
                    // Handle Tally Record click
                    true
                }
                R.id.menu_reports -> {
                    // Handle Reports click
                    val intent = Intent(this, AllReportsActivity::class.java)
                    intent.putExtra("RECORD_NAME", recordName)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.textInputEditTextAddRecord.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRecords(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterRecords(query: String) {
        tallyRecords.clear()
        if (query.isEmpty()) {
            tallyRecords.addAll(originalTallyRecords)
        } else {
            val filteredList = originalTallyRecords.filter { tallyRecord ->
                tallyRecord.merchandiseType.contains(query, ignoreCase = true) ||
                        tallyRecord.buyer.contains(query, ignoreCase = true)
            }
            tallyRecords.addAll(filteredList)
        }
        tallyRecordAdapter.notifyDataSetChanged()
    }

    private fun loadTallyRecords(recordName: String) {
        database.child("records").child(recordName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tallyRecords.clear()
                originalTallyRecords.clear()

                // Loop through each child node under the recordName node
                for (recordSnapshot in snapshot.children) {
                    // Check if the child node is not the "payments" node
                    if (recordSnapshot.key != "payments") {
                        // Retrieve TallyRecord data and add it to the tallyRecords list
                        val tallyRecord = recordSnapshot.getValue(TallyRecord::class.java)
                        if (tallyRecord != null && tallyRecord.merchandiseType.isNotBlank()) {
                            tallyRecords.add(tallyRecord)
                            originalTallyRecords.add(tallyRecord)
                        }
                    }
                }
                tallyRecordAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddRecordActivity, "Failed to load records", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun showAddTallyDialog(recordName: String) {
        // Inflate the dialog layout
        val dialogBinding = DialogAddTallyBinding.inflate(LayoutInflater.from(this))
        val dialogView = dialogBinding.root

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set click listener for the "Add tally" button
        dialogBinding.buttonAddTally.setOnClickListener {
            addTallyRecord(dialogBinding, recordName, dialog)
        }

        // Set click listener for the "Close" button
        dialogBinding.buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }


    private fun addTallyRecord(dialogBinding: DialogAddTallyBinding, recordName: String, dialog: AlertDialog) {
        // Get user input
        val kilos = dialogBinding.textInputEditTextKilos.text.toString()
        val price = dialogBinding.textInputEditTextPrice.text.toString()
        val merchandiseType = dialogBinding.spinnerMerchandiseType.selectedItem.toString()
        val buyer = dialogBinding.spinnerBuyer.selectedItem.toString()
        val paymentStatus = dialogBinding.spinnerPaymentStatus.selectedItem.toString()

        // Validate user input
        if (kilos.isEmpty() || kilos.toDouble() == 0.0) {
            Toast.makeText(this, "Enter a valid number of kilos", Toast.LENGTH_SHORT).show()
            return
        }

        if (price.isEmpty() || price.toDouble() == 0.0) {
            Toast.makeText(this, "Enter a valid number for price", Toast.LENGTH_SHORT).show()
            return
        }

        if (merchandiseType == "Select merchandise type") {
            Toast.makeText(this, "Select a merchandise type", Toast.LENGTH_SHORT).show()
            return
        }

        if (buyer == "Select a buyer") {
            Toast.makeText(this, "Select a buyer", Toast.LENGTH_SHORT).show()
            return
        }

        if (paymentStatus == "Select payment status") {
            Toast.makeText(this, "Select a payment status", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculate the total amount
        val totalAmount = kilos.toDouble() * price.toDouble()

        // Save the new record
        val recordId = database.child("records").child(recordName).push().key
        val tallyRecord = TallyRecord(
            recordName,
            kilos.toDouble(),
            price.toDouble(),
            merchandiseType,
            buyer,
            paymentStatus,
            totalAmount
        )

        if (recordId != null) {
            database.child("records").child(recordName).child(recordId).setValue(tallyRecord)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        tallyRecords.add(tallyRecord)
                        // Notify the adapter about the dataset change
                        tallyRecordAdapter.notifyDataSetChanged()
                        Toast.makeText(this@AddRecordActivity, "Tally added successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this@AddRecordActivity, "Failed to add tally", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this@AddRecordActivity, "Failed to generate record ID", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Retrieve the record name from the intent
        val recordName = intent.getStringExtra("RECORD_NAME")
        // Load existing records from Firebase
        if (recordName != null) {
            loadTallyRecords(recordName)
        }
    }

}
