package com.example.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("finance_tracker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_BALANCE_DOMPET = "balance_dompet"
        private const val KEY_BALANCE_SEABANK = "balance_seabank"
        private const val KEY_LAST_INTEREST_DATE = "last_interest_date"
    }

    fun hasSetupKey(): Boolean {
        return prefs.contains(KEY_SETUP_COMPLETED)
    }

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    var balanceDompet: Double
        get() = prefs.getFloat(KEY_BALANCE_DOMPET, 0.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_BALANCE_DOMPET, value.toFloat()).apply()

    var balanceSeaBank: Double
        get() = prefs.getFloat(KEY_BALANCE_SEABANK, 0.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_BALANCE_SEABANK, value.toFloat()).apply()

    var lastInterestDate: String?
        get() = prefs.getString(KEY_LAST_INTEREST_DATE, null)
        set(value) = prefs.edit().putString(KEY_LAST_INTEREST_DATE, value).apply()

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
