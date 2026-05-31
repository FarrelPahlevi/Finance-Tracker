package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TransactionFormScreen(
    viewModel: FinanceViewModel,
    editTx: TransactionEntity? = null
) {
    val isEditing = editTx != null
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // State bindings
    var jenisState by remember { mutableStateOf(editTx?.jenis ?: "pengeluaran") } // "pemasukan" or "pengeluaran"
    var namaState by remember { mutableStateOf(editTx?.nama ?: "") }
    var nominalState by remember { mutableStateOf(editTx?.nominal?.toLong()?.toString() ?: "") }
    var kategoriState by remember { mutableStateOf(editTx?.kategori ?: "") }
    var sumberState by remember { mutableStateOf(editTx?.sumber ?: "dompet") } // "dompet" or "seabank"
    var tanggalState by remember {
        mutableStateOf(
            editTx?.tanggal ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
    }
    var catatanState by remember { mutableStateOf(editTx?.catatan ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var loadingDialog by remember { mutableStateOf(false) }

    // Categories lists
    val pengeluaranCategories = listOf(
        "Makanan & Minuman",
        "Transportasi",
        "Belanja",
        "Tagihan & Utilitas",
        "Hiburan",
        "Kesehatan",
        "Pendidikan",
        "Lainnya"
    )

    val pemasukanCategories = listOf(
        "Gaji",
        "Freelance / Usaha",
        "Bonus",
        "Cashback",
        "Transfer Masuk",
        "Hadiah",
        "Lainnya"
    )

    val activeCategoriesList = if (jenisState == "pemasukan") pemasukanCategories else pengeluaranCategories

    // Autofill default category when list updates if nothing selected
    LaunchedEffect(jenisState) {
        if (!activeCategoriesList.contains(kategoriState)) {
            kategoriState = activeCategoriesList.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        // Form Title bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditing) {
                    IconButton(
                        onClick = { viewModel.dismissEditTransaction() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = if (isEditing) "Edit Transaksi" else "Tambah Transaksi Baru",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Transaction Type Toggle Selector (Disabled in Edit Mode)
            if (!isEditing) {
                Column {
                    Text(
                        text = "Jenis Transaksi",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Expense Button
                        Button(
                            onClick = { jenisState = "pengeluaran" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (jenisState == "pengeluaran") Color(0xFFEF4444) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Outflow")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pengeluaran", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Income Button
                        Button(
                            onClick = { jenisState = "pemasukan" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (jenisState == "pemasukan") Color(0xFF10B981) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Inflow")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pemasukan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // Readonly indicator if editing
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val color = if (jenisState == "pemasukan") Color(0xFF10B981) else Color(0xFFEF4444)
                        Icon(
                            imageVector = if (jenisState == "pemasukan") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = "Lock icon",
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Jenis: ${jenisState.uppercase()} (Terkunci saat edit)",
                            color = color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Transaction Name Input Field
            OutlinedTextField(
                value = namaState,
                onValueChange = { namaState = it },
                label = { Text("Nama Transaksi", color = Color.White.copy(alpha = 0.6f)) },
                placeholder = { Text("Belanja bulanan, gaji, dll.", color = Color.White.copy(alpha = 0.3f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Nominal Input Field
            OutlinedTextField(
                value = nominalState,
                onValueChange = { input ->
                    if (input.all { char -> char.isDigit() }) {
                        nominalState = input
                    }
                },
                label = { Text("Nominal Transaksi", color = Color.White.copy(alpha = 0.6f)) },
                placeholder = { Text("0", color = Color.White.copy(alpha = 0.3f)) },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = if (jenisState == "pemasukan") Color(0xFF10B981) else Color(0xFFEF4444)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Source Account Toggle Selector
            Column {
                Text(
                    text = if (jenisState == "pemasukan") "Masuk Ke " else "Keluar Dari",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Wallet
                    Button(
                        onClick = { sumberState = "dompet" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sumberState == "dompet") Color(0xFF10B981) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Dompet")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dompet / Cash", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // SeaBank
                    Button(
                        onClick = { sumberState = "seabank" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sumberState == "seabank") Color(0xFFF97316) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "SeaBank")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SeaBank", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Category Dropdown Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = kategoriState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori", color = Color.White.copy(alpha = 0.6f)) },
                    trailingIcon = {
                        IconButton(onClick = { categoryDropdownExpanded = true }) {
                            Icon(
                                imageVector = if (categoryDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle dropdown",
                                tint = Color.White
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF818CF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryDropdownExpanded = true }
                )

                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF1E293B))
                ) {
                    activeCategoriesList.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, color = Color.White) },
                            onClick = {
                                kategoriState = category
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 6. Date Picker Input
            OutlinedTextField(
                value = tanggalState,
                onValueChange = { tanggalState = it },
                label = { Text("Tanggal Transaksi (yyyy-MM-dd)", color = Color.White.copy(alpha = 0.6f)) },
                trailingIcon = {
                    IconButton(onClick = {
                        val calendar = Calendar.getInstance()
                        // parse existing date if possible
                        try {
                            val parts = tanggalState.split("-")
                            if (parts.size == 3) {
                                calendar.set(Calendar.YEAR, parts[0].toInt())
                                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                            }
                        } catch (e: Exception) {}

                        DatePickerDialog(
                            context,
                            { _, year, monthOfYear, dayOfMonth ->
                                val finalMonth = monthOfYear + 1
                                val formattedMonth = if (finalMonth < 10) "0$finalMonth" else "$finalMonth"
                                val formattedDay = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                                tanggalState = "$year-$formattedMonth-$formattedDay"
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = Color.White)
                    }
                },
                placeholder = { Text("Format: yyyy-MM-dd", color = Color.White.copy(alpha = 0.3f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 7. Optional Notes notes field
            OutlinedTextField(
                value = catatanState,
                onValueChange = { catatanState = it },
                label = { Text("Catatan Opsional", color = Color.White.copy(alpha = 0.6f)) },
                placeholder = { Text("Misal: beli pulsa, bensin shopee, dll.", color = Color.White.copy(alpha = 0.3f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Execute submit button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val amount = nominalState.toDoubleOrNull()
                    if (namaState.isEmpty()) {
                        viewModel.showToast("Nama transaksi tidak boleh kosong", isError = true)
                        return@Button
                    }
                    if (amount == null || amount <= 0) {
                        viewModel.showToast("Nominal transaksi tidak valid", isError = true)
                        return@Button
                    }
                    if (tanggalState.isEmpty()) {
                        viewModel.showToast("Tanggal tidak boleh kosong", isError = true)
                        return@Button
                    }

                    // Run add or edit action
                    val success = if (isEditing) {
                        viewModel.updateTransaction(
                            oldTx = editTx!!,
                            nama = namaState,
                            nominal = amount,
                            kategori = kategoriState,
                            sumber = sumberState,
                            tanggal = tanggalState,
                            catatan = catatanState
                        )
                    } else {
                        viewModel.addTransaction(
                            jenis = jenisState,
                            nama = namaState,
                            nominal = amount,
                            kategori = kategoriState,
                            sumber = sumberState,
                            tanggal = tanggalState,
                            catatan = catatanState
                        )
                    }

                    if (success && !isEditing) {
                        // Reset forms when successful added
                        namaState = ""
                        nominalState = ""
                        catatanState = ""
                        viewModel.setScreen("home")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (jenisState == "pemasukan") Color(0xFF10B981) else Color(0xFF6366F1), // Emerald vs Indigo accent
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isEditing) "Simpan Perubahan" else "Catat Transaksi",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
