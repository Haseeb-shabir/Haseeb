package com.example.ui.screens.add_udhaar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.CustomerWithBalance
import com.example.ui.model.UdhaarFormatter
import com.example.ui.theme.GreenPaid
import com.example.ui.theme.RedPending

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddUdhaarScreen(
    existingCustomers: List<CustomerWithBalance>,
    initialCustomerName: String = "",
    initialPhone: String = "",
    isLoading: Boolean,
    onSaveUdhaar: (
        name: String,
        phone: String,
        amount: Double,
        description: String,
        dateMillis: Long,
        isPaid: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var customerName by remember(initialCustomerName) { mutableStateOf(initialCustomerName) }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(false) } // false = Pending (default for Udhaar), true = Paid
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    // Suggestions from existing customers
    val matchingCustomers = remember(customerName, existingCustomers) {
        if (customerName.isBlank() || existingCustomers.any { it.customer.name.equals(customerName.trim(), ignoreCase = true) }) {
            emptyList()
        } else {
            existingCustomers.filter {
                it.customer.name.contains(customerName, ignoreCase = true) ||
                (it.customer.phone.isNotBlank() && it.customer.phone.contains(customerName))
            }.take(3)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Add Customer / Udhaar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Record new credit or payment entry. Automatically merges existing customer profiles.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Input Form Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Customer Name Field
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = {
                            customerName = it
                            nameError = null
                        },
                        label = { Text("Customer Name * (English / اردو)") },
                        placeholder = { Text("e.g. Muhammad Ali / علی احمد") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_customer_name_input")
                    )

                    // Autocomplete suggestion chips if matched
                    if (matchingCustomers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Existing Customer Suggestions:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            matchingCustomers.forEach { c ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        customerName = c.customer.name
                                        if (c.customer.phone.isNotBlank()) {
                                            phone = c.customer.phone
                                        }
                                        focusManager.clearFocus()
                                    },
                                    label = {
                                        Text(
                                            "${c.customer.name} (Pending: ${UdhaarFormatter.formatPKR(c.remainingPending)})",
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Number Field (Optional)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (Optional)") },
                        placeholder = { Text("e.g. 03001234567") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_customer_phone_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Amount Field
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.filter { char -> char.isDigit() || char == '.' }
                            amountError = null
                        },
                        label = { Text("Udhaar Amount (Rs.) *") },
                        placeholder = { Text("0") },
                        leadingIcon = {
                            Text(
                                text = "Rs.",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        singleLine = true,
                        isError = amountError != null,
                        supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_udhaar_amount_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Amount Increment Chips
                    Text(
                        text = "Quick Amount Add:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(500, 1000, 2000, 5000, 10000).forEach { quickAmt ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (current + quickAmt).toLong().toString()
                                },
                                label = { Text("+Rs. $quickAmt", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description / Item note
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Item / Description (Optional)") },
                        placeholder = { Text("e.g. Atta 10kg, Cooking Oil, Mobile Load") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_udhaar_description_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Date Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Transaction Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    UdhaarFormatter.formatDate(selectedDateMillis),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("Change")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Switcher: Pending vs Already Paid
                    Text(
                        text = "Payment Status:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isPaid,
                            onClick = { isPaid = false },
                            label = { Text("Pending (Udhaar / کھاتہ)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFDAD6),
                                selectedLabelColor = RedPending
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("status_chip_pending")
                        )

                        FilterChip(
                            selected = isPaid,
                            onClick = { isPaid = true },
                            label = { Text("Paid (Cash/Online)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFC7F3DC),
                                selectedLabelColor = GreenPaid
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("status_chip_paid")
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (customerName.trim().isBlank()) {
                                nameError = "Please enter customer name"
                                return@Button
                            }
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt <= 0.0) {
                                amountError = "Please enter a valid amount greater than 0"
                                return@Button
                            }

                            onSaveUdhaar(
                                customerName.trim(),
                                phone.trim(),
                                amt,
                                description.trim(),
                                selectedDateMillis,
                                isPaid
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_submit_udhaar")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPaid) "Save Paid Entry" else "Save Udhaar Entry",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
