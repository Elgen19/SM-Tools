package com.elgenium.recordkeeper

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ContentInfoCompat.Flags
import com.elgenium.recordkeeper.databinding.ActivityDisplayRecordBinding
import com.elgenium.recordkeeper.databinding.DialogUpdateTallyBinding
import com.elgenium.recordkeeper.firebase.models.TallyRecord
import com.google.firebase.database.*

class DisplayRecordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDisplayRecordBinding
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference

        // Retrieve individual properties from the intent
        val buyer = intent.getStringExtra("BUYER")
        val recordName = intent.getStringExtra("RECORD_NAME")
        val kilos = intent.getDoubleExtra("KILOS", 0.0)
        val price = intent.getDoubleExtra("PRICE", 0.0)
        val merchandiseType = intent.getStringExtra("MERCHANDISE_TYPE")
        val paymentStatus = intent.getStringExtra("PAYMENT_STATUS")
        val totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)

        // Populate the TextViews in your layout with the retrieved properties
        binding.textViewBuyerValue.text = buyer
        binding.textViewKilosValue.text = kilos.toString()
        binding.textViewPriceValue.text = price.toString()
        binding.textViewMerchandiseTypeValue.text = merchandiseType
        binding.textViewPaymentStatusValue.text = paymentStatus
        binding.textViewTotalAmountValue.text = totalAmount.toString()

        // Set click listener for the "Delete" button
        binding.buttonDelete.setOnClickListener {
            if (recordName != null) {
                findAndDeleteRecord(recordName, buyer, kilos, price, merchandiseType, paymentStatus, totalAmount)
            } else {
                Toast.makeText(this, "Record information is missing", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener for the "Update" button
        binding.buttonUpdate.setOnClickListener {
            showUpdateDialog(recordName, buyer, kilos, price, merchandiseType, paymentStatus, totalAmount)
        }
    }

    private fun findAndDeleteRecord(recordName: String, buyer: String?, kilos: Double, price: Double, merchandiseType: String?, paymentStatus: String?, totalAmount: Double) {
        val query = database.child("records").child(recordName)
            .orderByChild("buyer").equalTo(buyer)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (recordSnapshot in snapshot.children) {
                    val record = recordSnapshot.getValue(TallyRecord::class.java)
                    if (record != null &&
                        record.buyer == buyer &&
                        record.kilos == kilos &&
                        record.price == price &&
                        record.merchandiseType == merchandiseType &&
                        record.paymentStatus == paymentStatus &&
                        record.totalAmount == totalAmount
                    ) {
                        val recordId = recordSnapshot.key
                        if (recordId != null) {
                            deleteRecord(recordName, recordId)
                            return
                        }
                    }
                }
                Toast.makeText(this@DisplayRecordActivity, "Record not found", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DisplayRecordActivity, "Failed to find record: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteRecord(recordName: String, recordId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Record")
        builder.setMessage("Are you sure you want to delete this record?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            database.child("records").child(recordName).child(recordId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Record deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete record", Toast.LENGTH_SHORT).show()
                }
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun showUpdateDialog(recordName: String?, buyer: String?, kilos: Double, price: Double, merchandiseType: String?, paymentStatus: String?, totalAmount: Double) {
        if (recordName == null) {
            Toast.makeText(this, "Record information is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogUpdateTallyBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.textInputEditTextKilos.setText(kilos.toString())
        dialogBinding.textInputEditTextPrice.setText(price.toString())
        dialogBinding.spinnerMerchandiseType.setSelection(getSpinnerIndex(dialogBinding.spinnerMerchandiseType, merchandiseType))
        dialogBinding.spinnerBuyer.setSelection(getSpinnerIndex(dialogBinding.spinnerBuyer, buyer))
        dialogBinding.spinnerPaymentStatus.setSelection(getSpinnerIndex(dialogBinding.spinnerPaymentStatus, paymentStatus))

        dialogBinding.buttonClose.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialogBinding.buttonUpdateChanges.setOnClickListener {
            val updatedKilos = dialogBinding.textInputEditTextKilos.text.toString().toDoubleOrNull() ?: 0.0
            val updatedPrice = dialogBinding.textInputEditTextPrice.text.toString().toDoubleOrNull() ?: 0.0
            val updatedMerchandiseType = dialogBinding.spinnerMerchandiseType.selectedItem.toString()
            val updatedBuyer = dialogBinding.spinnerBuyer.selectedItem.toString()
            val updatedPaymentStatus = dialogBinding.spinnerPaymentStatus.selectedItem.toString()
            val updatedTotalAmount = updatedKilos * updatedPrice

            val updatedRecord = TallyRecord(
                recordName = recordName,
                kilos = updatedKilos,
                price = updatedPrice,
                merchandiseType = updatedMerchandiseType,
                buyer = updatedBuyer,
                paymentStatus = updatedPaymentStatus,
                totalAmount = updatedTotalAmount
            )

            findAndUpdateRecord(recordName, updatedRecord, buyer, kilos, price, merchandiseType)
        }

        dialog.show()
    }


    private fun getSpinnerIndex(spinner: Spinner, value: String?): Int {
        if (value == null) return 0
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString() == value) {
                return i
            }
        }
        return 0
    }

    private fun findAndUpdateRecord(recordName: String, updatedRecord: TallyRecord, intentBuyer: String?, originalKilos: Double, originalPrice: Double, originalMerchandiseType: String?) {
        val query = database.child("records").child(recordName)
            .orderByChild("buyer").equalTo(intentBuyer)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (recordSnapshot in snapshot.children) {
                    val record = recordSnapshot.getValue(TallyRecord::class.java)
                    if (record != null &&
                        record.kilos == originalKilos &&
                        record.price == originalPrice &&
                        record.merchandiseType == originalMerchandiseType &&
                        record.buyer == intentBuyer
                    ) {
                        val recordId = recordSnapshot.key
                        if (recordId != null) {
                            database.child("records").child(recordName).child(recordId).setValue(updatedRecord)
                                .addOnSuccessListener {
                                    Toast.makeText(this@DisplayRecordActivity, "Record updated successfully", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@DisplayRecordActivity, "Failed to update record", Toast.LENGTH_SHORT).show()
                                }
                            return
                        }
                    }
                }
                Toast.makeText(this@DisplayRecordActivity, "Record not found", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DisplayRecordActivity, "Failed to find record: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
