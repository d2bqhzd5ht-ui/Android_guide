package com.example.scenicguide.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** 登录态落盘：token + 用户基本信息，供自动登录使用 */
class UserPreferences(context: Context) {

    private val store = context.applicationContext.sessionDataStore

    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val NICKNAME = stringPreferencesKey("nickname")
        val ROLE = stringPreferencesKey("role")
    }

    val session: Flow<Session?> = store.data.map { prefs ->
        val token = prefs[Keys.TOKEN]?.takeIf { it.isNotBlank() } ?: return@map null
        Session(
            token = token,
            user = SessionUser(
                id = prefs[Keys.USER_ID].orEmpty(),
                username = prefs[Keys.USERNAME].orEmpty(),
                nickname = prefs[Keys.NICKNAME],
                role = prefs[Keys.ROLE],
            ),
        )
    }

    suspend fun save(session: Session) {
        store.edit { prefs ->
            prefs[Keys.TOKEN] = session.token
            prefs[Keys.USER_ID] = session.user.id
            prefs[Keys.USERNAME] = session.user.username
            session.user.nickname?.let { prefs[Keys.NICKNAME] = it } ?: prefs.remove(Keys.NICKNAME)
            session.user.role?.let { prefs[Keys.ROLE] = it } ?: prefs.remove(Keys.ROLE)
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}
