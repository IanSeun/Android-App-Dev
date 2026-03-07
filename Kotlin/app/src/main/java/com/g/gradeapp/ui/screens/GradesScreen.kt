// app/src/main/java/com/g/gradeapp/ui/screens/GradesScreen.kt
package com.g.gradeapp.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.g.gradeapp.data.model.ExportOptions
import com.g.gradeapp.ui.components.*
import com.g.gradeapp.ui.theme.*
import com.g.gradeapp.viewmodel.ExportState
import com.g.gradeapp.viewmodel.GradesViewModel
import com.g.gradeapp.viewmodel.ImportState
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    viewModel: GradesViewModel = hiltViewModel(),
) {
    val state     by viewModel.state.collectAsStateWithLifecycle()
    val snackHost  = remember { SnackbarHostState() }
    val context    = LocalContext.current

    // File picker launcher
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val fileName = context.contentResolver
            .query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(idx)
            } ?: "grades.xlsx"
        viewModel.onFilePicked(uri, fileName)
    }

    // Snackbar
    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            snackHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSnack()
        }
    }

    // Handle export Done → share via Intent
    val exportDone = state.exportState as? ExportState.Done
    LaunchedEffect(exportDone) {
        exportDone?.let { done ->
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", done.file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type    = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Grades"))
            viewModel.clearExportState()
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = viewModel::requestExport,
                containerColor = Sapphire,
                contentColor   = OnSapphire,
                icon           = { Icon(Icons.Default.FileDownload, null) },
                text           = { Text("Export", fontWeight = FontWeight.Bold) },
                shape          = RoundedCornerShape(14.dp),
            )
        },
        containerColor = Black,
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(Black),
        ) {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start  = 14.dp, end = 14.dp,
                    top    = 8.dp,  bottom = 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── Search bar ────────────────────────────────────────────
                item {
                    OutlinedTextField(
                        value         = state.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = { Text("Search students…", color = TextSecondary) },
                        leadingIcon   = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                        trailingIcon  = if (state.searchQuery.isNotEmpty()) {
                            { IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary)
                            }}
                        } else null,
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = Sapphire,
                            unfocusedBorderColor    = NavyOutline,
                            focusedContainerColor   = Surf2,
                            unfocusedContainerColor = Surf2,
                            cursorColor             = Sapphire,
                            focusedTextColor        = TextPrimary,
                            unfocusedTextColor      = TextPrimary,
                        ),
                        singleLine = true,
                    )
                }

                // ── Import banner ─────────────────────────────────────────
                item {
                    val session = state.session
                    if (session != null) {
                        ImportStatusBanner(
                            fileName     = session.fileName,
                            studentCount = session.studentCount,
                            warningCount = session.warningCount,
                            onClick      = viewModel::requestImport,
                        )
                    } else {
                        EmptyImportBanner(onClick = viewModel::requestImport)
                    }
                }

                // ── Stats strip ───────────────────────────────────────────
                if (state.students.isNotEmpty()) {
                    item {
                        val df = DecimalFormat("#.#")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(
                                label       = "Students",
                                value       = state.students.size.toString(),
                                accentColor = Sky,
                                modifier    = Modifier.weight(1f),
                            )
                            StatTile(
                                label       = "Avg Score",
                                value       = df.format(state.avgScore),
                                accentColor = GradeA,
                                modifier    = Modifier.weight(1f),
                            )
                            StatTile(
                                label       = "Pass Rate",
                                value       = "${(state.passRate * 100).toInt()}%",
                                accentColor = GradeC,
                                modifier    = Modifier.weight(1f),
                            )
                        }
                    }

                    // ── Distribution chart ────────────────────────────────
                    item {
                        Card(
                            shape  = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Surf2),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                SectionHeader("Grade Distribution")
                                Spacer(Modifier.height(12.dp))
                                GradeDistChart(
                                    distribution = state.distribution,
                                    modifier     = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    // ── Table header ──────────────────────────────────────
                    item {
                        SectionHeader("Student Grades", count = state.students.size)
                    }

                    // ── Table rows ────────────────────────────────────────
                    items(
                        items = state.students,
                        key   = { it.id },
                    ) { student ->
                        StudentRow(student = student)
                    }
                }
            }

            // Loading overlay
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = Sapphire,
                )
            }
        }
    }

    // ── Import bottom sheet ───────────────────────────────────────────────
    when (val imp = state.importState) {
        is ImportState.Idle    -> Unit
        is ImportState.Picking -> {
            LaunchedEffect(Unit) {
                fileLauncher.launch(arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                ))
                viewModel.cancelImport()
            }
        }
        is ImportState.Parsing -> ImportParsingSheet(progress = imp.progress)
        is ImportState.Preview -> ImportPreviewSheet(
            result    = imp,
            onConfirm = viewModel::confirmImport,
            onCancel  = viewModel::cancelImport,
        )
        is ImportState.Error -> ImportErrorDialog(
            message   = imp.message,
            onDismiss = viewModel::dismissImportError,
        )
        is ImportState.Done -> Unit
    }

    // ── Export bottom sheet ───────────────────────────────────────────────
    when (val exp = state.exportState) {
        is ExportState.Confirming -> ExportSheet(
            onConfirm = viewModel::confirmExport,
            onCancel  = viewModel::cancelExport,
        )
        is ExportState.Exporting -> ExportProgressSheet(progress = exp.progress)
        is ExportState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::clearExportState,
                title = { Text("Export Failed") },
                text  = { Text(exp.message) },
                confirmButton = { TextButton(onClick = viewModel::clearExportState) { Text("OK") } },
                containerColor = Surf3,
            )
        }
        else -> Unit
    }
}

