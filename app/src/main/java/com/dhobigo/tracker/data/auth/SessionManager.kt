package com.dhobigo.tracker.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.dhobigo.tracker.data.local.dao.UserDao
import com.dhobigo.tracker.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class SessionManager(
    private val context: Context,
    private val userDao: UserDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow<String?>(prefs.getString(KEY_ACTIVE_USER_ID, null))
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    val currentUserFlow: Flow<UserEntity?> = _currentUserId.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(null)
        } else {
            userDao.observeUserById(userId)
        }
    }

    val isLoggedIn: Boolean
        get() = !_currentUserId.value.isNullOrBlank()

    fun getActiveUserId(): String? = _currentUserId.value

    fun getRequiredUserId(): String {
        return _currentUserId.value ?: throw IllegalStateException("No active user session found.")
    }

    fun login(userId: String) {
        prefs.edit().putString(KEY_ACTIVE_USER_ID, userId).apply()
        _currentUserId.value = userId
    }

    fun logout() {
        prefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
        _currentUserId.value = null
    }

    companion object {
        private const val PREFS_NAME = "dhobi_auth_session"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
    }
}
