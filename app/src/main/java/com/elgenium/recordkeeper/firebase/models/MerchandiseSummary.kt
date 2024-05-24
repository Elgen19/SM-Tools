package com.elgenium.recordkeeper.firebase.models


data class MerchandiseSummary(
    val merchandiseType: String,
    var kilosInCash: Double,
    var paidInCash: Double,
    var kilosInCredit: Double,
    var unpaid: Double,
    var totalKilos: Double,
    var totalAmount: Double
)
