package com.example.utils

import com.example.data.TransactionEntity

data class Balances(val dompet: Double, val seabank: Double)

fun calculateNewBalances(
    currentBalances: Balances,
    transaction: TransactionEntity,
    action: String // "apply" or "revert"
): Balances {
    var dompet = currentBalances.dompet
    var seabank = currentBalances.seabank

    val nominal = transaction.nominal
    val sumber = transaction.sumber.lowercase()
    val jenis = transaction.jenis.lowercase()

    val factor = if (action == "apply") 1 else -1

    if (jenis == "pemasukan") {
        if (sumber == "dompet") {
            dompet += factor * nominal
        } else if (sumber == "seabank") {
            seabank += factor * nominal
        }
    } else if (jenis == "pengeluaran") {
        if (sumber == "dompet") {
            dompet -= factor * nominal
        } else if (sumber == "seabank") {
            seabank -= factor * nominal
        }
    }

    return Balances(dompet, seabank)
}
