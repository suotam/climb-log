package com.example.climblog.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LezecCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("lezec_credentials", Context.MODE_PRIVATE)

    private val _hasCredentials = MutableStateFlow(hasCredentials())
    val hasCredentialsFlow: StateFlow<Boolean> = _hasCredentials.asStateFlow()

    fun save(uid: String, password: String) {
        prefs.edit().putString("uid", uid).putString("password", password).apply()
        _hasCredentials.value = true
    }

    fun get(): Pair<String, String>? {
        val uid = prefs.getString("uid", null) ?: return null
        val password = prefs.getString("password", null) ?: return null
        return uid to password
    }

    fun getUid(): String = prefs.getString("uid", "") ?: ""

    fun hasCredentials(): Boolean = get() != null

    fun clear() {
        prefs.edit().clear().apply()
        _hasCredentials.value = false
    }
}
