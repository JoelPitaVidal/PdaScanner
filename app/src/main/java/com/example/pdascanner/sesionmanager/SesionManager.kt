package com.example.pdascanner.sesionmanager

import android.content.Context
import android.content.SharedPreferences

object SesionManager {
    private const val PREF_NAME = "sesion_pda"
    private const val KEY_USER_ID = "USER_ID"
    private const val KEY_USER_NAME = "USER_NAME"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Cambiamos 'id: String' a 'id: Int' para consistencia con la API
    fun saveUserSession(context: Context, id: Int, nombre: String) {
        if (id <= 0) return

        getPrefs(context).edit()
            .putInt(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, nombre)
            .apply()
    }

    // Retorna Int? (nulo si no hay sesión)
    fun getUserId(context: Context): Int {
        return getPrefs(context).getInt(KEY_USER_ID, -1)
    }

    fun getUserName(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_NAME, null)
    }

    fun cerrarSesion(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}