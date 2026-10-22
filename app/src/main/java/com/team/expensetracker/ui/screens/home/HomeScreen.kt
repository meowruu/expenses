package com.team.expensetracker.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.expensetracker.data.CategorySummary
import com.team.expensetracker.data.FakeData
import com.team.expensetracker.data.model.Transaction
import com.team.expensetracker.data.model.TransactionType
import com.team.expensetracker.ui.theme.AccentMint
import com.team.expensetracker.ui.theme.BackgroundDark
import com.team.expensetracker.ui.theme.BorderDark
import com.team.expensetracker.ui.theme.CreditGreen
import com.team.expensetracker.ui.theme.DebitRed
import com.team.expensetracker.ui.theme.ExpenseTrackerTheme
import com.team.expensetracker.ui.theme.OnAccentMint
import com.team.expensetracker.ui.theme.SurfaceContainerHighest
import com.team.expensetracker.ui.theme.SurfaceDark
import com.team.expensetracker.ui.theme.SurfaceVariantDark
import com.team.expensetracker.ui.theme.TextPrimary
import com.team.expensetracker.ui.theme.TextSecondary
import com.team.expensetracker.ui.theme.TextTertiary
import com.team.expensetracker.ui.util.formatRupees
import com.team.expensetracker.ui.util.formattedAmount
import com.team.expensetracker.ui.util.formattedDateTime

private const val DEFAULT_SAMPLE_SMS =
    "Sent Rs.450.00 from Axis Bank A/C ending 1234 to SWIGGY on 22-10-26 via UPI. Ref: UPI/429482910394. Not you? SMS BLOCK to 5676782"

private val CHART_COLORS = listOf(
    Color(0xFF00E599), // Accent Mint
    Color(0xFF6366F1), // Indigo
    Color(0xFFEC4899), // Pink
    Color(0xFFF59E0B), // Amber
    Color(0xFF38BDF8), // Sky
    Color(0xFF8B5CF6), // Purple
    Color(0xFF14B8A6), // Teal
    Color(0xFFFB923C)  // Orange
)

/**
 * HomeScreen displays this month's total spending, a Compose Canvas category donut chart,
 * category summaries, and a reactive LazyColumn of recent transactions.
 *
 * Implements pure state hoisting: accepts data and callbacks without ViewModel coupling.
 */
@Composable
fun HomeScreen(
    thisMonthSpentPaise: Long,
    categoryTotals: Map<String, Long>,
    categorySummaries: List<CategorySummary>,
    recentTransactions: List<Transaction>,
    onTransactionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSimulateSms: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSimulateSmsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = AccentMint,
                contentColor = OnAccentMint,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add manual expense",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top App Bar Header with Simulate and Settings buttons
            item {
                HomeHeader(
                    onSimulateClick = { showSimulateSmsDialog = true },
                    onSettingsClick = onSettingsClick
                )
            }

            // Hero Monthly Total Spending Card
            item {
                MonthlyTotalCard(thisMonthSpentPaise = thisMonthSpentPaise)
            }

            // Compose Canvas Category Distribution Chart
            item {
                CategorySpendingChart(categoryTotals = categoryTotals)
            }

            // Category-wise Summary Section
            item {
                CategorySummarySection(categorySummaries = categorySummaries)
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${recentTransactions.size} records",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextTertiary
                        )
                    )
                }
            }

            // Recent Transactions List
            if (recentTransactions.isEmpty()) {
                item {
                    EmptyTransactionsPlaceholder()
                }
            } else {
                items(
                    items = recentTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }

    // Simulate SMS Modal Dialog
    if (showSimulateSmsDialog) {
        SimulateSmsDialog(
            onDismiss = { showSimulateSmsDialog = false },
            onSend = { smsBody ->
                showSimulateSmsDialog = false
                onSimulateSms(smsBody)
            }
        )
    }
}

