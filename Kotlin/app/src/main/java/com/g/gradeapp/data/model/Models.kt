// app/src/main/java/com/g/gradeapp/data/model/Models.kt
package com.g.gradeapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Student record (persisted per import session) ─────────────────────────
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: String,        // e.g. "20240042"
    val name: String,
    val homeworkScore: Double = 0.0,
    val quizScore: Double = 0.0,
    val midtermScore: Double = 0.0,
    val finalExamScore: Double = 0.0,
    // Computed fields (calculated by GradeEngine)
    val finalScore: Double = 0.0,
    val letterGrade: String = "",
    // Source metadata
    val sourceRow: Int = 0,        // original row in xlsx
    val hasWarning: Boolean = false, // blank cell warning
    val sessionId: Long = 0,       // groups students by import
)

// ── Import session metadata ───────────────────────────────────────────────
@Entity(tableName = "import_sessions")
data class ImportSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val importedAt: Long = System.currentTimeMillis(),
    val studentCount: Int = 0,
    val warningCount: Int = 0,
    val isActive: Boolean = true,
)

// ── Weight configuration ──────────────────────────────────────────────────
data class WeightConfig(
    val homeworkWeight:  Double = 0.20,
    val quizWeight:      Double = 0.10,
    val midtermWeight:   Double = 0.30,
    val finalExamWeight: Double = 0.40,
) {
    val total: Double get() = homeworkWeight + quizWeight + midtermWeight + finalExamWeight
    val isValid: Boolean get() = kotlin.math.abs(total - 1.0) < 0.001
}

// ── Grade threshold entry ─────────────────────────────────────────────────
data class GradeThreshold(
    val letter: String,
    val minScore: Double,
)

// ── Grade scale (ordered high → low) ─────────────────────────────────────
data class GradeScale(
    val thresholds: List<GradeThreshold> = defaultGradeScale(),
) {
    fun letterFor(score: Double): String =
        thresholds.firstOrNull { score >= it.minScore }?.letter ?: "F"
}

fun defaultGradeScale(): List<GradeThreshold> = listOf(
    GradeThreshold("A",  93.0),
    GradeThreshold("A−", 90.0),
    GradeThreshold("B+", 87.0),
    GradeThreshold("B",  83.0),
    GradeThreshold("B−", 80.0),
    GradeThreshold("C+", 77.0),
    GradeThreshold("C",  73.0),
    GradeThreshold("C−", 70.0),
    GradeThreshold("D+", 67.0),
    GradeThreshold("D",  60.0),
    GradeThreshold("F",  0.0),
)

// ── Import parse result ───────────────────────────────────────────────────
data class ParseResult(
    val students: List<Student>,
    val warnings: List<String>,   // human-readable issues
    val errorMessage: String? = null,  // fatal parse failure
) {
    val hasError: Boolean get() = errorMessage != null
}

// ── Export options ────────────────────────────────────────────────────────
data class ExportOptions(
    val includeWeightSheet:   Boolean = true,
    val highlightFailingRows: Boolean = true,
    val includeDistChart:     Boolean = false,
)
