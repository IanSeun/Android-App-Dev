// app/src/main/java/com/g/gradeapp/repository/ConfigRepository.kt
package com.g.gradeapp.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.g.gradeapp.data.model.GradeScale
import com.g.gradeapp.data.model.GradeThreshold
import com.g.gradeapp.data.model.WeightConfig
import com.g.gradeapp.data.model.defaultGradeScale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("g_config")

@Singleton
class ConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    // ── Keys ──────────────────────────────────────────────────────────────
    private object Keys {
        val HW_WEIGHT  = doublePreferencesKey("hw_weight")
        val QZ_WEIGHT  = doublePreferencesKey("qz_weight")
        val MID_WEIGHT = doublePreferencesKey("mid_weight")
        val FIN_WEIGHT = doublePreferencesKey("fin_weight")
        // Grade thresholds stored as "A:93,A−:90,B+:87,…"
        val GRADE_SCALE = stringPreferencesKey("grade_scale")
    }

    // ── Weight config ─────────────────────────────────────────────────────
    val weightConfig: Flow<WeightConfig> = store.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            WeightConfig(
                homeworkWeight  = prefs[Keys.HW_WEIGHT]  ?: 0.20,
                quizWeight      = prefs[Keys.QZ_WEIGHT]  ?: 0.10,
                midtermWeight   = prefs[Keys.MID_WEIGHT] ?: 0.30,
                finalExamWeight = prefs[Keys.FIN_WEIGHT] ?: 0.40,
            )
        }

    suspend fun saveWeightConfig(config: WeightConfig) {
        store.edit { prefs ->
            prefs[Keys.HW_WEIGHT]  = config.homeworkWeight
            prefs[Keys.QZ_WEIGHT]  = config.quizWeight
            prefs[Keys.MID_WEIGHT] = config.midtermWeight
            prefs[Keys.FIN_WEIGHT] = config.finalExamWeight
        }
    }

    // ── Grade scale ───────────────────────────────────────────────────────
    val gradeScale: Flow<GradeScale> = store.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val raw = prefs[Keys.GRADE_SCALE] ?: return@map GradeScale()
            val thresholds = raw.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    GradeThreshold(parts[0], parts[1].toDoubleOrNull() ?: return@mapNotNull null)
                } else null
            }
            if (thresholds.isEmpty()) GradeScale() else GradeScale(thresholds)
        }

    suspend fun saveGradeScale(scale: GradeScale) {
        val encoded = scale.thresholds.joinToString(",") { "${it.letter}:${it.minScore}" }
        store.edit { prefs -> prefs[Keys.GRADE_SCALE] = encoded }
    }

    suspend fun resetToDefaults() {
        store.edit { prefs ->
            prefs[Keys.HW_WEIGHT]  = 0.20
            prefs[Keys.QZ_WEIGHT]  = 0.10
            prefs[Keys.MID_WEIGHT] = 0.30
            prefs[Keys.FIN_WEIGHT] = 0.40
            prefs[Keys.GRADE_SCALE] = defaultGradeScale()
                .joinToString(",") { "${it.letter}:${it.minScore}" }
        }
    }
}
