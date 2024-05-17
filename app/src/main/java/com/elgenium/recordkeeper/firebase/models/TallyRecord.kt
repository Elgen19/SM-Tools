package com.elgenium.recordkeeper.firebase.models

// Create a data class to represent a TallyRecord with default values
data class TallyRecord(
    val recordName: String = "",
    val kilos: Double = 0.0,
    val price: Double = 0.0,
    val merchandiseType: String = "",
    val buyer: String = "",
    val paymentStatus: String = "",
    val totalAmount: Double = 0.0
)
