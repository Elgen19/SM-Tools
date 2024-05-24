package com.elgenium.recordkeeper.firebase.models

data class Expense(
    val expenseType: String = "",
    val expenseDescription: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val amount: Double = 0.0,
    val showQuantityAndPrice: Boolean = false
)

