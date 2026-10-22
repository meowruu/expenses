package com.team.expensetracker.ui.screens.entry

import java.math.BigDecimal
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.team.expensetracker.data.FakeData
import com.team.expensetracker.parser.Categories
import com.team.expensetracker.ui.theme.ExpenseTrackerTheme
import com.team.expensetracker.ui.theme.AccentMint
import com.team.expensetracker.ui.theme.BackgroundDark
import com.team.expensetracker.ui.theme.BorderDark
import com.team.expensetracker.ui.theme.DebitRed
import com.team.expensetracker.ui.theme.OnAccentMint
import com.team.expensetracker.ui.theme.SurfaceContainerHighest
import com.team.expensetracker.ui.theme.SurfaceDark
import com.team.expensetracker.ui.theme.SurfaceVariantDark
import com.team.expensetracker.ui.theme.TextPrimary
import com.team.expensetracker.ui.theme.TextSecondary
import com.team.expensetracker.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import com.team.expensetracker.ui.viewmodel.ExpenseViewModelFactory
import com.team.expensetracker.ui.viewmodel.ManualEntryViewModel
import java.util.Date
import java.util.Locale

/**
 * ViewModel-backed entry point for ManualEntryScreen connecting to [ManualEntryViewModel].
 */
@Composable
fun ManualEntryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManualEntryViewModel = viewModel(factory = ExpenseViewModelFactory.create(LocalContext.current)),
    categories: List<String> = Categories.ALL.toList()
) {
    ManualEntryScreen(
        categories = categories,
        onSave = { amountPaise, note, category, dateMillis, upiId ->
            viewModel.addExpense(
                amountPaise = amountPaise,
                note = note,
                category = category,
                date = dateMillis,
                upiId = upiId
            ) { success ->
                if (success) {
                    onBack()
                }
            }
        },
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * ManualEntryScreen allows logging an expense manually.
 *
 * Implements pure state hoisting: takes data + lambdas, no ViewModel dependency.
 * Validates amount > 0 before calling onSave.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    categories: List<String>,
    onSave: (amountPaise: Long, note: String, category: String, dateMillis: Long, upiId: String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Other") }
    var upiIdInput by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Manual Expense",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Amount Input Card
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Amount (₹)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        // Allow digits and up to one decimal point
                        if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            amountInput = input
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.00", color = TextTertiary, fontSize = 24.sp) },
                    prefix = {
                        Text(
                            text = "₹ ",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentMint
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        errorContainerColor = SurfaceDark,
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = BorderDark,
                        errorBorderColor = DebitRed,
                        cursorColor = AccentMint
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = DebitRed),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            // Note / Merchant Field
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Note / Merchant",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = {
                        noteInput = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Swiggy, Groceries, Chai", color = TextTertiary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = BorderDark,
                        cursorColor = AccentMint,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Category Dropdown
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = null,
                                tint = AccentMint
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(SurfaceVariantDark)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (category == selectedCategory) AccentMint else TextPrimary,
                                            fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                },
                                leadingIcon = if (category == selectedCategory) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = AccentMint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // Date Picker Field
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                val formattedDate = remember(selectedDateMillis) {
                    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                    sdf.format(Date(selectedDateMillis))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = formattedDate,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = AccentMint
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = SurfaceDark,
                            disabledBorderColor = BorderDark,
                            disabledTextColor = TextPrimary,
                            disabledLeadingIconColor = AccentMint
                        )
                    )
                }
            }

            // Optional UPI ID Field
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "UPI ID (Optional)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                OutlinedTextField(
                    value = upiIdInput,
                    onValueChange = { upiIdInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. merchant@okaxis", color = TextTertiary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.QrCode,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = BorderDark,
                        cursorColor = AccentMint,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isFormValid = parseRupeesToPaise(amountInput) != null && noteInput.trim().isNotEmpty()

            // Save Button
            Button(
                onClick = {
                    val amountPaise = parseRupeesToPaise(amountInput)
                    if (amountPaise == null || amountPaise <= 0L) {
                        errorMessage = "Amount must be greater than ₹0.00"
                        return@Button
                    }
                    if (noteInput.trim().isEmpty()) {
                        errorMessage = "Please enter a note or merchant name"
                        return@Button
                    }

                    val cleanUpi = upiIdInput.trim().ifEmpty { null }

                    onSave(
                        amountPaise,
                        noteInput.trim(),
                        selectedCategory,
                        selectedDateMillis,
                        cleanUpi
                    )
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentMint,
                    contentColor = OnAccentMint
                )
            ) {
                Text(
                    text = "Save Expense",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = AccentMint, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ManualEntryScreenPreview() {
    ExpenseTrackerTheme {
        ManualEntryScreen(
            categories = FakeData.CATEGORIES,
            onSave = { _, _, _, _, _ -> },
            onBack = {}
        )
    }
}

/**
 * Safely converts a rupee amount string to paise using exact decimal math.
 * Rejects invalid strings, amounts <= 0, amounts with more than 2 decimal places,
 * and amounts that exceed [Long.MAX_VALUE] in paise.
 */
internal fun parseRupeesToPaise(input: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    // Support up to two decimal places; reject scientific notation or extra decimal places
    if (!trimmed.matches(Regex("^\\d+(\\.\\d{1,2})?$|^\\.\\d{1,2}$|^\\d+\\.$"))) {
        return null
    }

    val withoutTrailingDot = if (trimmed.endsWith(".")) trimmed.dropLast(1) else trimmed
    if (withoutTrailingDot.isEmpty()) return null

    val normalized = if (withoutTrailingDot.startsWith(".")) "0$withoutTrailingDot" else withoutTrailingDot

    return try {
        val bd = BigDecimal(normalized)
        if (bd.signum() <= 0) return null
        if (bd.scale() > 2) return null
        val paiseBd = bd.movePointRight(2)
        if (paiseBd > BigDecimal.valueOf(Long.MAX_VALUE)) return null
        val paise = paiseBd.longValueExact()
        if (paise <= 0L) null else paise
    } catch (_: Exception) {
        null
    }
}
