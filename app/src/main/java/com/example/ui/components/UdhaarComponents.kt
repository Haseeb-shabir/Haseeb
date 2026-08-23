package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.SyncState
import com.example.ui.model.UdhaarFormatter
import com.example.ui.theme.GreenPaid
import com.example.ui.theme.GreenPaidContainer
import com.example.ui.theme.RedPending
import com.example.ui.theme.RedPendingContainer

@Composable
fun SyncStatusBadge(
    syncState: SyncState,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, fg, text, icon) = when (syncState) {
        SyncState.SYNCED -> Quad(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Synced",
            Icons.Default.CloudDone
        )
        SyncState.SYNCING -> Quad(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            "Syncing...",
            Icons.Default.CloudSync
        )
        SyncState.OFFLINE -> Quad(
            Color(0xFFEEEEEE),
            Color(0xFF616161),
            "Offline",
            Icons.Default.CloudOff
        )
        SyncState.IDLE -> Quad(
            Color(0xFFE0F2F1),
            Color(0xFF00796B),
            "Team Sync",
            Icons.Default.CloudDone
        )
    }

    Surface(
        onClick = onSyncClick,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        modifier = modifier.testTag("sync_status_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            if (syncState == SyncState.SYNCING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = fg
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = "Sync Status",
                    tint = fg,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "PAID" -> Triple(GreenPaidContainer, GreenPaid, "PAID")
        "PARTIAL" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "PARTIAL")
        else -> Triple(RedPendingContainer, RedPending, "PENDING")
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ShopCodeCard(
    shopCode: String,
    shopName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("shop_code_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shopName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Shop Code: ",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = shopCode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Shop Code", shopCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Shop Code copied: $shopCode", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_shop_code_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my shop '$shopName' on Udhaar Tracker app! Our shared Shop Code is: $shopCode"
                            )
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Shop Code")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.testTag("share_shop_code_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Code",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun AppBottomNav(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = modifier.testTag("main_bottom_nav")
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.SpaceDashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard", fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_dashboard")
        )

        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Default.PeopleAlt, contentDescription = "Customers") },
            label = { Text("Customers", fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_customers")
        )

        // Center Highlighted Add Udhaar Tab
        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Udhaar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            label = {
                Text(
                    "+ Udhaar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent
            ),
            modifier = Modifier.testTag("nav_tab_add_udhaar")
        )

        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History", fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_history")
        )

        NavigationBarItem(
            selected = currentTab == 4,
            onClick = { onTabSelected(4) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}

@Composable
fun PartialPaymentDialog(
    customerName: String,
    pendingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Payment",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Customer: $customerName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Pending Balance: ${UdhaarFormatter.formatPKR(pendingAmount)}",
                    color = RedPending,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { char -> char.isDigit() || char == '.' }
                        errorText = null
                    },
                    label = { Text("Payment Amount (Rs.)") },
                    placeholder = { Text("e.g. 2000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { amountText = pendingAmount.toLong().toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pay Full (Rs. ${pendingAmount.toLong()})", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Method (Optional)") },
                    placeholder = { Text("e.g. Cash, JazzCash, EasyPaisa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorText = "Enter a valid amount"
                        return@Button
                    }
                    onConfirm(amount, noteText)
                },
                modifier = Modifier.testTag("confirm_payment_btn")
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
