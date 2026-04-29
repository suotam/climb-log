package com.example.climblog.ui.screen.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.remote.LezecComment
import com.example.climblog.data.remote.LezecCredentialsStore
import com.example.climblog.data.remote.LezecService
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

sealed class LezecCommentsState {
    object Idle : LezecCommentsState()
    object Loading : LezecCommentsState()
    data class Loaded(val comments: List<LezecComment>) : LezecCommentsState()
    data class Error(val message: String) : LezecCommentsState()
}

sealed class LezecPostState {
    object Idle : LezecPostState()
    object Sending : LezecPostState()
    object Success : LezecPostState()
    data class Error(val message: String) : LezecPostState()
}

data class RouteDetailUiState(
    val route: Route? = null,
    val ascents: List<Ascent> = emptyList(),
    val routePhotos: List<Photo> = emptyList(),
    val comments: List<RouteComment> = emptyList(),
    val wishlistEntry: WishlistEntry? = null,
    val isSent: Boolean = false,
    val bestStyle: AscentStyle? = null,
    val isLoading: Boolean = true,
    val lezecCommentsState: LezecCommentsState = LezecCommentsState.Idle,
    val lezecPostState: LezecPostState = LezecPostState.Idle,
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    private val ascentRepository: AscentRepository,
    private val photoRepository: PhotoRepository,
    private val wishlistRepository: WishlistRepository,
    private val commentRepository: RouteCommentRepository,
    private val lezecService: LezecService,
    private val credentialsStore: LezecCredentialsStore,
) : ViewModel() {

    private val routeId: Long = checkNotNull(savedStateHandle["routeId"])

    private val _route = MutableStateFlow<Route?>(null)
    private val _lezecCommentsState = MutableStateFlow<LezecCommentsState>(LezecCommentsState.Idle)
    private val _lezecPostState = MutableStateFlow<LezecPostState>(LezecPostState.Idle)

    val uiState: StateFlow<RouteDetailUiState> = combine(
        combine(
            _route,
            ascentRepository.getAscentsByRoute(routeId),
            photoRepository.getPhotosByRoute(routeId)
        ) { route, ascents, photos -> Triple(route, ascents, photos) },
        combine(
            wishlistRepository.getWishlistEntryForRoute(routeId),
            commentRepository.getCommentsByRoute(routeId)
        ) { wishlist, comments -> Pair(wishlist, comments) },
        combine(_lezecCommentsState, _lezecPostState) { lc, lp -> Pair(lc, lp) }
    ) { (route, ascents, photos), (wishlist, comments), (lezecComments, lezecPost) ->
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
            isLoading = false,
            lezecCommentsState = lezecComments,
            lezecPostState = lezecPost,
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

    // ── Lezec.cz comments ────────────────────────────────────────────────────

    fun loadLezecComments() {
        val lezecId = uiState.value.route?.lezecId ?: return
        viewModelScope.launch {
            _lezecCommentsState.value = LezecCommentsState.Loading
            try {
                val comments = lezecService.fetchRouteComments(lezecId)
                _lezecCommentsState.value = LezecCommentsState.Loaded(comments)
            } catch (e: Exception) {
                _lezecCommentsState.value = LezecCommentsState.Error("Nepodařilo se načíst komentáře")
            }
        }
    }

    fun postLezecComment(name: String, email: String, text: String) {
        val lezecId = uiState.value.route?.lezecId ?: return
        viewModelScope.launch {
            _lezecPostState.value = LezecPostState.Sending
            try {
                val ok = lezecService.postRouteComment(lezecId, name, email, text)
                if (ok) {
                    credentialsStore.saveCommentAuthor(name, email)
                    _lezecPostState.value = LezecPostState.Success
                    loadLezecComments()
                } else {
                    _lezecPostState.value = LezecPostState.Error("Komentář se nepodařilo odeslat")
                }
            } catch (e: Exception) {
                _lezecPostState.value = LezecPostState.Error("Chyba sítě")
            }
        }
    }

    fun resetLezecPostState() {
        _lezecPostState.value = LezecPostState.Idle
    }

    fun getSavedCommentName(): String = credentialsStore.getCommentName()
        .ifBlank { credentialsStore.getUid() }

    fun getSavedCommentEmail(): String = credentialsStore.getCommentEmail()

    fun deleteRoute(onDeleted: () -> Unit) {
        val route = uiState.value.route ?: return
        viewModelScope.launch {
            routeRepository.deleteRoute(route)
            onDeleted()
        }
    }
}