@Composable
private fun HomeHeader(
    onSimulateClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "ExpenseTracker",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = AccentMint,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "100% On-Device",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AccentMint,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Simulate SMS Button
            OutlinedButton(
                onClick = onSimulateClick,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint = AccentMint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Simulate",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // Settings Icon Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthlyTotalCard(thisMonthSpentPaise: Long) {
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
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentMint)
                )
                Text(
                    text = "SPENT THIS MONTH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formatRupees(thisMonthSpentPaise),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = AccentMint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Calculated securely from verified UPI SMS entries",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }
        }
    }
}

/**
 * Compose Canvas Donut Chart rendering category-wise expenditure breakdown.
 * Uses pure Canvas drawArc without any third-party chart libraries.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySpendingChart(
    categoryTotals: Map<String, Long>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )

            val totalPaise = remember(categoryTotals) {
                categoryTotals.values.sum()
            }

            if (categoryTotals.isEmpty() || totalPaise <= 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No category spending recorded yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            } else {
                val sortedEntries = remember(categoryTotals) {
                    categoryTotals.entries.sortedByDescending { it.value }
                }

                val strokeWidth = with(LocalDensity.current) { 24.dp.toPx() }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            var currentStartAngle = -90f
                            sortedEntries.forEachIndexed { index, entry ->
                                val sweepAngle = (entry.value.toFloat() / totalPaise.toFloat()) * 360f
                                val color = CHART_COLORS[index % CHART_COLORS.size]

                                drawArc(
                                    color = color,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                currentStartAngle += sweepAngle
                            }
                        }

                        // Center content in Donut hole
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${sortedEntries.size}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    // Top Legend items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        sortedEntries.take(4).forEachIndexed { index, entry ->
                            val color = CHART_COLORS[index % CHART_COLORS.size]
                            val percent = (entry.value * 100 / totalPaise).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = entry.key,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "$percent%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextTertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummarySection(categorySummaries: List<CategorySummary>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Category Summary",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )

        if (categorySummaries.isEmpty()) {
            Text(
                text = "No category data available yet.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(categorySummaries) { summary ->
                    CategorySummaryCard(summary = summary)
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryCard(summary: CategorySummary) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier.width(150.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = summary.category,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text = summary.formattedTotal(),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "${summary.transactionCount} txn${if (summary.transactionCount > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary
                )
            )
        }
    }
}

@Composable
private fun TransactionListItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariantDark),
                    contentAlignment = Alignment.Center
                ) {
                    val isDebit = transaction.type == TransactionType.DEBIT
                    Icon(
                        imageVector = if (isDebit) Icons.AutoMirrored.Rounded.TrendingDown else Icons.AutoMirrored.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = if (isDebit) DebitRed else CreditGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = transaction.merchant ?: "Unknown Payee",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transaction.bank,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentMint,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                        )
                        Text(
                            text = transaction.formattedDateTime(),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isDebit = transaction.type == TransactionType.DEBIT
                val prefix = if (isDebit) "- " else "+ "
                Text(
                    text = prefix + transaction.formattedAmount(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDebit) TextPrimary else CreditGreen
                    )
                )

                Surface(
                    color = SurfaceContainerHighest,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulateSmsDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var smsText by remember { mutableStateOf(DEFAULT_SAMPLE_SMS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint = AccentMint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Simulate Bank SMS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Input a mock bank SMS to simulate local on-device ingestion and transaction parsing:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(smsText.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentMint,
                    contentColor = OnAccentMint
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Send", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun EmptyTransactionsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "No transactions recorded yet",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = "Tap the '+' button to log an expense manually",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextTertiary
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ExpenseTrackerTheme {
        HomeScreen(
            thisMonthSpentPaise = FakeData.calculateTotalSpent(FakeData.initialTransactions),
            categoryTotals = FakeData.calculateCategoryTotals(FakeData.initialTransactions),
            categorySummaries = FakeData.calculateCategorySummaries(FakeData.initialTransactions),
            recentTransactions = FakeData.initialTransactions,
            onTransactionClick = {},
            onAddClick = {},
            onSettingsClick = {},
            onSimulateSms = {}
        )
    }
}
