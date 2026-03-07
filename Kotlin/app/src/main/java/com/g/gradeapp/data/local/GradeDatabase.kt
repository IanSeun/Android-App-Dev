// app/src/main/java/com/g/gradeapp/data/local/GradeDatabase.kt
package com.g.gradeapp.data.local

import android.content.Context
import androidx.room.*
import com.g.gradeapp.data.model.ImportSession
import com.g.gradeapp.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM students WHERE sessionId = :sessionId ORDER BY name ASC")
    fun getStudentsBySession(sessionId: Long): Flow<List<Student>>

    @Query("""
        SELECT * FROM students 
        WHERE sessionId = :sessionId 
          AND (name LIKE '%' || :query || '%' OR studentId LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchStudents(sessionId: Long, query: String): Flow<List<Student>>

    @Query("SELECT COUNT(*) FROM students WHERE sessionId = :sessionId")
    fun countBySession(sessionId: Long): Flow<Int>

    @Query("SELECT AVG(finalScore) FROM students WHERE sessionId = :sessionId")
    fun avgFinalScore(sessionId: Long): Flow<Double?>

    @Query("SELECT * FROM students WHERE sessionId = :sessionId AND letterGrade IN ('F', 'D', 'D+') ORDER BY finalScore ASC")
    fun getAtRiskStudents(sessionId: Long): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<Student>)

    @Update
    suspend fun update(student: Student)

    @Query("DELETE FROM students WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("UPDATE students SET finalScore = :score, letterGrade = :grade WHERE id = :id")
    suspend fun updateGrade(id: Long, score: Double, grade: String)
}

@Dao
interface SessionDao {

    @Query("SELECT * FROM import_sessions ORDER BY importedAt DESC")
    fun getAllSessions(): Flow<List<ImportSession>>

    @Query("SELECT * FROM import_sessions WHERE isActive = 1 ORDER BY importedAt DESC LIMIT 1")
    fun getActiveSession(): Flow<ImportSession?>

    @Insert
    suspend fun insert(session: ImportSession): Long

    @Query("UPDATE import_sessions SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE import_sessions SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query("DELETE FROM import_sessions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities     = [Student::class, ImportSession::class],
    version      = 1,
    exportSchema = false,
)
abstract class GradeDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "grade_db"
    }
}
