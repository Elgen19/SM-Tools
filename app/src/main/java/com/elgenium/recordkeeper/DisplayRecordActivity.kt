package com.elgenium.recordkeeper

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.elgenium.recordkeeper.databinding.ActivityDisplayRecordBinding

class DisplayRecordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDisplayRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve individual properties from the intent
        val buyer = intent.getStringExtra("BUYER")
        val kilos = intent.getDoubleExtra("KILOS", 0.0)
        val price = intent.getDoubleExtra("PRICE", 0.0)
        val merchandiseType = intent.getStringExtra("MERCHANDISE_TYPE")
        val paymentStatus = intent.getStringExtra("PAYMENT_STATUS")
        val totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)

        // Now, you can populate the TextViews in your layout with the retrieved properties
        binding.textViewBuyerValue.text = buyer
        binding.textViewKilosValue.text = kilos.toString()
        binding.textViewPriceValue.text = price.toString()
        binding.textViewMerchandiseTypeValue.text = merchandiseType
        binding.textViewPaymentStatusValue.text = paymentStatus
        binding.textViewTotalAmountValue.text = totalAmount.toString()
    }
}