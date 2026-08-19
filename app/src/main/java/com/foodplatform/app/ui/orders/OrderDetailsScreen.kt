package com.foodplatform.app.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodplatform.app.data.remote.DeliveryDto
import com.foodplatform.app.data.remote.PaymentDto
import com.foodplatform.app.data.remote.OrderAddressSnapshotDto
import com.foodplatform.app.data.remote.OrderItemDto
import com.foodplatform.app.data.remote.ReviewDto
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: String,
    viewModel: OrdersViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.detailsUiState.collectAsState()
    val reviewSubmitState by viewModel.reviewSubmitState.collectAsState()
    
    var showReviewDialog by remember { mutableStateOf(false) }
    var selectedOrderItemId by remember { mutableStateOf<String?>(null) }
    var existingReview by remember { mutableStateOf<ReviewDto?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetails(orderId)
    }

    LaunchedEffect(reviewSubmitState) {
        reviewSubmitState?.let { result ->
            val isSuccess = result.isSuccess
            val message = if (isSuccess) {
                "Review submitted successfully"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to submit review"
            }
            
            if (isSuccess) {
                showReviewDialog = false
            }
            
            viewModel.clearReviewState()
            
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
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
                is OrderDetailsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is OrderDetailsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadOrderDetails(orderId) }) {
                            Text("Retry")
                        }
                    }
                }
                is OrderDetailsUiState.Success -> {
                    val order = state.order
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("ID: ${order.id}")
                                    Text("Date: ${formatDate(order.createdAt)}")
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Status:", style = MaterialTheme.typography.bodyMedium)
                                        OrderStatusChip(status = order.status)
                                    }
                                    order.payment?.let { payment ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Payment:", style = MaterialTheme.typography.bodyMedium)
                                            PaymentStatusChip(status = payment.status)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Total: $${order.totalAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (order.address != null) {
                            item {
                                AddressDetailsCard(address = order.address)
                            }
                        }

                        if (order.delivery != null) {
                            item {
                                DeliveryDetailsCard(delivery = order.delivery)
                            }
                        }

                        if (order.items != null && order.items.isNotEmpty()) {
                            item {
                                Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(order.items) { item ->
                                OrderItemRow(
                                    item = item, 
                                    orderStatus = order.status,
                                    onWriteReview = { orderItemId ->
                                        selectedOrderItemId = orderItemId
                                        existingReview = null
                                        showReviewDialog = true
                                    },
                                    onEditReview = { orderItemId, review ->
                                        selectedOrderItemId = orderItemId
                                        existingReview = review
                                        showReviewDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog && selectedOrderItemId != null) {
        ReviewDialog(
            initialReview = existingReview,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                if (existingReview != null) {
                    viewModel.updateReview(existingReview!!.id, rating, comment, orderId)
                } else {
                    viewModel.submitReview(selectedOrderItemId!!, rating, comment, orderId)
                }
            },
            onDelete = if (existingReview != null) {
                { showDeleteConfirmDialog = true }
            } else null
        )
    }

    if (showDeleteConfirmDialog && existingReview != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete this review?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReview(existingReview!!.id, orderId)
                        showDeleteConfirmDialog = false
                        showReviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddressDetailsCard(address: OrderAddressSnapshotDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delivery Address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(address.street)
            Text("${address.city}, ${address.state} ${address.postalCode}")
            Text(address.country)
        }
    }
}

@Composable
fun DeliveryDetailsCard(delivery: com.foodplatform.app.data.remote.DeliveryDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delivery Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            DeliveryStatusChip(status = delivery.status)
            if (!delivery.trackingCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Tracking: ${delivery.trackingCode}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun OrderItemRow(
    item: OrderItemDto,
    orderStatus: String,
    onWriteReview: (String) -> Unit,
    onEditReview: (String, ReviewDto) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.product.name, fontWeight = FontWeight.Bold)
                    if (item.selectedCut != null) {
                        Text("Cut: ${item.selectedCut}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.selectedWeight != null) {
                        Text("Weight: ${item.selectedWeight}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Qty: ${item.quantity}")
                }
                Text("$${item.purchasePrice}", fontWeight = FontWeight.Bold)
            }
            if (orderStatus == "DELIVERED") {
                Spacer(modifier = Modifier.height(8.dp))
                if (item.review != null) {
                    Button(
                        onClick = { onEditReview(item.id, item.review) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Edit Review")
                    }
                } else {
                    Button(
                        onClick = { onWriteReview(item.id) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Write Review")
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewDialog(
    initialReview: ReviewDto? = null,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var rating by remember { mutableStateOf(initialReview?.rating ?: 5) }
    var comment by remember { mutableStateOf(initialReview?.comment ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (initialReview != null) "Edit Review" else "Write a Review")
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Review",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in 1..5) {
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star $i",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }) {
                Text(if (initialReview != null) "Update" else "Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
