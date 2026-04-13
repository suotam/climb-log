package com.example.climblog.ui.screen.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.PhotoRepository
import com.example.climblog.data.repository.RouteCommentRepository
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.data.repository.WishlistRepository
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.Photo
import com.example.climblog.domain.model.Route
import com.example.climblog.domain.model.RouteComment
import com.example.climblog.domain.model.WishlistEntry
import com.example.climblog.domain.model.priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteDetailUiState(
    val route: Route? = null,
    val ascents: List<Ascent> = emptyList(),
    val routePhotos: List<Photo> = emptyList(),
    val comments: List<RouteComment> = emptyList(),
    val wishlistEntry: WishlistEntry? = null,
    val isSent: Boolean = false,
    val bestStyle: AscentStyle? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    private val ascentRepository: AscentRepository,
    private val photoRepository: PhotoRepository,
    private val wishlistRepository: WishlistRepository,
    private val commentRepository: RouteCommentRepository
) : ViewModel() {

    private val routeId: Long = checkNotNull(savedStateHandle["routeId"])

    private val _route = MutableStateFlow<Route?>(null)

    val uiState: StateFlow<RouteDetailUiState> = combine(
        combine(
            _route,
            ascentRepository.getAscentsByRoute(routeId),
            photoRepository.getPhotosByRoute(routeId)
        ) { route, ascents, photos -> Triple(route, ascents, photos) },
        combine(
            wishlistRepository.getWishlistEntryForRoute(routeId),
            commentRepository.getCommentsByRoute(routeId)
        ) { wishlist, comments -> Pair(wishlist, comments) }
    ) { (route, ascents, photos), (wishlist, comments) ->
        val sentAscents = ascents.filter {
            it.style != AscentStyle.ATTEMPT && it.style != AscentStyle.PROJECT
        }
        RouteDetailUiState(
            route = route,
            ascents = ascents,
            routePhotos = photos,
            comments = comments,
            wishlistEntry = wishlist,
            isSent = sentAscents.isNotEmpty(),
            bestStyle = sentAscents.minByOrNull { it.style.priority }?.style,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RouteDetailUiState()
    )

    init {
        viewModelScope.launch {
            _route.value = routeRepository.getRouteById(routeId)
        }
    }

    fun deleteAscent(ascent: Ascent) {
        viewModelScope.launch { ascentRepository.deleteAscent(ascent) }
    }

    fun addRoutePhotos(uris: List<String>) {
        viewModelScope.launch {
            uris.forEach { uri -> photoRepository.savePhoto(Photo(routeId = routeId, uri = uri)) }
        }
    }

    fun deleteRoutePhoto(photo: Photo) {
        viewModelScope.launch { photoRepository.deletePhoto(photo) }
    }

    // ── Wishlist ──────────────────────────────────────────────────────────────

    fun addToWishlist(note: String, priority: Int) {
        viewModelScope.launch {
            wishlistRepository.addToWishlist(routeId, note, priority)
        }
    }

    fun updateWishlist(note: String, priority: Int) {
        val current = uiState.value.wishlistEntry ?: return
        viewModelScope.launch {
            wishlistRepository.updateWishlistEntry(current.copy(note = note, priority = priority))
        }
    }

    fun removeFromWishlist() {
        viewModelScope.launch { wishlistRepository.removeFromWishlist(routeId) }
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    fun addComment(authorName: String, text: String) {
        viewModelScope.launch {
            commentRepository.addComment(RouteComment(routeId = routeId, authorName = authorName, text = text))
        }
    }

    fun deleteComment(comment: RouteComment) {
        viewModelScope.launch { commentRepository.deleteComment(comment) }
    }

    fun deleteRoute(onDeleted: () -> Unit) {
        val route = uiState.value.route ?: return
        viewModelScope.launch {
            routeRepository.deleteRoute(route)
            onDeleted()
        }
    }
}
