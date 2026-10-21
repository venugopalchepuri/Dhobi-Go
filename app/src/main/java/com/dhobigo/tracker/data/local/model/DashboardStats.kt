package com.dhobigo.tracker.data.local.model

data class DashboardStats(
    val bagsAtDhobi: Int = 0,
    val itemsAwaitingReturn: Int = 0,
    val partiallyReturnedBags: Int = 0,
    val totalCollectedBags: Int = 0
)
