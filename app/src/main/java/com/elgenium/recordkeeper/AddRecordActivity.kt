package com.elgenium.recordkeeper

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.elgenium.recordkeeper.adapters.TallyRecordAdapter
import com.elgenium.recordkeeper.databinding.ActivityAddRecordBinding
import com.elgenium.recordkeeper.databinding.DialogAddTallyBinding
import com.elgenium.recordkeeper.firebase.models.TallyRecord
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AddRecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecordBinding
    private lateinit var database: DatabaseReference
    private lateinit var tallyRecordAdapter: TallyRecordAdapter
    private val tallyRecords = mutableListOf<TallyRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

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
            startActivity(intent)
        }

        binding.recyclerViewAddRecord.apply {
            layoutManager = LinearLayoutManager(this@AddRecordActivity)
            adapter = tallyRecordAdapter
        }

        // Retrieve the record name from the intent
        val recordName = intent.getStringExtra("RECORD_NAME")

        // Handle "Add a tally" button click
        binding.buttonAddTally.setOnClickListener {
            if (recordName != null) {
                showAddTallyDialog(recordName)
            }
        }

        // Load existing records from Firebase
        if (recordName != null) {
            loadTallyRecords(recordName)
        }
    }

    private fun loadTallyRecords(recordName: String) {
        database.child("records").child(recordName).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tallyRecords.clear()
                for (recordSnapshot in snapshot.children) {
                    val tallyRecord = recordSnapshot.getValue(TallyRecord::class.java)
                    if (tallyRecord != null) {
                        tallyRecords.add(tallyRecord)
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
            addTallyRecord(dialogBinding, recordName, dismissDialog = false, dialog)
        }

        // Set click listener for the "Done" button
        dialogBinding.buttonDone.setOnClickListener {
            addTallyRecord(dialogBinding, recordName, dismissDialog = true, dialog)
        }

        // Set click listener for the "Close" button
        dialogBinding.buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    private fun addTallyRecord(dialogBinding: DialogAddTallyBinding, recordName: String, dismissDialog: Boolean, dialog: AlertDialog) {
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

        // Check if the record already exists for the given merchandiseType and buyer
        database.child("records").child(recordName)
            .orderByChild("merchandiseType")
            .equalTo(merchandiseType)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var recordUpdated = false
                    for (snapshot in dataSnapshot.children) {
                        val existingRecord = snapshot.getValue(TallyRecord::class.java)
                        if (existingRecord != null && existingRecord.buyer == buyer) {
                            // Update the existing record
                            val updatedKilos = existingRecord.kilos + kilos.toDouble()
                            val updatedTotalAmount = updatedKilos * existingRecord.price
                            val updatedRecord = existingRecord.copy(kilos = updatedKilos, totalAmount = updatedTotalAmount)
                            snapshot.ref.setValue(updatedRecord)
                            recordUpdated = true
                            break
                        }
                    }
                    if (!recordUpdated) {
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
                        }
                    }
                    // Display success message
                    Toast.makeText(this@AddRecordActivity, "Tally added successfully", Toast.LENGTH_SHORT).show()

                    if (dismissDialog) {
                        dialog.dismiss()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@AddRecordActivity, "Failed to add tally", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
