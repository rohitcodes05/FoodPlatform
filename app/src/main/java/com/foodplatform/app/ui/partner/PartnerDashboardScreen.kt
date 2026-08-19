package com.foodplatform.app.ui.partner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foodplatform.app.data.remote.OrderDto
import com.foodplatform.app.data.remote.PartnerProductDto

// ── Valid status transitions (UI guidance only — backend is authoritative) ─────
private val VALID_TRANSITIONS = mapOf(
    "PENDING" to listOf("CONFIRMED", "CANCELLED"),
    "CONFIRMED" to listOf("PREPARING"),
    "PREPARING" to listOf("PACKED", "READY"),
    "PACKED" to listOf("READY")
)
private val TERMINAL_STATUSES = setOf("READY", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED")

private val PRODUCT_TYPES = listOf("COOKED_FOOD", "RAW_MEAT")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerDashboardScreen(
    viewModel: PartnerDashboardViewModel,
    onNavigateBack: () -> Unit
) {
    val ordersUiState by viewModel.ordersUiState.collectAsState()
    val productsUiState by viewModel.productsUiState.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateProductDialog by remember { mutableStateOf(false) }
    var orderToUpdateStatus by remember { mutableStateOf<OrderDto?>(null) }
    var productToDelete by remember { mutableStateOf<PartnerProductDto?>(null) }
    var productToManageVariations by remember { mutableStateOf<PartnerProductDto?>(null) }

    // Show action errors in a dialog
    if (actionError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearActionError() },
            title = { Text("Error") },
            text = { Text(actionError!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearActionError() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partner Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { showCreateProductDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Orders") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("My Products") }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> OrdersTab(
                        state = ordersUiState,
                        isUpdating = isUpdating,
                        onRetry = { viewModel.loadOrders() },
                        onUpdateStatus = { order -> orderToUpdateStatus = order }
                    )
                    1 -> ProductsTab(
                        state = productsUiState,
                        isUpdating = isUpdating,
                        onRetry = { viewModel.loadProducts() },
                        onToggleAvailability = { product -> viewModel.toggleProductAvailability(product) },
                        onDeleteProduct = { product -> productToDelete = product },
                        onManageVariations = { product -> productToManageVariations = product }
                    )
                }

                if (isUpdating) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // ── Order Status Update Dialog ─────────────────────────────────────────────
    orderToUpdateStatus?.let { order ->
        val transitions = VALID_TRANSITIONS[order.status] ?: emptyList()
        var selectedStatus by remember { mutableStateOf(transitions.firstOrNull() ?: "") }

        AlertDialog(
            onDismissRequest = { orderToUpdateStatus = null },
            title = { Text("Update Order Status") },
            text = {
                Column {
                    Text("Order: ${order.id.take(8)}...", style = MaterialTheme.typography.bodySmall)
                    Text("Current: ${order.status}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (transitions.isEmpty()) {
                        Text("No status updates available for this order.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        transitions.forEach { status ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = status == selectedStatus, onClick = { selectedStatus = status })
                                Text(status)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedStatus.isNotEmpty()) {
                            viewModel.updateOrderStatus(order.id, selectedStatus)
                        }
                        orderToUpdateStatus = null
                    },
                    enabled = transitions.isNotEmpty() && !isUpdating
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { orderToUpdateStatus = null }) { Text("Cancel") }
            }
        )
    }

    // ── Delete Confirmation Dialog ─────────────────────────────────────────────
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product") },
            text = { Text("Delete \"${product.name}\"? This cannot be undone.\n\nProducts with existing orders cannot be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isUpdating
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // ── Create Product Dialog ──────────────────────────────────────────────────
    if (showCreateProductDialog) {
        CreateProductDialog(
            isUpdating = isUpdating,
            onDismiss = { showCreateProductDialog = false },
            onCreate = { name, type, price, description ->
                viewModel.createProduct(name, type, price, description)
                showCreateProductDialog = false
            }
        )
    }

    productToManageVariations?.let { product ->
        ManageVariationsDialog(
            product = product,
            isUpdating = isUpdating,
            onDismiss = { productToManageVariations = null },
            onCreateCut = { name -> viewModel.createCutOption(product.id, name) },
            onDeleteCut = { cutId -> viewModel.deleteCutOption(product.id, cutId) },
            onCreateWeight = { label, price -> viewModel.createWeightOption(product.id, label, price) },
            onDeleteWeight = { weightId -> viewModel.deleteWeightOption(product.id, weightId) }
        )
    }
}

// ── Orders Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun OrdersTab(
    state: PartnerOrdersUiState,
    isUpdating: Boolean,
    onRetry: () -> Unit,
    onUpdateStatus: (OrderDto) -> Unit
) {
    when (state) {
        is PartnerOrdersUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PartnerOrdersUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
        is PartnerOrdersUiState.Success -> {
            if (state.orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No orders yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.orders) { order ->
                        PartnerOrderCard(order = order, isUpdating = isUpdating, onUpdateStatus = onUpdateStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerOrderCard(
    order: OrderDto,
    isUpdating: Boolean,
    onUpdateStatus: (OrderDto) -> Unit
) {
    val isTerminal = order.status in TERMINAL_STATUSES
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Order: ${order.id.take(8)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text("Status: ", fontWeight = FontWeight.Bold)
                        Text(order.status, color = MaterialTheme.colorScheme.primary)
                    }
                    Row {
                        Text("Total: ", fontWeight = FontWeight.Bold)
                        Text("₹${order.totalAmount}")
                    }
                    val itemCount = order.items?.size ?: 0
                    Row {
                        Text("Items: ", fontWeight = FontWeight.Bold)
                        Text("$itemCount item${if (itemCount != 1) "s" else ""}")
                    }
                }
                if (!isTerminal) {
                    IconButton(onClick = { onUpdateStatus(order) }, enabled = !isUpdating) {
                        Icon(Icons.Default.Edit, contentDescription = "Update Status")
                    }
                }
            }
            order.items?.takeIf { it.isNotEmpty() }?.let { items ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                items.forEach { item ->
                    val variationStr = buildString {
                        if (item.selectedCut != null) append(" - Cut: ${item.selectedCut}")
                        if (item.selectedWeight != null) append(" - Weight: ${item.selectedWeight}")
                    }
                    Text(
                        "• ${item.product.name}$variationStr x ${item.quantity.toInt()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ── Products Tab ───────────────────────────────────────────────────────────────

@Composable
private fun ProductsTab(
    state: PartnerProductsUiState,
    isUpdating: Boolean,
    onRetry: () -> Unit,
    onToggleAvailability: (PartnerProductDto) -> Unit,
    onDeleteProduct: (PartnerProductDto) -> Unit,
    onManageVariations: (PartnerProductDto) -> Unit
) {
    when (state) {
        is PartnerProductsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is PartnerProductsUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
        is PartnerProductsUiState.Success -> {
            if (state.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No products yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to add your first product.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.products) { product ->
                        PartnerProductCard(
                            product = product,
                            isUpdating = isUpdating,
                            onToggleAvailability = onToggleAvailability,
                            onDeleteProduct = onDeleteProduct,
                            onManageVariations = onManageVariations
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerProductCard(
    product: PartnerProductDto,
    isUpdating: Boolean,
    onToggleAvailability: (PartnerProductDto) -> Unit,
    onDeleteProduct: (PartnerProductDto) -> Unit,
    onManageVariations: (PartnerProductDto) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(product.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${product.price}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }

                if (product.type == "RAW_MEAT") {
                    IconButton(onClick = { onManageVariations(product) }, enabled = !isUpdating) {
                        Icon(Icons.Default.Edit, contentDescription = "Manage Variations", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Availability toggle — sends only isAvailable, no partnerId
                Switch(
                    checked = product.isAvailable,
                    onCheckedChange = { onToggleAvailability(product) },
                    enabled = !isUpdating
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onDeleteProduct(product) }, enabled = !isUpdating) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                if (product.isAvailable) "Available" else "Unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Create Product Dialog ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProductDialog(
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, type: String, price: Double, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PRODUCT_TYPES.first()) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Add Product") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; localError = null },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it; localError = null },
                    label = { Text("Price *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        PRODUCT_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = { selectedType = type; typeDropdownExpanded = false }
                            )
                        }
                    }
                }
                localError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull()
                    when {
                        name.isBlank() -> localError = "Name is required"
                        price == null || price < 0 -> localError = "Enter a valid price"
                        else -> onCreate(name.trim(), selectedType, price, description.takeIf { it.isNotBlank() })
                    }
                },
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isUpdating) onDismiss() }, enabled = !isUpdating) { Text("Cancel") }
        }
    )
}


// -- Variations Dialog ---------------------------------------------------

@Composable
private fun ManageVariationsDialog(
    product: PartnerProductDto,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onCreateCut: (String) -> Unit,
    onDeleteCut: (String) -> Unit,
    onCreateWeight: (String, Double) -> Unit,
    onDeleteWeight: (String) -> Unit
) {
    var newCutName by remember { mutableStateOf("") }
    var newWeightLabel by remember { mutableStateOf("") }
    var newWeightPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Variations") },
        text = {
            LazyColumn {
                item {
                    Text("Cuts", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
                items(product.cutOptions) { cut ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(cut.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDeleteCut(cut.id) }, enabled = !isUpdating) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCutName,
                            onValueChange = { newCutName = it },
                            label = { Text("New Cut") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onCreateCut(newCutName); newCutName = "" },
                            enabled = !isUpdating && newCutName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Cut")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Weights", fontWeight = FontWeight.Bold)
                }
                items(product.weightOptions) { weight ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(weight.weightLabel + " - ${weight.priceOverride}", modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDeleteWeight(weight.id) }, enabled = !isUpdating) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
                item {
                    Column {
                        OutlinedTextField(
                            value = newWeightLabel,
                            onValueChange = { newWeightLabel = it },
                            label = { Text("New Weight Label") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newWeightPrice,
                                onValueChange = { newWeightPrice = it },
                                label = { Text("Price Override") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val price = newWeightPrice.toDoubleOrNull()
                                    if (price != null) {
                                        onCreateWeight(newWeightLabel, price)
                                        newWeightLabel = ""
                                        newWeightPrice = ""
                                    }
                                },
                                enabled = !isUpdating && newWeightLabel.isNotBlank() && newWeightPrice.toDoubleOrNull() != null
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Weight")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
