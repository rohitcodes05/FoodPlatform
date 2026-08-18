package com.foodplatform.app.ui.profile

import com.foodplatform.app.data.remote.UserResponse

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: UserResponse) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
