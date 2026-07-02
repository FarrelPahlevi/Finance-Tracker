package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val jenis: String, // "pemasukan" or "pengeluaran"
    val nama: String,
    val kategori: String,
    val nominal: Double,
    val sumber: String, // "dompet" or "seabank"
    val tanggal: String, // "yyyy-MM-dd"
    val catatan: String,
    val isAuto: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
