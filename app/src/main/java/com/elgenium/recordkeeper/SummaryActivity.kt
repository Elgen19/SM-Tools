package com.elgenium.recordkeeper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.elgenium.recordkeeper.databinding.ActivitySummaryBinding
class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the message from the intent
        val summaryAndPayments = intent.getStringExtra("summary_and_payments_message")
        val creditsAndPrices = intent.getStringExtra("prices_and_credits_message")
        val numberOfKilos = intent.getStringExtra("number_of_kilos_message")
        val expenses = intent.getStringExtra("expenses_message")

        // Set the message to the TextView using ViewBinding
        binding.editTextForSummaryAndPayment.setText(summaryAndPayments)
        binding.editTextForPricesAndCredits.setText(creditsAndPrices)
        binding.editTextForNumberOfKilos.setText(numberOfKilos)
        binding.editTextForExpenses.setText(expenses)

        // Set onClickListener for the edit ImageViews
        binding.editImageViewForSummaryAndPayment.setOnClickListener {
            toggleEditText(binding.editTextForSummaryAndPayment)
        }

        binding.editImageViewForPricesAndCredits.setOnClickListener {
            toggleEditText(binding.editTextForPricesAndCredits)
        }

        binding.editImageViewForNumberOfKilos.setOnClickListener {
            toggleEditText(binding.editTextForNumberOfKilos)
        }

        binding.editImageViewForExpenses.setOnClickListener {
            toggleEditText(binding.editTextForExpenses)
        }

        // Set onClickListener for the send button
       binding.sendImageViewForSummaryAndPayment.setOnClickListener {
           sendMessage(summaryAndPayments)
       }

        binding.sendImageViewForPricesAndCredits.setOnClickListener {
            sendMessage(creditsAndPrices)
        }

        binding.sendImageViewForNumberOfKilos.setOnClickListener {
            sendMessage(numberOfKilos)

        }

        binding.sendImageViewForExpenses.setOnClickListener {
            sendMessage(expenses)

        }


    }

    private fun toggleEditText(editText: EditText) {
        // Toggle the enabled state of the EditText
        editText.isEnabled = !editText.isEnabled
    }

    private fun sendMessage(message: String?) {
        // Pre-configured phone number
        val phoneNumber = "09499663451"  // Replace with your target phone number

        // Create an intent to send the text message to the specific phone number
        val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
        }

        // Verify that the intent can be handled before starting it
        if (sendIntent.resolveActivity(packageManager) != null) {
            startActivity(sendIntent)
        }
    }

}