// ── StudentRow ─────────────────────────────────────────────────────────────
@Composable
fun StudentRow(student: com.g.gradeapp.data.model.Student) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Surf2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(gradeColor(student.letterGrade))
        )
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1.8f)) {
                Text(
                    text  = student.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold, color = TextPrimary,
                    ),
                    maxLines = 1,
                )
                Text(
                    text  = "#${student.studentId}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 10.sp,
                    ),
                )
            }

            ScoreBar(student.homeworkScore,  modifier = Modifier.weight(1f))
            ScoreBar(student.midtermScore,   modifier = Modifier.weight(1f))
            ScoreBar(student.finalExamScore, modifier = Modifier.weight(1f))

            Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                Text(
                    text  = DecimalFormat("#.#").format(student.finalScore),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color      = gradeColor(student.letterGrade),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 13.sp,
                    ),
                )
            }

            LetterGradeBadge(letter = student.letterGrade)
        }
    }
}

// ── Import Sheets ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportParsingSheet(progress: Float) {
    ModalBottomSheet(
        onDismissRequest = {},
        containerColor   = Surf2,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = NavyOutline) },
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Parsing file…", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth(),
                color      = Sapphire,
                trackColor = Navy600,
            )
            Spacer(Modifier.height(60.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewSheet(
    result: ImportState.Preview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor   = Surf2,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = NavyOutline) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "Import Excel File",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary,
                ),
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = Sapphire.copy(alpha = 0.1f)) {
                    Text(
                        "${result.result.students.size} students",
                        style    = MaterialTheme.typography.labelMedium.copy(color = Sapphire),
                        modifier = Modifier.padding(8.dp, 5.dp),
                    )
                }
                if (result.result.warnings.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Warning.copy(alpha = 0.1f)) {
                        Text(
                            "⚠ ${result.result.warnings.size} warnings",
                            style    = MaterialTheme.typography.labelMedium.copy(color = Warning),
                            modifier = Modifier.padding(8.dp, 5.dp),
                        )
                    }
                }
            }

            if (result.result.warnings.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.06f)),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        result.result.warnings.take(3).forEach { w ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text("⚠ ", style = MaterialTheme.typography.bodySmall)
                                Text(w, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            }
                        }
                        if (result.result.warnings.size > 3) {
                            Text(
                                "+ ${result.result.warnings.size - 3} more…",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextDisabled),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = onCancel,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, NavyOutline),
                ) { Text("Cancel") }
                Button(
                    onClick  = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = OnSapphire),
                ) { Text("Import", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun ImportErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Import Failed", color = Error) },
        text    = { Text(message, color = TextSecondary) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = Sapphire) } },
        containerColor = Surf3,
    )
}

// ── Export Sheets ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    onConfirm: (ExportOptions) -> Unit,
    onCancel: () -> Unit,
) {
    var weightSheet   by remember { mutableStateOf(true) }
    var highlightFail by remember { mutableStateOf(true) }
    var distChart     by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor   = Surf2,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = NavyOutline) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "Export Results",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, color = TextPrimary,
                ),
            )
            Text(
                "Output mirrors your import with Final Score and Grade columns added.",
                style    = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Card(
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surf3),
            ) {
                ExportOptionRow("Include weight breakdown", "Config used in second tab", weightSheet)   { weightSheet   = it }
                HorizontalDivider(color = NavyDivider)
                ExportOptionRow("Highlight failing students", "Color F-grade rows red",  highlightFail) { highlightFail = it }
                HorizontalDivider(color = NavyDivider)
                ExportOptionRow("Include distribution chart", "Embed bar chart",         distChart)     { distChart     = it }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = onCancel,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, NavyOutline),
                ) { Text("Cancel") }

                Button(
                    onClick  = { onConfirm(ExportOptions(weightSheet, highlightFail, distChart)) },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Sapphire, contentColor = OnSapphire),
                ) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download .xlsx", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExportOptionRow(
    name: String, desc: String, checked: Boolean, onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = TextPrimary))
            Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Black,
                checkedTrackColor   = Sapphire,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Navy500,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportProgressSheet(progress: Float) {
    ModalBottomSheet(
        onDismissRequest = {},
        containerColor   = Surf2,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = NavyOutline) },
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Generating Excel…", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary))
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth(),
                color      = Sapphire,
                trackColor = Navy600,
            )
            Spacer(Modifier.height(60.dp))
        }
    }
}
