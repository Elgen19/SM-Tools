package com.elgenium.recordkeeper.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elgenium.recordkeeper.R
import com.elgenium.recordkeeper.firebase.models.TallyRecord

class TallyRecordAdapter(
    private val tallyRecords: List<TallyRecord>,
    private val onItemClick: (TallyRecord) -> Unit
) : RecyclerView.Adapter<TallyRecordAdapter.TallyRecordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TallyRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tally_record, parent, false)
        return TallyRecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: TallyRecordViewHolder, position: Int) {
        val tallyRecord = tallyRecords[position]
        holder.bind(tallyRecord, onItemClick)
    }

    override fun getItemCount(): Int {
        return tallyRecords.size
    }

    class TallyRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewKilosMerchandise: TextView = itemView.findViewById(R.id.textViewKilosMerchandise)
        private val textViewBuyer: TextView = itemView.findViewById(R.id.textViewBuyer)

        fun bind(tallyRecord: TallyRecord, onItemClick: (TallyRecord) -> Unit) {
            textViewKilosMerchandise.text = "${tallyRecord.kilos} kilos ${tallyRecord.merchandiseType}"
            textViewBuyer.text = tallyRecord.buyer
            itemView.setOnClickListener {
                onItemClick(tallyRecord)
            }
        }
    }
}
