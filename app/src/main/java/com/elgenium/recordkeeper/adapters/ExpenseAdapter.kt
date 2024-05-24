package com.elgenium.recordkeeper.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elgenium.recordkeeper.databinding.ItemExpenseBinding
import com.elgenium.recordkeeper.firebase.models.Expense
import com.elgenium.recordkeeper.firebase.models.MerchandiseSummary

class ExpenseAdapter(
    private var expenses: List<Expense>
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense) {
            val expenseDescription = expense.expenseDescription

            if (expenseDescription.isEmpty()) {
                binding.textViewExpense.text = expense.expenseType
            }else{
                binding.textViewExpense.text = "${expense.expenseType} (${expense.expenseDescription})"
            }

            if (expense.showQuantityAndPrice) {
                val quantity = expense.quantity
                val price = expense.price
                val amount = quantity * price
                binding.textViewExpenseDetail.text = "$quantity x $price = $amount"
            } else {
                binding.textViewExpenseDetail.text = "Amount: ${expense.amount}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size

    fun updateData(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }

    fun getExpensesAt(position: Int): Expense {
        return expenses[position]
    }

    fun getExpenseSummaries(): List<Expense> {
        return expenses
    }

}
