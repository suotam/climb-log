package com.example.climblog.ui.screen.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.WishlistRepository
import com.example.climblog.domain.model.WishlistEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WishlistUiState(
    val entries: List<WishlistEntry> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    val uiState: StateFlow<WishlistUiState> =
        wishlistRepository.getWishlist()
            .map { WishlistUiState(entries = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = WishlistUiState()
            )

    fun remove(entry: WishlistEntry) {
        viewModelScope.launch {
            wishlistRepository.removeFromWishlist(entry.routeId)
        }
    }
}
