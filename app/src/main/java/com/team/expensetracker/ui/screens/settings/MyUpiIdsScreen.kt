package com.team.expensetracker.ui.screens.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.team.expensetracker.ui.theme.AccentMint
import com.team.expensetracker.ui.theme.BackgroundDark
import com.team.expensetracker.ui.theme.BorderDark
import com.team.expensetracker.ui.theme.DebitRed
import com.team.expensetracker.ui.theme.ExpenseTrackerTheme
import com.team.expensetracker.ui.theme.OnAccentMint
import com.team.expensetracker.ui.theme.SurfaceDark
import com.team.expensetracker.ui.theme.SurfaceVariantDark
import com.team.expensetracker.ui.theme.TextPrimary
import com.team.expensetracker.ui.theme.TextSecondary
import com.team.expensetracker.ui.theme.TextTertiary
import com.team.expensetracker.ui.viewmodel.ExpenseViewModelFactory
import com.team.expensetracker.ui.viewmodel.MyUpiIdsViewModel

private val UPI_REGEX = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")

/**
 * ViewModel-backed entry point for MyUpiIdsScreen connecting to [MyUpiIdsViewModel].
 */
@Composable
fun MyUpiIdsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyUpiIdsViewModel = viewModel(factory = ExpenseViewModelFactory.create(LocalContext.current))
) {
    val upiEntities by viewModel.upiIds.collectAsState()
    val upiIds = remember(upiEntities) { upiEntities.map { it.upiId } }
    MyUpiIdsScreen(
        upiIds = upiIds,
        onAdd = { viewModel.addUpiId(it) },
        onRemove = { viewModel.removeUpiId(it) },
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * MyUpiIdsScreen allows managing user-owned UPI IDs to exclude self-transfers from spending totals.
 *
 * Implements pure state hoisting: takes data + lambdas, no ViewModel coupling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyUpiIdsScreen(
    upiIds: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var upiInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My UPI IDs",
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Helper Informational Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariantDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = AccentMint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Transfers between your own IDs are not counted as spending.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Add UPI ID Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Add Your UPI ID",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = {
                            upiInput = it
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. name@okhdfcbank", color = TextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.QrCode,
                                contentDescription = null,
                                tint = AccentMint
                            )
                        },
                        singleLine = true,
                        isError = errorMessage != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            errorContainerColor = SurfaceDark,
                            focusedBorderColor = AccentMint,
                            unfocusedBorderColor = BorderDark,
                            errorBorderColor = DebitRed,
                            cursorColor = AccentMint,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Button(
                        onClick = {
                            val trimmed = upiInput.trim()
                            if (!trimmed.matches(UPI_REGEX)) {
                                errorMessage = "Invalid UPI ID format (use name@bank)"
                                return@Button
                            }
                            if (upiIds.any { it.equals(trimmed, ignoreCase = true) }) {
                                errorMessage = "This UPI ID is already added"
                                return@Button
                            }
                            onAdd(trimmed)
                            upiInput = ""
                            errorMessage = null
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentMint,
                            contentColor = OnAccentMint
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add UPI ID",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = DebitRed),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // List of User UPI IDs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configured Identifiers",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${upiIds.size} IDs",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                )
            }

            if (upiIds.isEmpty()) {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No UPI IDs added yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Add your personal UPI IDs above to ignore self-transfers.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = upiIds, key = { it }) { upiId ->
                        UpiIdListItem(
                            upiId = upiId,
                            onDelete = { onRemove(upiId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpiIdListItem(
    upiId: String,
    onDelete: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode,
                        contentDescription = null,
                        tint = AccentMint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = upiId,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete $upiId",
                    tint = DebitRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyUpiIdsScreenPreview() {
    ExpenseTrackerTheme {
        MyUpiIdsScreen(
            upiIds = listOf("user@okhdfcbank", "user@axisbank"),
            onAdd = {},
            onRemove = {},
            onBack = {}
        )
    }
}
