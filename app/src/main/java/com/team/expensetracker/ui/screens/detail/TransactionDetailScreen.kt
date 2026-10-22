package com.team.expensetracker.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.expensetracker.data.FakeData
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.ui.theme.AccentMint
import com.team.expensetracker.ui.theme.BackgroundDark
import com.team.expensetracker.ui.theme.BorderDark
import com.team.expensetracker.ui.theme.CreditGreen
import com.team.expensetracker.ui.theme.DebitRed
import com.team.expensetracker.ui.theme.ExpenseTrackerTheme
import com.team.expensetracker.ui.theme.SurfaceContainerHighest
import com.team.expensetracker.ui.theme.SurfaceDark
import com.team.expensetracker.ui.theme.SurfaceVariantDark
import com.team.expensetracker.ui.theme.TextPrimary
import com.team.expensetracker.ui.theme.TextSecondary
import com.team.expensetracker.ui.theme.TextTertiary
import com.team.expensetracker.ui.util.formattedAmount
import com.team.expensetracker.ui.util.formattedDateTime

/**
 * TransactionDetailScreen displays full metadata for a transaction and provides
 * a dropdown to update the expense category.
 *
 * Implements pure state hoisting: takes data + lambdas, no ViewModel coupling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: Transaction,
    categories: List<String>,
    onCategoryChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transaction Details",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Amount Card
            HeroAmountCard(transaction = transaction)

            // Category Management Section
            CategoryChangeCard(
                currentCategory = transaction.category,
                categories = categories,
                isExpanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it },
                onSelectCategory = { newCategory ->
                    onCategoryChange(newCategory)
                    categoryDropdownExpanded = false
                }
            )

            // Full Details Card
            FullDetailsCard(transaction = transaction)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroAmountCard(transaction: Transaction) {
    val isDebit = transaction.type == TransactionType.DEBIT

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Direction Badge
            Surface(
                color = SurfaceVariantDark,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isDebit) Icons.AutoMirrored.Rounded.TrendingDown else Icons.AutoMirrored.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = if (isDebit) DebitRed else CreditGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isDebit) "DEBIT (Spent)" else "CREDIT (Received)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDebit) DebitRed else CreditGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Formatted Rupee Amount
            Text(
                text = (if (isDebit) "- " else "+ ") + transaction.formattedAmount(),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )

            // Merchant / Description
            Text(
                text = transaction.merchant ?: "Unknown Payee",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChangeCard(
    currentCategory: String,
    categories: List<String>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectCategory: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Expense Category",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = onExpandedChange
            ) {
                OutlinedTextField(
                    value = currentCategory,
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
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { onExpandedChange(false) },
                    modifier = Modifier.background(SurfaceVariantDark)
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (category == currentCategory) AccentMint else TextPrimary,
                                        fontWeight = if (category == currentCategory) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            onClick = { onSelectCategory(category) },
                            leadingIcon = if (category == currentCategory) {
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

            Text(
                text = "Changes to this category update your personalized payee categorization rules.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun FullDetailsCard(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Full Information",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            DetailRow(
                icon = Icons.Rounded.Storefront,
                label = "Payee / Merchant",
                value = transaction.merchant ?: "Not specified"
            )

            DetailRow(
                icon = Icons.Rounded.QrCode,
                label = "UPI ID / VPA",
                value = transaction.upiId ?: "Not available (Direct Bank)"
            )

            DetailRow(
                icon = Icons.Rounded.Numbers,
                label = "Reference (UTR)",
                value = transaction.refNumber ?: "N/A"
            )

            DetailRow(
                icon = Icons.Rounded.AccountBalance,
                label = "Bank Account",
                value = "${transaction.bank} Bank"
            )

            DetailRow(
                icon = Icons.Rounded.CalendarToday,
                label = "Date & Time",
                value = transaction.formattedDateTime()
            )

            DetailRow(
                icon = Icons.Rounded.Fingerprint,
                label = "Storage Status",
                value = "100% On-Device (Raw SMS Discarded)"
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentMint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary
                )
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionDetailScreenPreview() {
    ExpenseTrackerTheme {
        TransactionDetailScreen(
            transaction = FakeData.initialTransactions.first(),
            categories = FakeData.CATEGORIES,
            onCategoryChange = {},
            onBack = {}
        )
    }
}
