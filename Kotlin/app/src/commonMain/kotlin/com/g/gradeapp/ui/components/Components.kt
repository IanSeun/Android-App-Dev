package com.g.gradeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g.gradeapp.ui.theme.*

// ── Grade color helpers ────────────────────────────────────────────────────
fun gradeColor(letter: String): Color = when {
    letter.startsWith("A") -> GradeA
    letter.startsWith("B") -> GradeB
    letter.startsWith("C") -> GradeC
    letter.startsWith("D") -> GradeD
    else                   -> GradeF
}

fun gradeContainerColor(letter: String): Color = when {
    letter.startsWith("A") -> GradeAContainer
    letter.startsWith("B") -> GradeBContainer
    letter.startsWith("C") -> GradeCContainer
    letter.startsWith("D") -> GradeDContainer
    else                   -> GradeFContainer
}

// ── LetterGradeBadge ───────────────────────────────────────────────────────
@Composable
fun LetterGradeBadge(letter: String, modifier: Modifier = Modifier) {
    val color     = gradeColor(letter)
    val container = gradeContainerColor(letter)
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text     = letter,
            style    = MaterialTheme.typography.labelMedium.copy(
                color      = color,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize   = 11.sp,
            ),
            maxLines = 1,
        )
    }
}

// ── ScoreBar ───────────────────────────────────────────────────────────────
@Composable
fun ScoreBar(score: Double, color: Color = Sapphire, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text  = score.toInt().toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize   = 11.sp,
                color      = TextPrimary,
            ),
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Navy600),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (score / 100.0).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

// ── StatTile ───────────────────────────────────────────────────────────────
@Composable
fun StatTile(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Surf3),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(accentColor))
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Text(
                text  = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color      = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 20.sp,
                ),
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
            )
        }
    }
}

// ── ImportStatusBanner ────────────────────────────────────────────────────
@Composable
fun ImportStatusBanner(
    fileName: String,
    studentCount: Int,
    warningCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Surf2),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Success.copy(alpha = 0.12f))
                    .border(1.dp, Success.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("📊", fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = fileName,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text  = "$studentCount students${if (warningCount > 0) " · $warningCount warnings" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                )
            }
            Surface(shape = RoundedCornerShape(20.dp), color = Success.copy(alpha = 0.12f)) {
                Text(
                    text     = "✓ Imported",
                    style    = MaterialTheme.typography.labelSmall.copy(color = Success, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── EmptyImportBanner ─────────────────────────────────────────────────────
@Composable
fun EmptyImportBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Surf2),
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, Sapphire.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Sapphire.copy(alpha = 0.12f))
                    .border(1.dp, Sapphire.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("⬆", fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Import Excel File",
                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text  = "Tap to upload .xlsx or .xls",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                )
            }
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint               = TextSecondary,
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}

// ── SectionHeader ─────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, count: Int? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = TextSecondary, letterSpacing = 0.1.sp, fontWeight = FontWeight.SemiBold,
            ),
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Surface(shape = CircleShape, color = NavyOutline) {
                Text(
                    text     = count.toString(),
                    style    = MaterialTheme.typography.labelSmall.copy(color = TextPrimary),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
    }
}

// ── GradeDistChart ────────────────────────────────────────────────────────
@Composable
fun GradeDistChart(distribution: Map<String, Int>, modifier: Modifier = Modifier) {
    val grades   = listOf("A", "A−", "B+", "B", "B−", "C+", "C", "C−", "D+", "D", "F")
    val maxCount = distribution.values.maxOrNull()?.takeIf { it > 0 } ?: 1

    Column(modifier = modifier) {
        Row(
            modifier              = Modifier.fillMaxWidth().height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.Bottom,
        ) {
            grades.forEach { grade ->
                val count   = distribution[grade] ?: 0
                val fraction = count.toFloat() / maxCount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction = fraction.coerceAtLeast(0.04f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(gradeColor(grade)),
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            grades.forEach { grade ->
                Text(
                    text      = grade,
                    modifier  = Modifier.weight(1f),
                    style     = MaterialTheme.typography.labelSmall.copy(
                        color = TextDisabled, fontSize = 7.sp,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                )
            }
        }
    }
}
