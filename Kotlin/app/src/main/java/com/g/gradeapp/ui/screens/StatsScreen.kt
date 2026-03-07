// app/src/main/java/com/g/gradeapp/ui/screens/StatsScreen.kt
package com.g.gradeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.g.gradeapp.ui.components.*
import com.g.gradeapp.ui.theme.*
import com.g.gradeapp.viewmodel.GradesViewModel
import java.text.DecimalFormat

@Composable
fun StatsScreen(
    viewModel: GradesViewModel = hiltViewModel(),
) {
    val state    by viewModel.state.collectAsStateWithLifecycle()
    val students = state.students
    val df       = DecimalFormat("#.#")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (students.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No data yet",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                    )
                    Text(
                        "Import an Excel file from the Grades tab",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    )
                }
            }
            return@Column
        }

        val passRate = state.passRate
        val avgScore = state.avgScore
        val highest  = state.highestStudent
        val lowest   = students.minByOrNull { it.finalScore }

        // ── Pass rate ring ────────────────────────────────────────────────
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Surf2),
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress    = { passRate.toFloat() },
                        modifier    = Modifier.size(76.dp),
                        color       = Sapphire,
                        trackColor  = Navy600,
                        strokeWidth = 7.dp,
                    )
                    Text(
                        "${(passRate * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary, fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Column {
                    Text(
                        "Pass Rate",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = TextPrimary, fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        "${students.count { it.letterGrade != "F" }} of ${students.size} students passed",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress   = { passRate.toFloat() },
                        modifier   = Modifier.fillMaxWidth().height(5.dp),
                        color      = Sapphire,
                        trackColor = Navy600,
                    )
                }
            }
        }

        // ── Summary stats ─────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Average", df.format(avgScore),                       GradeB, Modifier.weight(1f))
            StatTile("Highest", df.format(highest?.finalScore ?: 0.0),     GradeA, Modifier.weight(1f))
            StatTile("Lowest",  df.format(lowest?.finalScore  ?: 0.0),     GradeF, Modifier.weight(1f))
        }

        // ── Distribution chart ────────────────────────────────────────────
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Surf2),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SectionHeader("Grade Distribution")
                Spacer(Modifier.height(12.dp))
                GradeDistChart(distribution = state.distribution, modifier = Modifier.fillMaxWidth())
            }
        }

        // ── Score range breakdown ─────────────────────────────────────────
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Surf2),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Score Range Breakdown")

                data class Range(val label: String, val min: Double, val max: Double, val color: androidx.compose.ui.graphics.Color)
                val ranges = listOf(
                    Range("90–100 (A range)", 90.0, 100.0, GradeA),
                    Range("80–89  (B range)", 80.0,  89.99, GradeB),
                    Range("70–79  (C range)", 70.0,  79.99, GradeC),
                    Range("60–69  (D range)", 60.0,  69.99, GradeD),
                    Range("Below 60  (F)",     0.0,  59.99, GradeF),
                )

                ranges.forEach { r ->
                    val cnt  = students.count { it.finalScore in r.min..r.max }
                    val frac = if (students.isNotEmpty()) cnt.toFloat() / students.size else 0f

                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            r.label,
                            style    = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            modifier = Modifier.width(130.dp),
                        )
                        LinearProgressIndicator(
                            progress   = { frac },
                            modifier   = Modifier.weight(1f).height(5.dp),
                            color      = r.color,
                            trackColor = Navy600,
                        )
                        Text(
                            cnt.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color      = r.color,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            ),
                            modifier = Modifier.width(24.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
