package com.foodplatform.app.ui.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodplatform.app.data.remote.ProductType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: ProductDetailViewModel,
    cartItemCount: Int,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdding by viewModel.isAddingToCart.collectAsState()
    val addToCartEvent by viewModel.addToCartEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedCutId by remember { mutableStateOf<String?>(null) }
    var selectedWeightId by remember { mutableStateOf<String?>(null) }
    var expandedCut by remember { mutableStateOf(false) }
    var expandedWeight by remember { mutableStateOf(false) }

    LaunchedEffect(addToCartEvent) {
        addToCartEvent?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearAddToCartEvent()
        }
    }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) {
                                    Badge { Text(cartItemCount.toString()) }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ProductDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ProductDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProduct(productId) }) {
                            Text("Retry")
                        }
                    }
                }
                is ProductDetailUiState.Success -> {
                    val product = state.product
                    val currentWeight = product.weightOptions.find { it.id == selectedWeightId }
                    val currentCut = product.cutOptions.find { it.id == selectedCutId }
                    val displayPrice = currentWeight?.priceOverride ?: product.price

                    val needsCut = product.type == ProductType.RAW_MEAT && product.cutOptions.isNotEmpty()
                    val needsWeight = product.type == ProductType.RAW_MEAT && product.weightOptions.isNotEmpty()
                    val canAdd = product.isAvailable && !isAdding && (!needsCut || selectedCutId != null) && (!needsWeight || selectedWeightId != null)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
                    ) {
                        item {
                            Icon(
                                imageVector = if (product.type == ProductType.COOKED_FOOD) Icons.Default.Restaurant else Icons.Default.SetMeal,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "{displayPrice}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (!product.isAvailable) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("Currently Out of Stock", modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (!product.description.isNullOrBlank()) {
                                Text(
                                    text = product.description,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            if (needsCut) {
                                ExposedDropdownMenuBox(
                                    expanded = expandedCut,
                                    onExpandedChange = { expandedCut = !expandedCut },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = currentCut?.name ?: "Select Cut",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCut) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedCut,
                                        onDismissRequest = { expandedCut = false }
                                    ) {
                                        product.cutOptions.forEach { cut ->
                                            DropdownMenuItem(
                                                text = { Text(cut.name) },
                                                onClick = {
                                                    selectedCutId = cut.id
                                                    expandedCut = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (needsWeight) {
                                ExposedDropdownMenuBox(
                                    expanded = expandedWeight,
                                    onExpandedChange = { expandedWeight = !expandedWeight },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = currentWeight?.weightLabel ?: "Select Weight",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWeight) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedWeight,
                                        onDismissRequest = { expandedWeight = false }
                                    ) {
                                        product.weightOptions.forEach { weight ->
                                            DropdownMenuItem(
                                                text = { Text(weight.weightLabel) },
                                                onClick = {
                                                    selectedWeightId = weight.id
                                                    expandedWeight = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.addToCart(product.id, 1, selectedCutId, selectedWeightId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = canAdd
                            ) {
                                if (isAdding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Add to Cart")
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Reviews",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Average rating summary
                            if (state.reviews.isNotEmpty()) {
                                val avg = state.reviews.map { it.rating }.average()
                                val count = state.reviews.size
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", avg),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "( )",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (state.reviews.isEmpty()) {
                                Text(
                                    text = "No reviews yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                )
                            }
                        }

                        items(state.reviews) { review ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "Rating: /5", fontWeight = FontWeight.Bold)
                                        Text(text = review.createdAt.take(10), style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (!review.comment.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = review.comment)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
