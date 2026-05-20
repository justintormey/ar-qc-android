package com.arqcdemo.app.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "argoqc")

/** Tiny DataStore wrapper for the persisted room PIN. */
class Prefs(private val ctx: Context) {

    val roomPin: Flow<String> = ctx.dataStore.data.map { it[KEY_PIN] ?: "" }

    suspend fun setRoomPin(value: String) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_PIN] = value.filter { it.isDigit() }.take(8)
        }
    }

    companion object {
        private val KEY_PIN = stringPreferencesKey("room_pin")
    }
}
