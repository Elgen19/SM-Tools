package com.elgenium.recordkeeper.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elgenium.recordkeeper.databinding.ItemPaymentBinding
import com.elgenium.recordkeeper.firebase.models.Payment
import java.text.NumberFormat
import java.util.Currency

class PaymentAdapter(private val payments: MutableList<Payment>) : RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    inner class PaymentViewHolder(private val binding: ItemPaymentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(payment: Payment) {
            val formattedAmount = NumberFormat.getCurrencyInstance().apply {
                currency = Currency.getInstance("PHP") // Change the currency as needed
            }.format(payment.paymentAmount)
            binding.buyerName.text = payment.buyerName
            binding.paymentAmount.text = formattedAmount
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(payments[position])
    }

    override fun getItemCount(): Int {
        return payments.size
    }


    fun getPaymentAt(position: Int): Payment {
        return payments[position]
    }

}

