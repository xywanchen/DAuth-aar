package com.cyberflow.dauthsdk.login.utils

import android.content.Context
import android.content.SharedPreferences

private const val LOGIN_STATE_INFO = "LOGIN_STATE_INFO"
private const val ACCESS_TOKEN = "access_token"
private const val AUTH_ID = "auth_id"
private const val USER_ID = "user_id"
private const val DID_TOKEN = "did_token"
private const val REFRESH_TOKEN = "refresh_token"

internal class LoginPrefs(private val context: Context) {

    private val defaultAsync = true
    private val sp get() = context.getSharedPreferences(LOGIN_STATE_INFO, Context.MODE_PRIVATE)!!

    private fun getPrefs(): SharedPreferences {
        return context.getSharedPreferences(LOGIN_STATE_INFO, Context.MODE_PRIVATE)
    }

    fun getAccessToken(): String {
        return getPrefs().getString(ACCESS_TOKEN, null).orEmpty()
    }

    fun setAccessToken(accessToken: String) {
        getPrefs().edit().putString(ACCESS_TOKEN, accessToken).apply()
    }

    fun getAuthId(): String {
        return getPrefs().getString(AUTH_ID, null).orEmpty()
    }

    fun setAuthID(authId: String) {
        getPrefs().edit().putString(AUTH_ID, authId).apply()
    }

    fun setDidToken(didToken: String) {
        getPrefs().edit().putString(DID_TOKEN, didToken).apply()
    }

    fun getDidToken(): String {
        return getPrefs().getString(DID_TOKEN, null).orEmpty()
    }

    fun getRefreshToken(): String {
        return getPrefs().getString(REFRESH_TOKEN, null).orEmpty()
    }

    fun setRefreshToken(refreshToken: String) {
        getPrefs().edit().putString(REFRESH_TOKEN, refreshToken).apply()
    }

    fun putLoginInfo(
        accessToken: String? = null,
        authId: String? = null,
        userId: String? = null,
        refreshToken: String? = null,
        async: Boolean = false
    ) {
        val values = ArrayList<Pair<String, Any>>()
        userId?.let {
            values.add(USER_ID to it)
        }
        accessToken?.let {
            values.add(ACCESS_TOKEN to it)
        }
        authId?.let {
            values.add(AUTH_ID to it)
        }
        refreshToken?.let {
            values.add(REFRESH_TOKEN to it)
        }

        put(values, async)
    }

    fun put(kvs: Collection<Pair<String, Any>>, async: Boolean = defaultAsync) {
        modify(async) { editor ->
            kvs.forEach { pair ->
                putX(editor, pair.first, pair.second)
            }
        }
    }
    private fun putX(editor: SharedPreferences.Editor, key: String, value: Any) {
        when (value) {
            is Int -> {
                editor.putInt(key, value)
            }
            is String -> {
                editor.putString(key, value)
            }
            is Long -> {
                editor.putLong(key, value)
            }
            is Boolean -> {
                editor.putBoolean(key, value)
            }
            is Float -> {
                editor.putFloat(key, value)
            }
        }
    }

    protected fun modify(async: Boolean = true, block: (editor: SharedPreferences.Editor) -> Unit) {
        sp.edit().apply {
            block.invoke(this)
            if (async) {
                apply()
            } else {
                commit()
            }
        }
    }


    fun clearLoginStateInfo() {
        val prefsEditor = getPrefs().edit()
        prefsEditor.clear()
        prefsEditor.apply()
    }

}