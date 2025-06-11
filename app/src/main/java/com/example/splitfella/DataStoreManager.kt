package com.example.splitfella

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore by preferencesDataStore(name = "splitfella_data")

object DataStoreManager {
    private val USERS_KEY = stringPreferencesKey("users")
    private val EVENTS_KEY = stringPreferencesKey("events")

    suspend fun saveUsers(context: Context, users: List<User>) {
        val json = Json.encodeToString(users)
        context.dataStore.edit { it[USERS_KEY] = json }
    }

    suspend fun loadUsers(context: Context): List<User> {
        val json = context.dataStore.data.first()[USERS_KEY] ?: return emptyList()
        return Json.decodeFromString(json)
    }

    suspend fun saveEvents(context: Context, events: List<Event>) {
        val json = Json.encodeToString(events)
        context.dataStore.edit { it[EVENTS_KEY] = json }
    }

    suspend fun loadEvents(context: Context): List<Event> {
        val json = context.dataStore.data.first()[EVENTS_KEY] ?: return emptyList()
        return Json.decodeFromString(json)
    }
}
