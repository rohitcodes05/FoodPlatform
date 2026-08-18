package com.foodplatform.app.ui.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodplatform.app.data.remote.CartItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val errorEvent by viewModel.errorEvent.collectAsState()

    LaunchedEffect(errorEvent) {
        errorEvent?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Cart") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is CartUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CartUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadCart() }) {
                            Text("Retry")
                        }
                    }
                }
                is CartUiState.Success -> {
                    if (state.cart.items.isEmpty()) {
                        Text(
                            text = "Your cart is empty.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.cart.items, key = { it.id }) { item ->
                                    CartItemRow(
                                        item = item,
                                        onIncrement = { viewModel.updateQuantity(item.productId, item.quantity.toInt() + 1) },
                                        onDecrement = {
                                            if (item.quantity.toInt() > 1) {
                                                viewModel.updateQuantity(item.productId, item.quantity.toInt() - 1)
                                            } else {
                                                viewModel.removeItem(item.productId)
                                            }
                                        },
                                        onRemove = { viewModel.removeItem(item.productId) },
                                        enabled = !isActionLoading
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total:", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            "$${String.format("%.2f", viewModel.calculateTotal(state.cart))}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { /* disabled */ },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = false
                                    ) {
                                        Text("Checkout coming soon")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { viewModel.clearCart() },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isActionLoading
                                    ) {
                                        Text("Clear Cart", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isActionLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItemDto,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.product.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "$${item.product.price}", style = MaterialTheme.typography.bodyMedium)
                if (!item.product.isAvailable) {
                    Text(text = "Currently unavailable", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, enabled = enabled) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                }
                Text(
                    text = item.quantity.toInt().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onIncrement, enabled = enabled && item.product.isAvailable) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                }
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
