package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinanceViewModel

@Composable
fun SetupScreen(viewModel: FinanceViewModel) {
    var dompetInput by remember { mutableStateFlowOf("") }
    var seabankInput by remember { mutableStateFlowOf("") }
    var validationError by remember { mutableStateFlowOf<String?>(null) }
    
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF020617)  // Slate 950
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo/Icon Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Slate 800
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .size(80.dp)
                    .padding(4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = "Logo Finance Tracker",
                        tint = Color(0xFF6366F1), // Indigo 500
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Selamat Datang!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Atur saldo awal Anda untuk memulai pencatatan keuangan pribadi dengan aman dan praktis.",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.60f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Konfigurasi Saldo Awal",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Wallet Field
                    OutlinedTextField(
                        value = dompetInput,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                dompetInput = it
                                validationError = null
                            }
                        },
                        label = { Text("Mulai Saldo Dompet (Cash)", color = Color.White.copy(alpha = 0.6f)) },
                        placeholder = { Text("Contoh: 1300000", color = Color.White.copy(alpha = 0.3f)) },
                        prefix = { Text("Rp ", color = Color(0xFF10B981), fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF818CF8),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // SeaBank Field
                    OutlinedTextField(
                        value = seabankInput,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                seabankInput = it
                                validationError = null
                            }
                        },
                        label = { Text("Mulai Saldo SeaBank", color = Color.White.copy(alpha = 0.6f)) },
                        placeholder = { Text("Contoh: 705000", color = Color.White.copy(alpha = 0.3f)) },
                        prefix = { Text("Rp ", color = Color(0xFFF97316), fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF818CF8),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = Color(0xFFEF4444), // Error Red
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    val dompetVal = dompetInput.toDoubleOrNull()
                    val seabankVal = seabankInput.toDoubleOrNull()

                    if (dompetVal == null || seabankVal == null) {
                        validationError = "Silakan isi kedua saldo awal dengan benar"
                    } else if (dompetVal < 0 || seabankVal < 0) {
                        validationError = "Saldo awal tidak boleh kurang dari 0"
                    } else {
                        viewModel.setupInitialBalances(dompetVal, seabankVal)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1), // Premium Indigo
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Mulai Mencatat Keuangan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// Custom remember method helper for state flow integration/creation in Compose
@Composable
fun <T> rememberStateFlowOf(value: T): MutableState<T> {
    return remember { mutableStateOf(value) }
}

fun <T> mutableStateFlowOf(value: T): MutableState<T> {
    return mutableStateOf(value)
}
