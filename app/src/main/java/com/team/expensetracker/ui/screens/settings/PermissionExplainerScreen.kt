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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.expensetracker.ui.theme.AccentMint
import com.team.expensetracker.ui.theme.BackgroundDark
import com.team.expensetracker.ui.theme.BorderDark
import com.team.expensetracker.ui.theme.ExpenseTrackerTheme
import com.team.expensetracker.ui.theme.OnAccentMint
import com.team.expensetracker.ui.theme.SurfaceDark
import com.team.expensetracker.ui.theme.SurfaceVariantDark
import com.team.expensetracker.ui.theme.TextPrimary
import com.team.expensetracker.ui.theme.TextSecondary

/**
 * PermissionExplainerScreen informs the user about SMS auto-logging before
 * any runtime permission is requested.
 *
 * Implements pure state hoisting: accepts callbacks, no ViewModel coupling.
 *
 * NOTE — [onEnableAutoLogging] is a B3 integration placeholder.
 * Tapping "Turn on auto-logging" does NOT grant or request any permission yet.
 * B3 must replace the stub in AppNavHost with the real runtime permission
 * request (READ_SMS / RECEIVE_SMS) and the SMS receiver registration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionExplainerScreen(
    onEnableAutoLogging: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNotNow) {
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: icon + title + subtitle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                        .border(1.dp, BorderDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = AccentMint,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "Auto-log your bank SMS",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = "Let the app silently read bank messages and record your spending automatically — no manual entry needed.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Section 1: What we read
            ExplainerSection(
                icon = Icons.Rounded.Info,
                title = "What we read",
                body = "Only bank transaction SMS messages — debits, credits, and refunds from your bank. " +
                        "Promotional, personal, and OTP messages are completely ignored."
            )

            // Section 2: Why
            ExplainerSection(
                icon = Icons.Rounded.CheckCircle,
                title = "Why",
                body = "To automatically record every UPI and bank payment as a transaction the moment " +
                        "it happens, without you needing to open the app or type anything."
            )

            // Section 3: What we never do
            ExplainerSection(
                icon = Icons.Rounded.Block,
                title = "What we never do",
                body = "We never store the raw SMS text. We never send any data to a server. The app has " +
                        "zero internet permission. Only structured fields — amount, payee, date, reference " +
                        "number — are kept on this device."
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Primary action — B3 placeholder: no permission is granted by this button yet.
            // B3 must replace the onEnableAutoLogging stub in AppNavHost with the real
            // READ_SMS / RECEIVE_SMS runtime permission request.
            Button(
                onClick = onEnableAutoLogging,
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
                    text = "Turn on auto-logging",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Secondary dismiss action
            TextButton(
                onClick = onNotNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Not now",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * A single card section in the explainer: icon, title, and body text.
 */
@Composable
private fun ExplainerSection(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentMint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionExplainerScreenPreview() {
    ExpenseTrackerTheme {
        PermissionExplainerScreen(
            onEnableAutoLogging = {},
            onNotNow = {}
        )
    }
}
