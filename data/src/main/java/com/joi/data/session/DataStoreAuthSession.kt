package com.joi.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.joi.domain.model.Role
import com.joi.domain.session.AuthSession
import com.joi.domain.session.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private object Keys {
    val TOKEN = stringPreferencesKey("token")
    val USER_ID = stringPreferencesKey("user_id")
    val FULL_NAME = stringPreferencesKey("full_name")
    val ROLE = stringPreferencesKey("role")
    val MUST_CHANGE_PASSWORD = booleanPreferencesKey("must_change_password")
}

/** Persists the session to disk via Jetpack DataStore, so a signed-in person stays signed in across app restarts. */
class DataStoreAuthSession(private val dataStore: DataStore<Preferences>) : AuthSession {

    override val state: Flow<SessionState> = dataStore.data.map { prefs ->
        val token = prefs[Keys.TOKEN]
        if (token == null) {
            SessionState.SignedOut
        } else {
            SessionState(
                token = token,
                userId = prefs[Keys.USER_ID],
                fullName = prefs[Keys.FULL_NAME],
                role = prefs[Keys.ROLE]?.let { if (it == "MODERATOR") Role.MODERATOR else Role.MEMBER },
                mustChangePassword = prefs[Keys.MUST_CHANGE_PASSWORD] ?: false,
            )
        }
    }

    override suspend fun current(): SessionState = state.first()

    override suspend fun save(
        token: String,
        userId: String,
        fullName: String,
        role: Role,
        mustChangePassword: Boolean,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.FULL_NAME] = fullName
            prefs[Keys.ROLE] = role.name
            prefs[Keys.MUST_CHANGE_PASSWORD] = mustChangePassword
        }
    }

    override suspend fun updateMustChangePassword(value: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.MUST_CHANGE_PASSWORD] = value }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
