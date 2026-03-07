package com.g.gradeapp.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createDataStore(path: String): DataStore<Preferences>
