package com.elgenium.recordkeeper.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elgenium.recordkeeper.R
import com.elgenium.recordkeeper.databinding.ItemMerchandiseSummaryBinding
import com.elgenium.recordkeeper.firebase.models.MerchandiseSummary
import java.text.NumberFormat
import java.util.Locale

class MerchandiseSummaryAdapter(private var summaryList: List<MerchandiseSummary>) : RecyclerView.Adapter<MerchandiseSummaryAdapter.SummaryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val binding = ItemMerchandiseSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val summary = summaryList[position]
        holder.bind(summary)
    }

    fun updateData(newSummaryList: List<MerchandiseSummary>) {
        summaryList = newSummaryList
        notifyDataSetChanged()
    }

    fun getMerchandiseSummaries(): List<MerchandiseSummary> {
        return summaryList
    }

    override fun getItemCount() = summaryList.size

    class SummaryViewHolder(private val binding: ItemMerchandiseSummaryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: MerchandiseSummary) {
            val context = binding.root.context
            val locale = Locale.getDefault()
            val numberFormat = NumberFormat.getNumberInstance(locale).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }

            binding.merchandiseType.text = context.getString(R.string.merchandise_type, summary.merchandiseType)
            binding.kilosInCash.text = context.getString(R.string.kilos_in_cash, numberFormat.format(summary.kilosInCash))
            binding.paidInCash.text = context.getString(R.string.paid_in_cash, numberFormat.format(summary.paidInCash))
            binding.kilosInCredit.text = context.getString(R.string.kilos_in_credit, numberFormat.format(summary.kilosInCredit))
            binding.unpaid.text = context.getString(R.string.unpaid, numberFormat.format(summary.unpaid))
            binding.totalKilos.text = context.getString(R.string.total_kilos, numberFormat.format(summary.totalKilos))
            binding.totalAmount.text = context.getString(R.string.total_amount, numberFormat.format(summary.totalAmount))
        }

    }
}
