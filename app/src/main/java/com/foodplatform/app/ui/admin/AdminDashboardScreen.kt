package com.foodplatform.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodplatform.app.data.remote.OrderDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val updateError by viewModel.updateError.collectAsState()

    var showOrderStatusDialog by remember { mutableStateOf<OrderDto?>(null) }
    var showDeliveryStatusDialog by remember { mutableStateOf<OrderDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is AdminOrdersUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AdminOrdersUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadOrders() }) {
                            Text("Retry")
                        }
                    }
                }
                is AdminOrdersUiState.Success -> {
                    if (state.orders.isEmpty()) {
                        Text("No orders found.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.orders) { order ->
                                AdminOrderCard(
                                    order = order,
                                    onUpdateOrderStatus = { showOrderStatusDialog = order },
                                    onUpdateDeliveryStatus = { showDeliveryStatusDialog = order }
                                )
                            }
                        }
                    }
                }
            }

            if (isUpdating) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showOrderStatusDialog != null) {
        val order = showOrderStatusDialog!!
        var selectedStatus by remember { mutableStateOf(order.status) }
        val statuses = listOf("PENDING", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED")

        AlertDialog(
            onDismissRequest = { showOrderStatusDialog = null },
            title = { Text("Update Order Status") },
            text = {
                Column {
                    Text("Order: ${order.id}")
                    Spacer(modifier = Modifier.height(16.dp))
                    statuses.forEach { status ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (status == selectedStatus),
                                onClick = { selectedStatus = status }
                            )
                            Text(status)
                        }
                    }
                    updateError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateOrderStatus(order.id, selectedStatus)
                        showOrderStatusDialog = null
                    },
                    enabled = !isUpdating
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOrderStatusDialog = null },
                    enabled = !isUpdating
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeliveryStatusDialog != null) {
        val order = showDeliveryStatusDialog!!
        val delivery = order.delivery
        if (delivery != null) {
            var selectedStatus by remember { mutableStateOf(delivery.status) }
            var trackingCode by remember { mutableStateOf(delivery.trackingCode ?: "") }
            val statuses = listOf("PENDING", "PICKED_UP", "IN_TRANSIT", "DELIVERED", "FAILED")

            AlertDialog(
                onDismissRequest = { showDeliveryStatusDialog = null },
                title = { Text("Update Delivery") },
                text = {
                    Column {
                        Text("Delivery ID: ${delivery.id}")
                        Spacer(modifier = Modifier.height(16.dp))
                        statuses.forEach { status ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (status == selectedStatus),
                                    onClick = { selectedStatus = status }
                                )
                                Text(status)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = trackingCode,
                            onValueChange = { trackingCode = it },
                            label = { Text("Tracking Code") }
                        )
                        updateError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateDeliveryStatus(delivery.id, selectedStatus, trackingCode.takeIf { it.isNotBlank() })
                            showDeliveryStatusDialog = null
                        },
                        enabled = !isUpdating
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeliveryStatusDialog = null },
                        enabled = !isUpdating
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AdminOrderCard(
    order: OrderDto,
    onUpdateOrderStatus: () -> Unit,
    onUpdateDeliveryStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Order ID: ${order.id}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Order Status: ", fontWeight = FontWeight.Bold)
                Text(order.status)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onUpdateOrderStatus) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Order Status")
                }
            }
            
            if (order.delivery != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row {
                            Text("Delivery Status: ", fontWeight = FontWeight.Bold)
                            Text(order.delivery.status)
                        }
                        Row {
                            Text("Tracking: ", fontWeight = FontWeight.Bold)
                            Text(order.delivery.trackingCode ?: "None")
                        }
                    }
                    IconButton(onClick = onUpdateDeliveryStatus) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Delivery")
                    }
                }
            }
        }
    }
}
