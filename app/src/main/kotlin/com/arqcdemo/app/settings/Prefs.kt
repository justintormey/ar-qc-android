package com.arqcdemo.app.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "argoqc")

/**
 * DataStore wrapper for persisted room PINs.
 *
 * QC + Builder use distinct keys so each remembers its own session PIN
 * independently. Both default to empty until first launch bakes in the
 * platform default (471471 for QC, 526526 for Builder).
 */
class Prefs(private val ctx: Context) {

    val roomPin: Flow<String> = ctx.dataStore.data.map { it[KEY_PIN] ?: "" }
    val builderRoomPin: Flow<String> = ctx.dataStore.data.map { it[KEY_BUILDER_PIN] ?: "" }

    suspend fun setRoomPin(value: String) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_PIN] = value.filter { it.isDigit() }.take(8)
        }
    }

    suspend fun setBuilderRoomPin(value: String) {
        ctx.dataStore.edit { prefs ->
            prefs[KEY_BUILDER_PIN] = value.filter { it.isDigit() }.take(8)
        }
    }

    companion object {
        private val KEY_PIN = stringPreferencesKey("room_pin")
        private val KEY_BUILDER_PIN = stringPreferencesKey("builder_room_pin")
    }
}
