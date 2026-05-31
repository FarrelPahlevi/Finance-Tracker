package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.FinanceViewModel
import com.example.ui.formatRupiah

@Composable
fun HistoryScreen(viewModel: FinanceViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val filterJenis by viewModel.filterJenis.collectAsState()
    val filterSumber by viewModel.filterSumber.collectAsState()

    // Filter computation
    val filteredList = transactions.filter { tx ->
        val matchJenis = when (filterJenis) {
            "Pemasukan" -> tx.jenis == "pemasukan"
            "Pengeluaran" -> tx.jenis == "pengeluaran"
            else -> true
        }
        val matchSumber = when (filterSumber) {
            "Dompet" -> tx.sumber == "dompet"
            "SeaBank" -> tx.sumber == "seabank"
            else -> true
        }
        matchJenis && matchSumber
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Riwayat Keuangan",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Filter Jenis Row
            Text(
                text = "Jenis Transaksi",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Semua", "Pemasukan", "Pengeluaran").forEach { label ->
                    val isSelected = filterJenis == label
                    FilterChipComponent(
                        label = label,
                        isSelected = isSelected,
                        onClick = { viewModel.setFilters(label, filterSumber) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Sumber Row
            Text(
                text = "Sumber Saldo",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Semua", "Dompet", "SeaBank").forEach { label ->
                    val isSelected = filterSumber == label
                    FilterChipComponent(
                        label = label,
                        isSelected = isSelected,
                        onClick = { viewModel.setFilters(filterJenis, label) }
                    )
                }
            }
        }

        // Transactions List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = "No items",
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tidak ada transaksi yang cocok",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
            ) {
                items(filteredList, key = { it.id }) { tx ->
                    EditableTransactionRow(tx = tx, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FilterChipComponent(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF6366F1) else Color(0xFF334155),
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun EditableTransactionRow(tx: TransactionEntity, viewModel: FinanceViewModel) {
    val isPemasukan = tx.jenis == "pemasukan"
    val isSetup = tx.kategori == "Setup"
    val isAuto = tx.isAuto
    val isManual = !isSetup && !isAuto

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon representation
                val iconBg = if (isPemasukan) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                val iconColor = if (isPemasukan) Color(0xFF10B981) else Color(0xFFEF4444)
                val iconVec = when {
                    isSetup -> Icons.Default.PlaylistAddCheck
                    isAuto -> Icons.Default.AutoMode
                    isPemasukan -> Icons.Default.TrendingUp
                    else -> Icons.Default.TrendingDown
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconBg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVec,
                            contentDescription = tx.jenis,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tx.nama,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (isAuto) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OTOMATIS",
                                color = Color(0xFF6366F1),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        } else if (isSetup) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Awal",
                                color = Color(0xFF10B981),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${tx.kategori} • ${tx.sumber.replaceFirstChar { it.uppercase() }}",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    val prefix = if (isPemasukan) "+" else "-"
                    val amountColor = if (isPemasukan) Color(0xFF10B981) else Color(0xFFEF4444)
                    Text(
                        text = "$prefix${formatRupiah(tx.nominal)}",
                        color = amountColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tx.tanggal,
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 10.sp
                    )
                }
            }

            if (tx.catatan.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tx.catatan,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Manual actions (Edit and Delete triggers)
            if (isManual) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Trigger button
                    IconButton(
                        onClick = { viewModel.startEditTransaction(tx) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Transaksi",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delete Trigger button
                    IconButton(
                        onClick = { viewModel.requestDeleteTransaction(tx) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Transaksi",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
