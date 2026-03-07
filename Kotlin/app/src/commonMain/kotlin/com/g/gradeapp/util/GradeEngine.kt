package com.g.gradeapp.util

import com.g.gradeapp.data.model.GradeScale
import com.g.gradeapp.data.model.Student
import com.g.gradeapp.data.model.WeightConfig
import kotlin.math.roundToInt

object GradeEngine {

    /**
     * Recalculates finalScore and letterGrade for every student
     * using the supplied [WeightConfig] and [GradeScale].
     */
    fun calculate(
        students: List<Student>,
        weights: WeightConfig,
        scale: GradeScale,
    ): List<Student> {
        return students.map { s ->
            val raw = (s.homeworkScore  * weights.homeworkWeight) +
                      (s.quizScore      * weights.quizWeight)     +
                      (s.midtermScore   * weights.midtermWeight)  +
                      (s.finalExamScore * weights.finalExamWeight)
            val rounded = (raw * 10.0).roundToInt() / 10.0
            s.copy(
                finalScore  = (raw * 10.0).roundToInt() / 10.0,
                letterGrade = scale.letterFor(rounded),
            )
        }
    }

    /** Distribution map: letter → count */
    fun distribution(students: List<Student>): Map<String, Int> =
        students.groupingBy { it.letterGrade }.eachCount()

    /** Pass rate: students with grade ≠ F / total */
    fun passRate(students: List<Student>): Double {
        if (students.isEmpty()) return 0.0
        return students.count { it.letterGrade != "F" }.toDouble() / students.size
    }

    fun average(students: List<Student>): Double =
        if (students.isEmpty()) 0.0
        else students.sumOf { it.finalScore } / students.size

    fun highest(students: List<Student>): Student? =
        students.maxByOrNull { it.finalScore }

    fun lowest(students: List<Student>): Student? =
        students.minByOrNull { it.finalScore }
}
