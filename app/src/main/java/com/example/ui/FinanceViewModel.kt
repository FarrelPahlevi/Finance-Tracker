package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.TransactionEntity
import com.example.utils.Balances
import com.example.utils.calculateNewBalances
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ToastMessage(val message: String, val isError: Boolean = false)

class FinanceViewModel(
    private val database: AppDatabase,
    private val prefs: AppPreferences
) : ViewModel() {

    private val _isSetupCompleted = MutableStateFlow<Boolean?>(null)
    val isSetupCompleted: StateFlow<Boolean?> = _isSetupCompleted.asStateFlow()

    private val _balanceDompet = MutableStateFlow(0.0)
    val balanceDompet: StateFlow<Double> = _balanceDompet.asStateFlow()

    private val _balanceSeaBank = MutableStateFlow(0.0)
    val balanceSeaBank: StateFlow<Double> = _balanceSeaBank.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    // Active Screen state: "home", "add", "history", "settings"
    private val _activeScreen = MutableStateFlow("home")
    val activeScreen: StateFlow<String> = _activeScreen.asStateFlow()

    // Filters for execution in memory
    private val _filterJenis = MutableStateFlow("Semua")
    val filterJenis: StateFlow<String> = _filterJenis.asStateFlow()

    private val _filterSumber = MutableStateFlow("Semua")
    val filterSumber: StateFlow<String> = _filterSumber.asStateFlow()

    // Popups/Dialogs UI states
    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private val _confirmDeleteTransaction = MutableStateFlow<TransactionEntity?>(null)
    val confirmDeleteTransaction: StateFlow<TransactionEntity?> = _confirmDeleteTransaction.asStateFlow()

    private val _confirmReset = MutableStateFlow(false)
    val confirmReset: StateFlow<Boolean> = _confirmReset.asStateFlow()

    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    val editingTransaction: StateFlow<TransactionEntity?> = _editingTransaction.asStateFlow()

    // Setup dates helper function to format & parse yyyy-MM-dd
    private fun getSdf(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            // Load setup completion state
            val completed = prefs.isSetupCompleted
            _isSetupCompleted.value = completed

            if (completed) {
                _balanceDompet.value = prefs.balanceDompet
                _balanceSeaBank.value = prefs.balanceSeaBank
                
                // Trigger daily interest calculations safely ONCE here, outside the database flow collection to prevent infinite loops
                triggerDailyInterestIncremental()

                // Fetch transactions from Room DB
                database.transactionDao.getAllTransactionsFlow().collect { list ->
                    _transactions.value = list
                }
            } else {
                _isSetupCompleted.value = false
            }
        }
    }

    private fun triggerDailyInterestIncremental() {
        val lastDateStr = prefs.lastInterestDate ?: return
        try {
            val sdf = getSdf()
            val lastDate = sdf.parse(lastDateStr) ?: return
            val today = Date()

            // Normalize calendar components to compare purely by dates
            val calLast = Calendar.getInstance().apply {
                time = lastDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val calToday = Calendar.getInstance().apply {
                time = today
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Capping: If the last interest date is older than 30 days or corrupt,
            // we cap the backfill window to 30 days to protect memory, avoid heavy CPU blocks,
            // and prevent database bloating with hundreds of daily records.
            val ageMs = calToday.timeInMillis - calLast.timeInMillis
            val maxBackfillMs = 30L * 24L * 60L * 60L * 1000L // 30 Days
            if (ageMs > maxBackfillMs) {
                calLast.timeInMillis = calToday.timeInMillis - maxBackfillMs
                calLast.set(Calendar.HOUR_OF_DAY, 0)
                calLast.set(Calendar.MINUTE, 0)
                calLast.set(Calendar.SECOND, 0)
                calLast.set(Calendar.MILLISECOND, 0)
            }

            if (calToday.after(calLast)) {
                calculateAndApplyInterest(calLast, calToday)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateAndApplyInterest(startCal: Calendar, endCal: Calendar) {
        viewModelScope.launch {
            try {
                // Offload database writes and date computation to Dispatchers.IO to keep the Main Thread completely responsive
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val sdf = getSdf()
                    val tempCal = (startCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    var currentSeaBank = _balanceSeaBank.value
                    val addedTxs = mutableListOf<TransactionEntity>()

                    // Extra guard for max iterations to prevent any potential infinite loops
                    var loopCount = 0
                    while (!tempCal.after(endCal) && loopCount < 45) {
                        loopCount++
                        // SeaBank interest: 2.5% p.a.
                        val interestRate = 0.025
                        val divisor = 365.0
                        val rawDailyInterest = (currentSeaBank * interestRate) / divisor

                        // Tax of 20% if balance is over Rp 7.500.000
                        val finalDailyInterest = if (currentSeaBank > 7500000.0) {
                            rawDailyInterest * 0.8
                        } else {
                            rawDailyInterest
                        }

                        // Round daily interest because banks credit integer rupiah and standard round-half-up applies
                        val roundedDailyInterest = java.lang.Math.round(finalDailyInterest).toDouble()

                        if (roundedDailyInterest > 0.0) {
                            currentSeaBank += roundedDailyInterest
                            val dateFormatted = sdf.format(tempCal.time)
                            val interestTx = TransactionEntity(
                                id = "auto-interest-${dateFormatted}-${System.currentTimeMillis()}-${(100..999).random()}",
                                jenis = "pemasukan",
                                nama = "Bunga Tabungan",
                                kategori = "Bunga",
                                nominal = roundedDailyInterest,
                                sumber = "seabank",
                                tanggal = dateFormatted,
                                catatan = "Auto calculate harian",
                                isAuto = true
                            )
                            addedTxs.add(interestTx)
                        }
                        tempCal.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    if (addedTxs.isNotEmpty()) {
                        // Bulk insert all auto-calculated daily interest transactions
                        addedTxs.forEach { database.transactionDao.insertTransaction(it) }
                        
                        // Commit values to SharedPreferences securely
                        prefs.balanceSeaBank = currentSeaBank
                        
                        // Update StateFlow & Toast back on the Main UI dispatcher
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _balanceSeaBank.value = currentSeaBank
                            val totalAdded = addedTxs.sumOf { it.nominal }
                            showToast("Bunga SeaBank otomatis diterapkan: +${formatRupiah(totalAdded)}")
                        }
                    }

                    prefs.lastInterestDate = sdf.format(endCal.time)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showToast("Gagal menerapkan perhitungan bunga otomatis", isError = true)
                }
            }
        }
    }

    fun setupInitialBalances(walletInitial: Double, seabankInitial: Double) {
        viewModelScope.launch {
            try {
                prefs.balanceDompet = walletInitial
                prefs.balanceSeaBank = seabankInitial
                prefs.isSetupCompleted = true
                
                val todayStr = getSdf().format(Date())
                prefs.lastInterestDate = todayStr

                _balanceDompet.value = walletInitial
                _balanceSeaBank.value = seabankInitial

                // Dynamic setup base transactions
                val dompetSetup = TransactionEntity(
                    id = "setup-dompet-${System.currentTimeMillis()}",
                    jenis = "pemasukan",
                    nama = "Saldo Awal Dompet",
                    kategori = "Setup",
                    nominal = walletInitial,
                    sumber = "dompet",
                    tanggal = todayStr,
                    catatan = "Baseline saldo awal Dompet"
                )

                val seabankSetup = TransactionEntity(
                    id = "setup-seabank-${System.currentTimeMillis()}",
                    jenis = "pemasukan",
                    nama = "Saldo Awal SeaBank",
                    kategori = "Setup",
                    nominal = seabankInitial,
                    sumber = "seabank",
                    tanggal = todayStr,
                    catatan = "Baseline saldo awal SeaBank"
                )

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.transactionDao.insertTransaction(dompetSetup)
                    database.transactionDao.insertTransaction(seabankSetup)
                }

                _isSetupCompleted.value = true
                showToast("Setup berhasil disimpan")
                setScreen("home")
                
                // Collect transactions flow
                database.transactionDao.getAllTransactionsFlow().collect { list ->
                    _transactions.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal memulai saldo awal", isError = true)
            }
        }
    }

    fun setScreen(screen: String) {
        _activeScreen.value = screen
    }

    fun setFilters(jenis: String, sumber: String) {
        _filterJenis.value = jenis
        _filterSumber.value = sumber
    }

    fun showToast(message: String, isError: Boolean = false) {
        _toast.value = ToastMessage(message, isError)
    }

    fun dismissToast() {
        _toast.value = null
    }

    fun requestDeleteTransaction(tx: TransactionEntity) {
        _confirmDeleteTransaction.value = tx
    }

    fun dismissDeleteConfirmation() {
        _confirmDeleteTransaction.value = null
    }

    fun requestReset() {
        _confirmReset.value = true
    }

    fun dismissResetConfirmation() {
        _confirmReset.value = false
    }

    fun startEditTransaction(tx: TransactionEntity) {
        _editingTransaction.value = tx
    }

    fun dismissEditTransaction() {
        _editingTransaction.value = null
    }

    // Core transaction adding
    fun addTransaction(
        jenis: String, // "pemasukan" or "pengeluaran"
        nama: String,
        nominal: Double,
        kategori: String,
        sumber: String, // "dompet" or "seabank"
        tanggal: String,
        catatan: String
    ): Boolean {
        // Construct transaction
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            jenis = jenis,
            nama = nama,
            kategori = kategori,
            nominal = nominal,
            sumber = sumber,
            tanggal = tanggal,
            catatan = catatan
        )

        val currentBalances = Balances(_balanceDompet.value, _balanceSeaBank.value)
        val newBalances = calculateNewBalances(currentBalances, tx, "apply")

        // Reject if balances go negative
        if (newBalances.dompet < 0.0) {
            showToast("Saldo Dompet tidak cukup", isError = true)
            return false
        }
        if (newBalances.seabank < 0.0) {
            showToast("Saldo SeaBank tidak cukup", isError = true)
            return false
        }

        viewModelScope.launch {
            try {
                // Apply balances
                prefs.balanceDompet = newBalances.dompet
                prefs.balanceSeaBank = newBalances.seabank
                _balanceDompet.value = newBalances.dompet
                _balanceSeaBank.value = newBalances.seabank

                // Insert into the Room Database on IO dispatcher
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.transactionDao.insertTransaction(tx)
                }
                showToast("Transaksi berhasil disimpan")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal menyimpan transaksi", isError = true)
            }
        }

        return true
    }

    // Core transaction editing
    fun updateTransaction(
        oldTx: TransactionEntity,
        nama: String,
        nominal: Double,
        kategori: String,
        sumber: String,
        tanggal: String,
        catatan: String
    ): Boolean {
        val currentBalances = Balances(_balanceDompet.value, _balanceSeaBank.value)
        // Step 1: Revert old tx
        val revertedBalances = calculateNewBalances(currentBalances, oldTx, "revert")

        // Step 2: Formulate new tx
        val newTx = oldTx.copy(
            nama = nama,
            nominal = nominal,
            kategori = kategori,
            sumber = sumber,
            tanggal = tanggal,
            catatan = catatan
        )

        // Step 3: Apply new tx
        val finalBalances = calculateNewBalances(revertedBalances, newTx, "apply")

        // Reject changes if resulting balances go below zero
        if (finalBalances.dompet < 0.0) {
            showToast("Perubahan ditolak: Saldo Dompet tidak cukup", isError = true)
            return false
        }
        if (finalBalances.seabank < 0.0) {
            showToast("Perubahan ditolak: Saldo SeaBank tidak cukup", isError = true)
            return false
        }

        viewModelScope.launch {
            try {
                // Save final balances
                prefs.balanceDompet = finalBalances.dompet
                prefs.balanceSeaBank = finalBalances.seabank
                _balanceDompet.value = finalBalances.dompet
                _balanceSeaBank.value = finalBalances.seabank

                // Replace in Database on IO dispatcher
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.transactionDao.insertTransaction(newTx)
                }
                showToast("Transaksi berhasil diperbarui")
                dismissEditTransaction()
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal memperbarui transaksi", isError = true)
            }
        }

        return true
    }

    // Core transaction deleting
    fun deleteTransactionConfirmed(tx: TransactionEntity) {
        val currentBalances = Balances(_balanceDompet.value, _balanceSeaBank.value)
        // Revert transaction effect
        val revertedBalances = calculateNewBalances(currentBalances, tx, "revert")

        // Reject if any balance becomes negative
        if (revertedBalances.dompet < 0.0) {
            showToast("Perubahan ditolak: Saldo Dompet tidak mencukupi untuk dikembalikan", isError = true)
            dismissDeleteConfirmation()
            return
        }
        if (revertedBalances.seabank < 0.0) {
            showToast("Perubahan ditolak: Saldo SeaBank tidak mencukupi untuk dikembalikan", isError = true)
            dismissDeleteConfirmation()
            return
        }

        viewModelScope.launch {
            try {
                prefs.balanceDompet = revertedBalances.dompet
                prefs.balanceSeaBank = revertedBalances.seabank
                _balanceDompet.value = revertedBalances.dompet
                _balanceSeaBank.value = revertedBalances.seabank

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.transactionDao.deleteTransaction(tx)
                }
                showToast("Transaksi berhasil dihapus")
                dismissDeleteConfirmation()
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal menghapus transaksi", isError = true)
                dismissDeleteConfirmation()
            }
        }
    }

    // Reset app
    fun resetApplication() {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    database.transactionDao.deleteAllTransactions()
                }
                prefs.resetAll()

                _balanceDompet.value = 0.0
                _balanceSeaBank.value = 0.0
                _transactions.value = emptyList()
                _isSetupCompleted.value = false
                _activeScreen.value = "home"
                dismissResetConfirmation()
                showToast("Semua data berhasil direset")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Gagal mereset data", isError = true)
                dismissResetConfirmation()
            }
        }
    }

    // Export JSON backup data
    fun getBackupJson(): String {
        val root = JSONObject()
        root.put("setup_completed", true)
        
        // Save balances
        val balancesObj = JSONObject()
        balancesObj.put("dompet", _balanceDompet.value)
        balancesObj.put("seabank", _balanceSeaBank.value)
        root.put("balances", balancesObj)

        root.put("lastInterestDate", prefs.lastInterestDate ?: "")
        root.put("exportedAt", System.currentTimeMillis())

        // Save transactions list
        val txArray = JSONArray()
        _transactions.value.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("jenis", tx.jenis)
            obj.put("nama", tx.nama)
            obj.put("kategori", tx.kategori)
            obj.put("nominal", tx.nominal)
            obj.put("sumber", tx.sumber)
            obj.put("tanggal", tx.tanggal)
            obj.put("catatan", tx.catatan)
            obj.put("isAuto", tx.isAuto)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        return root.toString(4)
    }

    // Import JSON backup data
    fun importBackupJson(jsonString: String): Boolean {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("balances") || !root.has("transactions")) {
                showToast("File backup tidak valid", isError = true)
                return false
            }

            val balancesObj = root.optJSONObject("balances") ?: return false
            val dompetVal = balancesObj.optDouble("dompet", 0.0)
            val seabankVal = balancesObj.optDouble("seabank", 0.0)

            val lastDate = root.optString("lastInterestDate", "")

            val txArray = root.optJSONArray("transactions") ?: return false
            val txList = mutableListOf<TransactionEntity>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                txList.add(
                    TransactionEntity(
                        id = obj.getString("id"),
                        jenis = obj.getString("jenis"),
                        nama = obj.getString("nama"),
                        kategori = obj.getString("kategori"),
                        nominal = obj.getDouble("nominal"),
                        sumber = obj.getString("sumber"),
                        tanggal = obj.getString("tanggal"),
                        catatan = obj.optString("catatan", ""),
                        isAuto = obj.optBoolean("isAuto", false)
                    )
                )
            }

            viewModelScope.launch {
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        // Clear old transactions, update state
                        database.transactionDao.deleteAllTransactions()
                        txList.forEach { database.transactionDao.insertTransaction(it) }
                    }

                    prefs.balanceDompet = dompetVal
                    prefs.balanceSeaBank = seabankVal
                    _balanceDompet.value = dompetVal
                    _balanceSeaBank.value = seabankVal

                    if (lastDate.isNotEmpty()) {
                        prefs.lastInterestDate = lastDate
                    } else {
                        prefs.lastInterestDate = getSdf().format(Date())
                    }

                    prefs.isSetupCompleted = true
                    _isSetupCompleted.value = true

                    showToast("Data berhasil diimport")
                    setScreen("home")
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Gagal memulihkan file backup", isError = true)
                }
            }
            return true
        } catch (e: Exception) {
            showToast("Format JSON salah atau rusak", isError = true)
            return false
        }
    }
}

// Utility formatting currencies Indonesian standard
fun formatRupiah(amount: Double): String {
    val integerPart = java.lang.Math.round(amount)
    val str = integerPart.toString()
    val builder = StringBuilder()
    var count = 0
    for (i in str.length - 1 downTo 0) {
        builder.append(str[i])
        count++
        if (count % 3 == 0 && i != 0) {
            builder.append('.')
        }
    }
    return "Rp " + builder.reverse().toString()
}
