// app/src/main/java/com/g/gradeapp/util/ExcelExporter.kt
package com.g.gradeapp.util

import android.content.Context
import android.net.Uri
import com.g.gradeapp.data.model.ExportOptions
import com.g.gradeapp.data.model.GradeScale
import com.g.gradeapp.data.model.Student
import com.g.gradeapp.data.model.WeightConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcelExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Writes grades to a new xlsx file in the app's cache and returns the [File].
     * Caller should then share it via FileProvider / SAF write URI.
     */
    suspend fun export(
        students: List<Student>,
        weights: WeightConfig,
        scale: GradeScale,
        options: ExportOptions,
        sourceFileName: String,
    ): File = withContext(Dispatchers.IO) {
        val wb    = XSSFWorkbook()
        val sheet = wb.createSheet("Grades")

        // ── Style helpers ─────────────────────────────────────────────────
        fun headerStyle(): CellStyle = wb.createCellStyle().apply {
            val font = wb.createFont().apply {
                bold      = true
                fontHeightInPoints = 11
                color     = IndexedColors.WHITE.index
            }
            setFont(font)
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
        }

        fun newColHeaderStyle(): CellStyle = wb.createCellStyle().apply {
            val font = wb.createFont().apply {
                bold      = true
                fontHeightInPoints = 11
                color     = IndexedColors.SEA_GREEN.index
            }
            setFont(font)
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
        }

        fun failStyle(): CellStyle = wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.ROSE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        // ── Header row ────────────────────────────────────────────────────
        val headers = listOf("Student ID", "Name", "Homework", "Quiz", "Midterm", "Final Exam", "Final Score", "Grade")
        val newCols  = setOf(6, 7)
        val hRow     = sheet.createRow(0)
        headers.forEachIndexed { i, title ->
            hRow.createCell(i).apply {
                setCellValue(title)
                setCellStyle(if (i in newCols) newColHeaderStyle() else headerStyle())
            }
        }

        // Auto-filter
        sheet.setAutoFilter(CellRangeAddress(0, 0, 0, headers.size - 1))

        // ── Data rows ─────────────────────────────────────────────────────
        val fStyle = failStyle()
        students.forEachIndexed { idx, s ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(s.studentId)
            row.createCell(1).setCellValue(s.name)
            row.createCell(2).setCellValue(s.homeworkScore)
            row.createCell(3).setCellValue(s.quizScore)
            row.createCell(4).setCellValue(s.midtermScore)
            row.createCell(5).setCellValue(s.finalExamScore)
            row.createCell(6).setCellValue(s.finalScore)
            row.createCell(7).setCellValue(s.letterGrade)

            if (options.highlightFailingRows && s.letterGrade == "F") {
                (0 until 8).forEach { row.getCell(it)?.setCellStyle(fStyle) }
            }
        }

        // Auto-size columns
        (0 until headers.size).forEach { sheet.autoSizeColumn(it) }

        // ── Weight breakdown sheet ─────────────────────────────────────────
        if (options.includeWeightSheet) {
            val ws = wb.createSheet("Configuration")
            var rIdx = 0

            fun addConfigRow(label: String, value: String) {
                ws.createRow(rIdx++).apply {
                    createCell(0).setCellValue(label)
                    createCell(1).setCellValue(value)
                }
            }

            addConfigRow("Export Date",     SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))
            addConfigRow("Source File",     sourceFileName)
            addConfigRow("Students",        students.size.toString())
            rIdx++ // blank
            addConfigRow("Homework Weight", "${(weights.homeworkWeight  * 100).toInt()}%")
            addConfigRow("Quiz Weight",     "${(weights.quizWeight      * 100).toInt()}%")
            addConfigRow("Midterm Weight",  "${(weights.midtermWeight   * 100).toInt()}%")
            addConfigRow("Final Weight",    "${(weights.finalExamWeight * 100).toInt()}%")
            rIdx++
            scale.thresholds.forEach { t ->
                addConfigRow("Grade ${t.letter}", "≥ ${t.minScore}")
            }
            ws.autoSizeColumn(0)
            ws.autoSizeColumn(1)
        }

        // ── Write to cache file ───────────────────────────────────────────
        val outFile = File(context.cacheDir, "grades_export_${System.currentTimeMillis()}.xlsx")
        outFile.outputStream().use { wb.write(it) }
        wb.close()
        outFile
    }
}
