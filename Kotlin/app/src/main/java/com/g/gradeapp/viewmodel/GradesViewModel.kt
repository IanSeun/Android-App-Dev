// app/src/main/java/com/g/gradeapp/viewmodel/GradesViewModel.kt
package com.g.gradeapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g.gradeapp.data.model.*
import com.g.gradeapp.repository.ConfigRepository
import com.g.gradeapp.repository.GradeRepository
import com.g.gradeapp.util.ExcelExporter
import com.g.gradeapp.util.ExcelParser
import com.g.gradeapp.util.GradeEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────
data class GradesUiState(
    val session: ImportSession? = null,
    val students: List<Student> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val importState: ImportState = ImportState.Idle,
    val exportState: ExportState = ExportState.Idle,
    val snackMessage: String? = null,
    // Stats (derived)
    val avgScore: Double = 0.0,
    val passRate: Double = 0.0,
    val highestStudent: Student? = null,
    val distribution: Map<String, Int> = emptyMap(),
)

sealed class ImportState {
    object Idle : ImportState()
    object Picking : ImportState()
    data class Parsing(val progress: Float = 0f) : ImportState()
    data class Preview(val result: ParseResult, val fileName: String) : ImportState()
    data class Error(val message: String) : ImportState()
    object Done : ImportState()
}

sealed class ExportState {
    object Idle : ExportState()
    object Confirming : ExportState()
    data class Exporting(val progress: Float = 0f) : ExportState()
    data class Done(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class GradesViewModel @Inject constructor(
    private val gradeRepo: GradeRepository,
    private val configRepo: ConfigRepository,
    private val parser: ExcelParser,
    private val exporter: ExcelExporter,
) : ViewModel() {

    private val _state = MutableStateFlow(GradesUiState())
    val state: StateFlow<GradesUiState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var _lastFileName = ""

    init {
        observeActiveSession()
    }

    private fun observeActiveSession() {
        gradeRepo.getActiveSession()
            .onEach { session ->
                _state.update { it.copy(session = session) }
                if (session != null) observeStudents(session.id)
            }
            .launchIn(viewModelScope)
    }

    private fun observeStudents(sessionId: Long) {
        combine(
            _searchQuery.debounce(250),
            gradeRepo.getStudentsBySession(sessionId),
        ) { query, all ->
            if (query.isBlank()) all
            else all.filter { s ->
                s.name.contains(query, ignoreCase = true) ||
                s.studentId.contains(query, ignoreCase = true)
            }
        }
            .onEach { filtered ->
                _state.update { ui ->
                    ui.copy(
                        students         = filtered,
                        avgScore         = GradeEngine.average(filtered),
                        passRate         = GradeEngine.passRate(filtered),
                        highestStudent   = GradeEngine.highest(filtered),
                        distribution     = GradeEngine.distribution(filtered),
                        isLoading        = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    // ── Search ────────────────────────────────────────────────────────────
    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        _state.update { it.copy(searchQuery = q) }
    }

    // ── Import flow ───────────────────────────────────────────────────────
    fun requestImport() {
        _state.update { it.copy(importState = ImportState.Picking) }
    }

    fun onFilePicked(uri: Uri, fileName: String) = viewModelScope.launch {
        _lastFileName = fileName
        _state.update { it.copy(importState = ImportState.Parsing(0.2f)) }

        // Create a placeholder session ID for parsing
        val tempSessionId = System.currentTimeMillis()
        val result = parser.parse(uri, tempSessionId)

        _state.update {
            if (result.hasError) {
                it.copy(importState = ImportState.Error(result.errorMessage ?: "Unknown error"))
            } else {
                it.copy(importState = ImportState.Preview(result, fileName))
            }
        }
    }

    fun confirmImport() = viewModelScope.launch {
        val preview = _state.value.importState as? ImportState.Preview ?: return@launch
        val result  = preview.result

        _state.update { it.copy(importState = ImportState.Parsing(0.7f)) }

        // Persist session + students
        val sessionId = gradeRepo.createSession(
            fileName     = preview.fileName,
            studentCount = result.students.size,
            warningCount = result.warnings.size,
        )

        // Recalculate grades with current config before saving
        val weights = configRepo.weightConfig.firstOrNull() ?: WeightConfig()
        val scale   = configRepo.gradeScale.firstOrNull()   ?: GradeScale()
        val withGrades = GradeEngine.calculate(
            result.students.map { it.copy(sessionId = sessionId) },
            weights, scale,
        )

        gradeRepo.saveStudents(withGrades)
        _state.update { it.copy(importState = ImportState.Done) }
        showSnack("✓ ${result.students.size} students imported")
    }

    fun cancelImport() {
        _state.update { it.copy(importState = ImportState.Idle) }
    }

    fun dismissImportError() {
        _state.update { it.copy(importState = ImportState.Idle) }
    }

    // ── Export flow ───────────────────────────────────────────────────────
    fun requestExport() {
        if (_state.value.session == null) {
            showSnack("Import data first")
            return
        }
        _state.update { it.copy(exportState = ExportState.Confirming) }
    }

    fun confirmExport(options: ExportOptions) = viewModelScope.launch {
        _state.update { it.copy(exportState = ExportState.Exporting(0.3f)) }
        try {
            val weights = configRepo.weightConfig.firstOrNull() ?: WeightConfig()
            val scale   = configRepo.gradeScale.firstOrNull()   ?: GradeScale()
            val file = exporter.export(
                students       = _state.value.students,
                weights        = weights,
                scale          = scale,
                options        = options,
                sourceFileName = _state.value.session?.fileName ?: "grades",
            )
            _state.update { it.copy(exportState = ExportState.Done(file)) }
        } catch (e: Exception) {
            _state.update { it.copy(exportState = ExportState.Error(e.message ?: "Export failed")) }
        }
    }

    fun cancelExport() {
        _state.update { it.copy(exportState = ExportState.Idle) }
    }

    fun clearExportState() {
        _state.update { it.copy(exportState = ExportState.Idle) }
    }

    // ── Recalculate (called after config change) ──────────────────────────
    fun recalculate() = viewModelScope.launch {
        val sessionId = _state.value.session?.id ?: return@launch
        val weights   = configRepo.weightConfig.firstOrNull() ?: WeightConfig()
        val scale     = configRepo.gradeScale.firstOrNull()   ?: GradeScale()
        gradeRepo.recalculate(sessionId, weights, scale)
        showSnack("Grades recalculated")
    }

    private fun showSnack(msg: String) {
        _state.update { it.copy(snackMessage = msg) }
    }

    fun clearSnack() {
        _state.update { it.copy(snackMessage = null) }
    }
}
