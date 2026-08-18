package com.foodplatform.app.data.repository

import com.foodplatform.app.data.remote.AddCartItemRequest
import com.foodplatform.app.data.remote.CartApi
import com.foodplatform.app.data.remote.CartDto
import com.foodplatform.app.data.remote.UpdateCartItemRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class CartRepository(
    private val cartApi: CartApi
) {
    private val _cartUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cartUpdates: SharedFlow<Unit> = _cartUpdates.asSharedFlow()

    fun notifyCartUpdated() {
        _cartUpdates.tryEmit(Unit)
    }

    open suspend fun getCart(): Resource<CartDto> {
        return try {
            val response = cartApi.getCart()
            Resource.Success(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load cart")
        }
    }

    open suspend fun addItem(productId: String, quantity: Int): Resource<Unit> {
        return try {
            cartApi.addItem(AddCartItemRequest(productId, quantity))
            notifyCartUpdated()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add item to cart")
        }
    }

    open suspend fun updateItemQuantity(productId: String, quantity: Int): Resource<Unit> {
        return try {
            cartApi.updateItemQuantity(productId, UpdateCartItemRequest(quantity))
            notifyCartUpdated()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update quantity")
        }
    }

    open suspend fun removeItem(productId: String): Resource<Unit> {
        return try {
            cartApi.removeItem(productId)
            notifyCartUpdated()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove item")
        }
    }

    open suspend fun clearCart(): Resource<Unit> {
        return try {
            cartApi.clearCart()
            notifyCartUpdated()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to clear cart")
        }
    }
}
