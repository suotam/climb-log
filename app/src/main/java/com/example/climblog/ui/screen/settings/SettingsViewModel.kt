package com.example.climblog.ui.screen.settings

import androidx.lifecycle.ViewModel
import com.example.climblog.data.remote.LezecCredentialsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val lezecUid: String = "",
    val lezecPassword: String = "",
    val hasCredentials: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val credentialsStore: LezecCredentialsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            lezecUid = credentialsStore.getUid(),
            hasCredentials = credentialsStore.hasCredentials()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onUidChange(uid: String) = _uiState.update { it.copy(lezecUid = uid, saved = false) }
    fun onPasswordChange(pw: String) = _uiState.update { it.copy(lezecPassword = pw, saved = false) }

    fun save() {
        val state = _uiState.value
        if (state.lezecUid.isBlank() || state.lezecPassword.isBlank()) return
        credentialsStore.save(state.lezecUid, state.lezecPassword)
        _uiState.update { it.copy(hasCredentials = true, lezecPassword = "", saved = true) }
    }

    fun clearCredentials() {
        credentialsStore.clear()
        _uiState.update { it.copy(lezecUid = "", lezecPassword = "", hasCredentials = false, saved = false) }
    }
}
