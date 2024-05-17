package com.elgenium.recordkeeper.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elgenium.recordkeeper.R

class RecordNameAdapter(
    private val recordNames: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<RecordNameAdapter.RecordNameViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordNameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record_name, parent, false)
        return RecordNameViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordNameViewHolder, position: Int) {
        val recordName = recordNames[position]
        holder.bind(recordName, onItemClick)
    }

    override fun getItemCount(): Int {
        return recordNames.size
    }

    class RecordNameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewRecordName: TextView = itemView.findViewById(R.id.textViewRecordName)

        fun bind(recordName: String, onItemClick: (String) -> Unit) {
            textViewRecordName.text = recordName
            itemView.setOnClickListener {
                onItemClick(recordName)
            }
        }
    }
}

