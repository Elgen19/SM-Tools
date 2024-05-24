package com.elgenium.recordkeeper.firebase.models

data class Credits(
    val buyer: String,
    val kilos: Double,
    val price: Double,
    val merchandiseType: String,
    val totalAmount: Double
)

