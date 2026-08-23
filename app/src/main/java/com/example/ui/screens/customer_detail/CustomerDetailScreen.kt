package com.example.ui.screens.customer_detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.PartialPaymentDialog
import com.example.ui.components.StatusPill
import com.example.ui.model.CustomerWithBalance
import com.example.ui.model.UdhaarFormatter
import com.example.ui.theme.GreenPaid
import com.example.ui.theme.GreenPaidContainer
import com.example.ui.theme.NaturalPrimaryLight
import com.example.ui.theme.RedPending
import com.example.ui.theme.RedPendingContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerWithBalance: CustomerWithBalance,
    transactions: List<TransactionEntity>,
    onBackClick: () -> Unit,
    onMarkAsPaid: (transactionId: String) -> Unit,
    onRecordPartialPayment: (transactionId: String, amount: Double, note: String) -> Unit,
    onRecordGeneralPayment: (customerId: String, amount: Double, note: String) -> Unit,
    onAddUdhaarForCustomer: (customerName: String, phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customer = customerWithBalance.customer
    val isPending = customerWithBalance.remainingPending > 0.0

    var showGeneralPaymentDialog by remember { mutableStateOf(false) }
    var singleTxPayment by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = customer.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("btn_back_customer_detail")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${customer.phone}")
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call Customer", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                val cleanNumber = customer.phone.replace("+", "").replace(" ", "").replace("-", "")
                                val formatted = if (cleanNumber.startsWith("0")) "92" + cleanNumber.substring(1) else cleanNumber
                                val url = "https://api.whatsapp.com/send?phone=$formatted&text=Assalam-o-Alaikum ${customer.name}, your total pending udhaar balance is ${UdhaarFormatter.formatPKR(customerWithBalance.remainingPending)}."
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = GreenPaid)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Balance Summary Hero Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPending) MaterialTheme.colorScheme.primary else GreenPaid
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_balance_hero")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    if (isPending) {
                                        listOf(MaterialTheme.colorScheme.primary, Color(0xFF3F4D22))
                                    } else {
                                        listOf(GreenPaid, Color(0xFF334A25))
                                    }
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CURRENT PENDING BALANCE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                StatusPill(status = if (isPending) "PENDING" else "PAID")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = UdhaarFormatter.formatPKR(customerWithBalance.remainingPending),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.testTag("customer_pending_balance_text")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Grid of Total Udhaar vs Total Paid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Udhaar Given",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = UdhaarFormatter.formatPKR(customerWithBalance.totalUdhaar),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Amount Paid",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = UdhaarFormatter.formatPKR(customerWithBalance.totalPaid),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE8EAD3)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Primary Customer Actions: + Add Udhaar & Record Payment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onAddUdhaarForCustomer(customer.name, customer.phone) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_give_udhaar_customer")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Add Udhaar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                if (isPending) {
                                    FilledTonalButton(
                                        onClick = { showGeneralPaymentDialog = true },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.25f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_record_payment_customer")
                                    ) {
                                        Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record Payment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ledger Transaction History Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction History & Khata",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${transactions.size} records",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Transaction Cards
            if (transactions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap '+ Add Udhaar' above to record credit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    CustomerLedgerItem(
                        transaction = tx,
                        onMarkAsPaid = { onMarkAsPaid(tx.id) },
                        onPartialPayClick = { singleTxPayment = tx }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Single Transaction Partial Payment Dialog
    singleTxPayment?.let { tx ->
        PartialPaymentDialog(
            customerName = tx.customerName,
            pendingAmount = tx.remainingAmount,
            onDismiss = { singleTxPayment = null },
            onConfirm = { amount, note ->
                if (amount >= tx.remainingAmount) {
                    onMarkAsPaid(tx.id)
                } else {
                    onRecordPartialPayment(tx.id, amount, note)
                }
                singleTxPayment = null
            }
        )
    }

    // General Customer Payment Dialog
    if (showGeneralPaymentDialog) {
        PartialPaymentDialog(
            customerName = customer.name,
            pendingAmount = customerWithBalance.remainingPending,
            onDismiss = { showGeneralPaymentDialog = false },
            onConfirm = { amount, note ->
                onRecordGeneralPayment(customer.id, amount, note)
                showGeneralPaymentDialog = false
            }
        )
    }
}

@Composable
private fun CustomerLedgerItem(
    transaction: TransactionEntity,
    onMarkAsPaid: () -> Unit,
    onPartialPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPayment = transaction.type == "PAYMENT"
    val isPending = transaction.status != "PAID" && transaction.remainingAmount > 0.0

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ledger_item_${transaction.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Type & Description & Date
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPayment) "Payment Received" else "Udhaar Given",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isPayment) GreenPaid else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusPill(status = transaction.status)
                    }

                    if (transaction.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = UdhaarFormatter.formatDateTime(transaction.dateMillis),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Right Column: Amount & Status
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isPayment) "- " else "+ ") + UdhaarFormatter.formatPKR(transaction.amount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isPayment) GreenPaid else if (isPending) RedPending else GreenPaid
                    )

                    if (!isPayment && transaction.paidAmount > 0.0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Paid: ${UdhaarFormatter.formatPKR(transaction.paidAmount)}",
                            fontSize = 11.sp,
                            color = GreenPaid,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Remaining: ${UdhaarFormatter.formatPKR(transaction.remainingAmount)}",
                            fontSize = 11.sp,
                            color = RedPending,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action Buttons for Pending Udhaar Entry: "Mark as Paid" and "Partial Pay"
            if (!isPayment && isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPartialPayClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_partial_pay_${transaction.id}")
                    ) {
                        Text("Partial Pay", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onMarkAsPaid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPaid
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_mark_paid_${transaction.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark as Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
