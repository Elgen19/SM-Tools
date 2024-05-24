package com.elgenium.recordkeeper.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elgenium.recordkeeper.databinding.ItemPriceBinding
import com.elgenium.recordkeeper.firebase.models.PriceItem
import java.text.DecimalFormat


class PriceAdapter(private var priceList: List<PriceItem>) : RecyclerView.Adapter<PriceAdapter.PriceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceViewHolder {
        val binding = ItemPriceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PriceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PriceViewHolder, position: Int) {
        val currentItem = priceList[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = priceList.size

    fun updateData(newPriceList: List<PriceItem>) {
        priceList = newPriceList
        notifyDataSetChanged()
    }

    fun getPriceItems(): List<PriceItem> {
        return priceList
    }

    class PriceViewHolder(private val binding: ItemPriceBinding) : RecyclerView.ViewHolder(binding.root) {

        private val decimalFormat = DecimalFormat("#,##0.00")

        fun bind(priceItem: PriceItem) {
            binding.merchandiseType.text = priceItem.merchandiseType

            // Format prices with commas and two decimal points
            val formattedPrices = priceItem.prices.map { decimalFormat.format(it) }
            val formattedPriceString = formattedPrices.joinToString(separator = "/")
            binding.priceList.text = formattedPriceString
        }
    }
}
