package com.g.gradeapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("g_config")

// Note: This is a simplified version. In a real KMP app, we'd pass the Context 
// to the actual implementation or use a provider.
lateinit var appContext: Context

actual fun createDataStore(path: String): DataStore<Preferences> {
    return appContext.dataStore
}
