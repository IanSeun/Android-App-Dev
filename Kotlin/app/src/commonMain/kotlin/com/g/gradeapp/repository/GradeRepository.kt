package com.g.gradeapp.repository

import com.g.gradeapp.data.model.*
import kotlinx.coroutines.flow.Flow

interface GradeRepository {
    fun getAllSessions(): Flow<List<ImportSession>>
    fun getActiveSession(): Flow<ImportSession?>
    fun getStudentsBySession(sessionId: Long): Flow<List<Student>>
    
    suspend fun createSession(fileName: String, studentCount: Int, warningCount: Int): Long
    suspend fun saveStudents(students: List<Student>)
}
