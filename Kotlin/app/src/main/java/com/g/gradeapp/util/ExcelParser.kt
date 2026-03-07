// app/src/main/java/com/g/gradeapp/util/ExcelParser.kt
package com.g.gradeapp.util

import android.content.Context
import android.net.Uri
import com.g.gradeapp.data.model.ParseResult
import com.g.gradeapp.data.model.Student
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelParser @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Parses an xlsx/xls file from [uri] into a [ParseResult].
     *
     * Expected column layout (row 0 = headers):
     *   A: Student ID
     *   B: Name
     *   C: Homework Score (0–100)
     *   D: Quiz Score     (0–100)
     *   E: Midterm Score  (0–100)
     *   F: Final Exam     (0–100)
     *
     * Additional columns are preserved but ignored for grading.
     */
    suspend fun parse(uri: Uri, sessionId: Long): ParseResult = withContext(Dispatchers.IO) {
        val warnings = mutableListOf<String>()
        val students = mutableListOf<Student>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ParseResult(emptyList(), emptyList(), "Cannot open file")

            val workbook = WorkbookFactory.create(inputStream)
            val sheet    = workbook.getSheetAt(0)

            if (sheet.physicalNumberOfRows < 2) {
                return@withContext ParseResult(
                    emptyList(), emptyList(), "File must have at least one data row"
                )
            }

            // Validate headers (row 0)
            val headerRow = sheet.getRow(0)
            val expectedHeaders = listOf("student id", "name", "homework", "quiz", "midterm", "final")
            val actualHeaders   = (0 until minOf(headerRow.lastCellNum.toInt(), 6))
                .map { headerRow.getCell(it)?.stringCellValue?.lowercase()?.trim() ?: "" }

            expectedHeaders.forEachIndexed { idx, expected ->
                if (idx < actualHeaders.size && !actualHeaders[idx].contains(expected.split(" ")[0])) {
                    warnings.add("Column ${idx + 1} expected '$expected', found '${actualHeaders[idx]}'")
                }
            }

            // Parse data rows (1..lastRowNum)
            for (rowIdx in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIdx) ?: continue

                fun cellDouble(col: Int): Pair<Double, Boolean> {
                    val cell = row.getCell(col)
                    return when {
                        cell == null || cell.cellType == CellType.BLANK -> {
                            warnings.add("Row ${rowIdx + 1}, col ${col + 1}: blank — defaulting to 0")
                            Pair(0.0, true)
                        }
                        cell.cellType == CellType.NUMERIC -> Pair(cell.numericCellValue.coerceIn(0.0, 100.0), false)
                        cell.cellType == CellType.STRING  -> {
                            val v = cell.stringCellValue.trim().toDoubleOrNull()
                            if (v == null) {
                                warnings.add("Row ${rowIdx + 1}, col ${col + 1}: '${cell.stringCellValue}' is not a number — using 0")
                                Pair(0.0, true)
                            } else Pair(v.coerceIn(0.0, 100.0), false)
                        }
                        else -> Pair(0.0, true)
                    }
                }

                val studentId = row.getCell(0)?.stringCellValue?.trim() ?: "R${rowIdx + 1}"
                val name      = row.getCell(1)?.stringCellValue?.trim() ?: "Unknown"

                val (hw,  hwWarn)  = cellDouble(2)
                val (qz,  qzWarn)  = cellDouble(3)
                val (mid, midWarn) = cellDouble(4)
                val (fin, finWarn) = cellDouble(5)

                students.add(
                    Student(
                        studentId      = studentId,
                        name           = name,
                        homeworkScore  = hw,
                        quizScore      = qz,
                        midtermScore   = mid,
                        finalExamScore = fin,
                        sourceRow      = rowIdx + 1,
                        hasWarning     = hwWarn || qzWarn || midWarn || finWarn,
                        sessionId      = sessionId,
                    )
                )
            }

            workbook.close()
            inputStream.close()

            ParseResult(students, warnings)
        } catch (e: Exception) {
            ParseResult(emptyList(), warnings, "Parse error: ${e.message}")
        }
    }
}
