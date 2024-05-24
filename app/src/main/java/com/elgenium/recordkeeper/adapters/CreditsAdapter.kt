package com.elgenium.recordkeeper.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elgenium.recordkeeper.databinding.ItemCreditsBinding
import com.elgenium.recordkeeper.firebase.models.Credits
import java.util.Locale

class CreditsAdapter(private val credits: List<Credits>) : RecyclerView.Adapter<CreditsAdapter.CreditViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditViewHolder {
        val binding = ItemCreditsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CreditViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CreditViewHolder, position: Int) {
        val credit = credits[position]
        holder.bind(credit)
    }

    override fun getItemCount() = credits.size

    class CreditViewHolder(private val binding: ItemCreditsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(credit: Credits) {
            binding.apply {
                buyerName.text = credit.buyer
                val formattedDetails = String.format(
                    Locale.US,
                    "%s: (%.2f x %.2f) = %.2f",
                    credit.merchandiseType,
                    credit.kilos,
                    credit.price,
                    credit.totalAmount
                )
                binding.merchandiseDetails.text = formattedDetails


            }
        }
    }
}
