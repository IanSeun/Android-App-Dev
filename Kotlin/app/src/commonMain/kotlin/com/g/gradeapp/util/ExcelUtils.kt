package com.g.gradeapp.util

import com.g.gradeapp.data.model.ParseResult
import com.g.gradeapp.data.model.Student
import com.g.gradeapp.data.model.WeightConfig
import com.g.gradeapp.data.model.GradeScale
import com.g.gradeapp.data.model.ExportOptions
import java.io.File

interface ExcelParser {
    suspend fun parse(path: String, sessionId: Long): ParseResult
}

interface ExcelExporter {
    suspend fun export(
        students: List<Student>,
        weights: WeightConfig,
        scale: GradeScale,
        options: ExportOptions,
        sourceFileName: String,
    ): File
}
