package com.g.gradeapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g.gradeapp.data.model.GradeScale
import com.g.gradeapp.data.model.GradeThreshold
import com.g.gradeapp.data.model.WeightConfig
import com.g.gradeapp.data.model.defaultGradeScale
import com.g.gradeapp.repository.ConfigRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConfigUiState(
    val weights: WeightConfig = WeightConfig(),
    val gradeScale: List<GradeThreshold> = defaultGradeScale(),
    val isSaved: Boolean = false,
    val errorMsg: String? = null,
)

class ConfigViewModel(
    private val configRepo: ConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConfigUiState())
    val state: StateFlow<ConfigUiState> = _state.asStateFlow()

    init {
        configRepo.weightConfig
            .onEach { w -> _state.update { it.copy(weights = w) } }
            .launchIn(viewModelScope)
        configRepo.gradeScale
            .onEach { s -> _state.update { it.copy(gradeScale = s.thresholds) } }
            .launchIn(viewModelScope)
    }

    // ── Weight updates ────────────────────────────────────────────────────
    fun setHomework(v: Float) = _state.update {
        it.copy(weights = it.weights.copy(homeworkWeight = v.toDouble()))
    }
    fun setQuiz(v: Float) = _state.update {
        it.copy(weights = it.weights.copy(quizWeight = v.toDouble()))
    }
    fun setMidterm(v: Float) = _state.update {
        it.copy(weights = it.weights.copy(midtermWeight = v.toDouble()))
    }
    fun setFinalExam(v: Float) = _state.update {
        it.copy(weights = it.weights.copy(finalExamWeight = v.toDouble()))
    }

    // ── Grade scale updates ───────────────────────────────────────────────
    fun updateThreshold(index: Int, newMin: Double) {
        val updated = _state.value.gradeScale.toMutableList()
        if (index < updated.size) {
            updated[index] = updated[index].copy(minScore = newMin)
            _state.update { it.copy(gradeScale = updated) }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────
    fun save() = viewModelScope.launch {
        val state = _state.value
        if (!state.weights.isValid) {
            _state.update { it.copy(errorMsg = "Weights must total 100%") }
            return@launch
        }
        configRepo.saveWeightConfig(state.weights)
        configRepo.saveGradeScale(GradeScale(state.gradeScale))
        _state.update { it.copy(isSaved = true, errorMsg = null) }
    }

    fun resetToDefaults() = viewModelScope.launch {
        configRepo.resetToDefaults()
        _state.update { it.copy(isSaved = false, errorMsg = null) }
    }

    fun clearError() = _state.update { it.copy(errorMsg = null) }
    fun clearSaved() = _state.update { it.copy(isSaved = false) }
}
