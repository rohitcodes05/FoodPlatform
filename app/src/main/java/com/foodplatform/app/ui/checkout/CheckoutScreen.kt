package com.foodplatform.app.ui.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodplatform.app.data.remote.AddressDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    cartItemCount: Int,
    cartTotalAmount: String,
    onNavigateBack: () -> Unit,
    onOrderSuccess: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val errorEvent by viewModel.errorEvent.collectAsState()
    val orderSuccessEvent by viewModel.orderSuccessEvent.collectAsState()

    var showAddressDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<AddressDto?>(null) }
    var addressToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorEvent) {
        errorEvent?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(orderSuccessEvent) {
        if (orderSuccessEvent) {
            viewModel.clearSuccessEvent()
            onOrderSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is CheckoutUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CheckoutUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAddresses() }) {
                            Text("Retry")
                        }
                    }
                }
                is CheckoutUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Order Summary
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Items:")
                                    Text("$cartItemCount")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total:")
                                    Text("$$cartTotalAmount", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Address Selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Delivery Address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                editingAddress = null
                                showAddressDialog = true
                            }, enabled = !isActionLoading) {
                                Icon(Icons.Default.Add, contentDescription = "Add Address")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add New")
                            }
                        }

                        if (state.addresses.isEmpty()) {
                            Text(
                                "No saved addresses found. Please add a new address.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(state.addresses) { address ->
                                    AddressItem(
                                        address = address,
                                        isSelected = address.id == state.selectedAddressId,
                                        onSelect = { viewModel.selectAddress(address.id) },
                                        onEdit = {
                                            editingAddress = address
                                            showAddressDialog = true
                                        },
                                        onDelete = {
                                            addressToDelete = address.id
                                        },
                                        isActionLoading = isActionLoading
                                    )
                                }
                            }
                        }

                        // Place Order Button
                        Button(
                            onClick = { viewModel.placeOrder() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            enabled = !isActionLoading && state.selectedAddressId != null
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Place Order")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddressDialog) {
        AddressDialog(
            address = editingAddress,
            onDismiss = { showAddressDialog = false },
            onSave = { street, city, state, postalCode, country ->
                val currentEditing = editingAddress
                if (currentEditing != null) {
                    viewModel.updateAddress(currentEditing.id, street, city, state, postalCode, country)
                } else {
                    viewModel.addAddress(street, city, state, postalCode, country)
                }
                showAddressDialog = false
            }
        )
    }

    if (addressToDelete != null) {
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            title = { Text("Delete Address") },
            text = { Text("Are you sure you want to delete this address?") },
            confirmButton = {
                Button(
                    onClick = {
                        addressToDelete?.let { viewModel.deleteAddress(it) }
                        addressToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { addressToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddressItem(
    address: AddressDto,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isActionLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(address.street, fontWeight = FontWeight.Bold)
                Text("${address.city}, ${address.state} ${address.postalCode}")
                Text(address.country)
            }
            IconButton(onClick = onEdit, enabled = !isActionLoading) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Address")
            }
            IconButton(onClick = onDelete, enabled = !isActionLoading) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Address", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddressDialog(
    address: AddressDto?,
    onDismiss: () -> Unit,
    onSave: (street: String, city: String, state: String, postalCode: String, country: String) -> Unit
) {
    var street by remember { mutableStateOf(address?.street ?: "") }
    var city by remember { mutableStateOf(address?.city ?: "") }
    var state by remember { mutableStateOf(address?.state ?: "") }
    var postalCode by remember { mutableStateOf(address?.postalCode ?: "") }
    var country by remember { mutableStateOf(address?.country ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (address == null) "Add New Address" else "Edit Address") },
        text = {
            Column {
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Street Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text("Postal Code") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(street, city, state, postalCode, country) },
                enabled = street.isNotBlank() && city.isNotBlank() && state.isNotBlank() && postalCode.isNotBlank() && country.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
