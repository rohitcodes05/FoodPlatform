package com.foodplatform.app.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodplatform.app.data.remote.ProductDto
import com.foodplatform.app.data.remote.ProductType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    cartItemCount: Int,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FoodPlatform Catalog") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = onNavigateToCart) {
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) {
                                    Badge { Text(cartItemCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        }
                    }
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar is ALWAYS in the composition so it never loses focus when
            // the state transitions between Loading / Success / Error.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search products...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true
            )

            // State-dependent content fills the remaining vertical space.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is CatalogUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    is CatalogUiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.retry() }) {
                                Text("Retry")
                            }
                        }
                    }

                    is CatalogUiState.Success -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Category filter chips
                            if (state.categories.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = state.selectedCategoryId == null,
                                            onClick = { viewModel.selectCategory(null) },
                                            label = { Text("All") }
                                        )
                                    }
                                    items(state.categories) { category ->
                                        FilterChip(
                                            selected = state.selectedCategoryId == category.id,
                                            onClick = { viewModel.selectCategory(category.id) },
                                            label = { Text(category.name) }
                                        )
                                    }
                                }
                            }

                            if (state.products.isEmpty()) {
                                // Empty state
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = if (searchQuery.isNotBlank())
                                                "No products found matching \"$searchQuery\""
                                            else
                                                "No products available",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (searchQuery.isNotBlank() ||
                                            state.selectedCategoryId != null
                                        ) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(onClick = {
                                                viewModel.updateSearchQuery("")
                                                viewModel.selectCategory(null)
                                            }) {
                                                Text("Clear Filters")
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Product list
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = 16.dp,
                                        top = 8.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(state.products) { product ->
                                        ProductItem(
                                            product = product,
                                            onClick = { onNavigateToProduct(product.id) }
                                        )
                                    }

                                    if (state.isNextPageLoading) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Pagination trigger: load next page when the last item is visible.
                        val isAtBottom by remember {
                            derivedStateOf {
                                val lastVisibleItem =
                                    listState.layoutInfo.visibleItemsInfo.lastOrNull()
                                lastVisibleItem?.index != 0 &&
                                    lastVisibleItem?.index ==
                                        listState.layoutInfo.totalItemsCount - 1
                            }
                        }

                        LaunchedEffect(isAtBottom) {
                            if (isAtBottom &&
                                !state.isNextPageLoading &&
                                !state.isEndReached
                            ) {
                                viewModel.loadNextPage()
                            }
                        }
                    }
                } // end when
            } // end Box
        } // end Column
    } // end Scaffold
}

@Composable
fun ProductItem(product: ProductDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .alpha(if (product.isAvailable) 1f else 0.5f),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (product.type == ProductType.COOKED_FOOD)
                    Icons.Default.Restaurant
                else
                    Icons.Default.SetMeal,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${product.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (!product.isAvailable) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("Out of Stock", modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}
