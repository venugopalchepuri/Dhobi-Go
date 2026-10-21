package com.dhobigo.tracker.data.local.model

enum class LaundryStatus(val displayName: String) {
    PENDING("Pending"),
    PARTIALLY_RETURNED("Partially Returned"),
    COLLECTED("Collected");

    companion object {
        fun fromString(value: String?): LaundryStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
