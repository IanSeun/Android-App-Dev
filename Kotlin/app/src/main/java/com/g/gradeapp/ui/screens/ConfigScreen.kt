// app/src/main/java/com/g/gradeapp/ui/screens/ConfigScreen.kt
package com.g.gradeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.g.gradeapp.ui.components.gradeColor
import com.g.gradeapp.ui.components.gradeContainerColor
import com.g.gradeapp.ui.components.SectionHeader
import com.g.gradeapp.ui.theme.*
import com.g.gradeapp.viewmodel.ConfigViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = hiltViewModel(),
) {
    val state    by viewModel.state.collectAsStateWithLifecycle()
    val snackHost = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackHost.showSnackbar("Configuration saved ✓", duration = SnackbarDuration.Short)
            viewModel.clearSaved()
        }
    }

    LaunchedEffect(state.errorMsg) {
        state.errorMsg?.let {
            snackHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackHost) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Surf3,
                    contentColor   = TextPrimary,
                    actionColor    = Sapphire,
                    shape          = RoundedCornerShape(10.dp),
                )
            }
        },
        containerColor = Black,
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(Black)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Assessment Weights ────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Surf2),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionHeader("Assessment Weights")

                    val w = state.weights
                    WeightSlider("Homework",   w.homeworkWeight.toFloat(),  viewModel::setHomework)
                    WeightSlider("Quizzes",    w.quizWeight.toFloat(),      viewModel::setQuiz)
                    WeightSlider("Midterm",    w.midtermWeight.toFloat(),   viewModel::setMidterm)
                    WeightSlider("Final Exam", w.finalExamWeight.toFloat(), viewModel::setFinalExam)

                    val totalPct   = (w.total * 100).roundToInt()
                    val totalColor = if (w.isValid) GradeA else Error

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surf3, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextSecondary, fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Text(
                            "$totalPct%",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = totalColor, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            ),
                        )
                    }
                }
            }

            // ── Grade Scale ───────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Surf2),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectionHeader("Grading Scale")
                    Text(
                        "Set minimum score for each letter grade.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    )
                    Spacer(Modifier.height(4.dp))

                    state.gradeScale.forEachIndexed { idx, threshold ->
                        GradeThresholdRow(
                            letter   = threshold.letter,
                            minScore = threshold.minScore,
                            onChange = { viewModel.updateThreshold(idx, it) },
                        )
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = viewModel::resetToDefaults,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, NavyOutline),
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick  = viewModel::save,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = OnSapphire),
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Config", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WeightSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = TextPrimary),
            )
            Text(
                "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge.copy(color = Sapphire, fontWeight = FontWeight.SemiBold),
            )
        }
        Slider(
            value         = value,
            onValueChange = onChange,
            valueRange    = 0f..1f,
            steps         = 19,
            colors        = SliderDefaults.colors(
                thumbColor         = Sapphire,
                activeTrackColor   = Sapphire,
                inactiveTrackColor = Navy600,
            ),
        )
    }
}

@Composable
private fun GradeThresholdRow(
    letter: String,
    minScore: Double,
    onChange: (Double) -> Unit,
) {
    var text by remember(minScore) { mutableStateOf(minScore.toInt().toString()) }

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(gradeContainerColor(letter), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = gradeColor(letter), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                ),
            )
        }

        Text(
            "Min score:",
            style    = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            modifier = Modifier.width(66.dp),
        )

        OutlinedTextField(
            value         = text,
            onValueChange = { v ->
                text = v
                v.toDoubleOrNull()?.let { onChange(it) }
            },
            modifier        = Modifier.width(72.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine      = true,
            shape           = RoundedCornerShape(8.dp),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Sapphire,
                unfocusedBorderColor    = NavyOutline,
                focusedContainerColor   = Surf3,
                unfocusedContainerColor = Surf3,
                cursorColor             = Sapphire,
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
        )

        Text(
            "– ${minScore.toInt() + 6}+",
            style = MaterialTheme.typography.bodySmall.copy(color = TextDisabled),
        )
    }
}
