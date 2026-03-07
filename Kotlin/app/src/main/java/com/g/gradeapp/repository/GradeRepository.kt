// app/src/main/java/com/g/gradeapp/repository/GradeRepository.kt
package com.g.gradeapp.repository

import com.g.gradeapp.data.local.SessionDao
import com.g.gradeapp.data.local.StudentDao
import com.g.gradeapp.data.model.*
import com.g.gradeapp.util.GradeEngine
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val sessionDao: SessionDao,
) {
    // ── Sessions ──────────────────────────────────────────────────────────
    fun getAllSessions(): Flow<List<ImportSession>> = sessionDao.getAllSessions()
    fun getActiveSession(): Flow<ImportSession?> = sessionDao.getActiveSession()

    suspend fun createSession(fileName: String, studentCount: Int, warningCount: Int): Long {
        sessionDao.deactivateAll()
        return sessionDao.insert(
            ImportSession(
                fileName     = fileName,
                studentCount = studentCount,
                warningCount = warningCount,
                isActive     = true,
            )
        )
    }

    suspend fun activateSession(id: Long) {
        sessionDao.deactivateAll()
        sessionDao.activate(id)
    }

    // ── Students ──────────────────────────────────────────────────────────
    fun getStudentsBySession(sessionId: Long): Flow<List<Student>> =
        studentDao.getStudentsBySession(sessionId)

    fun searchStudents(sessionId: Long, query: String): Flow<List<Student>> =
        studentDao.searchStudents(sessionId, query)

    fun getActiveStudents(): Flow<List<Student>> =
        sessionDao.getActiveSession().flatMapLatest { session ->
            if (session != null) studentDao.getStudentsBySession(session.id)
            else flowOf(emptyList())
        }

    suspend fun saveStudents(students: List<Student>) {
        studentDao.insertAll(students)
    }

    suspend fun recalculate(
        sessionId: Long,
        weights: WeightConfig,
        scale: GradeScale,
    ) {
        val current = studentDao.getStudentsBySession(sessionId).firstOrNull() ?: return
        val recalc  = GradeEngine.calculate(current, weights, scale)
        recalc.forEach { s ->
            studentDao.updateGrade(s.id, s.finalScore, s.letterGrade)
        }
    }

    fun countBySession(id: Long): Flow<Int>    = studentDao.countBySession(id)
    fun avgBySession(id: Long): Flow<Double?>  = studentDao.avgFinalScore(id)
    fun atRisk(id: Long): Flow<List<Student>>  = studentDao.getAtRiskStudents(id)
}
