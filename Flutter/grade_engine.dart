// lib/viewmodel/grades_viewmodel.dart
//
// WEB-COMPATIBILITY CHANGES FROM DESKTOP VERSION:
// ─────────────────────────────────────────────────────────────────────────────
// 1. dart:io REMOVED — File, dart:io imports eliminated entirely.
//    ExportDone now carries Uint8List bytes instead of a File reference.
//
// 2. FILE PICK — replaced FilePicker.platform.pickFiles() with
//    pickExcelFile() from platform_file_io.dart, which returns either a
//    file path (desktop) or in-memory bytes (web) via PlatformFileResult.
//
// 3. FILE SAVE — replaced FilePicker.platform.saveFile() + File.copy() with
//    saveExcelBytes() from platform_file_io.dart, which triggers a browser
//    download anchor on web and a native save-dialog on desktop.
//
// 4. EXCEL EXPORTER — ExcelExporter.export() now returns Uint8List instead
//    of a File.  ExcelParser.parse() accepts PlatformFileResult.

import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';

import '../data/model/models.dart';
import '../platform/platform_file_io.dart';
import '../repository/config_repository.dart';
import '../repository/grade_repository.dart';
import '../util/excel_exporter.dart';
import '../util/excel_parser.dart';
import '../util/grade_engine.dart';

// ── State classes (sealed hierarchy) ─────────────────────────────────────
sealed class ImportState {}
class ImportIdle extends ImportState {}
class ImportPicking extends ImportState {}
class ImportParsing extends ImportState {
  final double progress;
  ImportParsing([this.progress = 0.0]);
}
class ImportPreview extends ImportState {
  final ParseResult result;
  final String fileName;
  ImportPreview(this.result, this.fileName);
}
class ImportError extends ImportState {
  final String message;
  ImportError(this.message);
}
class ImportDone extends ImportState {}

sealed class ExportState {}
class ExportIdle extends ExportState {}
class ExportConfirming extends ExportState {}
class ExportExporting extends ExportState {
  final double progress;
  ExportExporting([this.progress = 0.0]);
}
class ExportDone extends ExportState {
  final Uint8List bytes;
  ExportDone(this.bytes);
}
class ExportError extends ExportState {
  final String message;
  ExportError(this.message);
}

// ── UI State ──────────────────────────────────────────────────────────────
class GradesUiState {
  final ImportSession? session;
  final List<Student> students;
  final String searchQuery;
  final bool isLoading;
  final ImportState importState;
  final ExportState exportState;
  final String? snackMessage;
  final double avgScore;
  final double passRate;
  final Student? highestStudent;
  final Map<String, int> distribution;

  const GradesUiState({
    this.session,
    this.students = const [],
    this.searchQuery = '',
    this.isLoading = false,
    ImportState? importState,
    ExportState? exportState,
    this.snackMessage,
    this.avgScore = 0.0,
    this.passRate = 0.0,
    this.highestStudent,
    this.distribution = const {},
  })  : importState = importState ?? const _IdleImport(),
        exportState = exportState ?? const _IdleExport();

  GradesUiState copyWith({
    ImportSession? Function()? session,
    List<Student>? students,
    String? searchQuery,
    bool? isLoading,
    ImportState? importState,
    ExportState? exportState,
    String? Function()? snackMessage,
    double? avgScore,
    double? passRate,
    Student? Function()? highestStudent,
    Map<String, int>? distribution,
  }) {
    return GradesUiState(
      session: session != null ? session() : this.session,
      students: students ?? this.students,
      searchQuery: searchQuery ?? this.searchQuery,
      isLoading: isLoading ?? this.isLoading,
      importState: importState ?? this.importState,
      exportState: exportState ?? this.exportState,
      snackMessage: snackMessage != null ? snackMessage() : this.snackMessage,
      avgScore: avgScore ?? this.avgScore,
      passRate: passRate ?? this.passRate,
      highestStudent: highestStudent != null ? highestStudent() : this.highestStudent,
      distribution: distribution ?? this.distribution,
    );
  }
}

class _IdleImport extends ImportState implements ImportIdle { const _IdleImport(); }
class _IdleExport extends ExportState implements ExportIdle { const _IdleExport(); }

// ── ViewModel ─────────────────────────────────────────────────────────────
class GradesViewModel extends ChangeNotifier {
  final GradeRepository _gradeRepo;
  final ConfigRepository _configRepo;
  final ExcelParser _parser;
  final ExcelExporter _exporter;

  GradesUiState _state = const GradesUiState();
  GradesUiState get state => _state;

  String _rawSearchQuery = '';
  Timer? _debounce;
  StreamSubscription<ImportSession?>? _sessionSub;
  StreamSubscription<List<Student>>? _studentsSub;

  GradesViewModel({
    required GradeRepository gradeRepo,
    required ConfigRepository configRepo,
    required ExcelParser parser,
    required ExcelExporter exporter,
  })  : _gradeRepo = gradeRepo,
        _configRepo = configRepo,
        _parser = parser,
        _exporter = exporter {
    _init();
  }

  void _init() {
    _loadActiveSession();
    _sessionSub = _gradeRepo.activeSessionStream.listen((session) {
      _update((s) => s.copyWith(session: () => session));
      if (session != null) _subscribeStudents(session.id);
    });
  }

  Future<void> _loadActiveSession() async {
    final session = await _gradeRepo.getActiveSession();
    _update((s) => s.copyWith(session: () => session));
    if (session != null) {
      _subscribeStudents(session.id);
      await _gradeRepo.refreshStudents(session.id);
    }
  }

  void _subscribeStudents(int sessionId) {
    _studentsSub?.cancel();
    _studentsSub = _gradeRepo.studentsStream.listen(_applyFilter);
  }

  void _applyFilter(List<Student> all) {
    final q = _rawSearchQuery.trim().toLowerCase();
    final filtered = q.isEmpty
        ? all
        : all.where((s) {
            return s.name.toLowerCase().contains(q) ||
                s.studentId.toLowerCase().contains(q);
          }).toList();
    _update((s) => s.copyWith(
          students: filtered,
          avgScore: GradeEngine.average(filtered),
          passRate: GradeEngine.passRate(filtered),
          highestStudent: () => GradeEngine.highest(filtered),
          distribution: GradeEngine.distribution(filtered),
          isLoading: false,
        ));
  }

  void setSearchQuery(String q) {
    _rawSearchQuery = q;
    _update((s) => s.copyWith(searchQuery: q));
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 250), () async {
      final session = _state.session;
      if (session == null) return;
      final all = q.isEmpty
          ? await _gradeRepo.getStudentsBySession(session.id)
          : await _gradeRepo.searchStudents(session.id, q);
      _applyFilter(all);
    });
  }

  // ── Import ──────────────────────────────────────────────────────────────
  void requestImport() {
    _update((s) => s.copyWith(importState: ImportPicking()));
    _pickFile();
  }

  Future<void> _pickFile() async {
    final fileResult = await pickExcelFile();
    if (fileResult == null) {
      _update((s) => s.copyWith(importState: ImportIdle()));
      return;
    }
    await onFilePicked(fileResult);
  }

  Future<void> onFilePicked(PlatformFileResult fileResult) async {
    _update((s) => s.copyWith(importState: ImportParsing(0.2)));
    final tempSessionId = DateTime.now().millisecondsSinceEpoch;
    final result = await _parser.parse(fileResult, tempSessionId);
    if (result.hasError) {
      _update((s) => s.copyWith(
            importState: ImportError(result.errorMessage ?? 'Unknown error'),
          ));
    } else {
      _update((s) => s.copyWith(
            importState: ImportPreview(result, fileResult.name),
          ));
    }
  }

  Future<void> confirmImport() async {
    final preview = _state.importState;
    if (preview is! ImportPreview) return;
    _update((s) => s.copyWith(importState: ImportParsing(0.7)));

    final sessionId = await _gradeRepo.createSession(
      fileName: preview.fileName,
      studentCount: preview.result.students.length,
      warningCount: preview.result.warnings.length,
    );

    final weights = await _configRepo.getWeightConfig();
    final scale = await _configRepo.getGradeScale();
    final withGrades = GradeEngine.calculate(
      preview.result.students.map((s) => s.copyWith(sessionId: sessionId)).toList(),
      weights, scale,
    );

    await _gradeRepo.saveStudents(withGrades);
    _update((s) => s.copyWith(importState: ImportDone()));
    _showSnack('✓ ${preview.result.students.length} students imported');
    await _loadActiveSession();
  }

  void cancelImport() => _update((s) => s.copyWith(importState: ImportIdle()));
  void dismissImportError() => _update((s) => s.copyWith(importState: ImportIdle()));

  // ── Export ──────────────────────────────────────────────────────────────
  void requestExport() {
    if (_state.session == null) { _showSnack('Import data first'); return; }
    _update((s) => s.copyWith(exportState: ExportConfirming()));
  }

  Future<void> confirmExport(ExportOptions options) async {
    _update((s) => s.copyWith(exportState: ExportExporting(0.3)));
    try {
      final weights = await _configRepo.getWeightConfig();
      final scale = await _configRepo.getGradeScale();
      final bytes = await _exporter.export(
        students: _state.students,
        weights: weights,
        scale: scale,
        options: options,
        sourceFileName: _state.session?.fileName ?? 'grades',
      );
      final fileName = 'grades_export_${DateTime.now().millisecondsSinceEpoch}.xlsx';
      await saveExcelBytes(bytes, fileName);
      _update((s) => s.copyWith(exportState: ExportDone(bytes)));
    } catch (e) {
      _update((s) => s.copyWith(exportState: ExportError(e.toString())));
    }
  }

  void cancelExport() => _update((s) => s.copyWith(exportState: ExportIdle()));
  void clearExportState() => _update((s) => s.copyWith(exportState: ExportIdle()));

  Future<void> recalculate() async {
    final sessionId = _state.session?.id;
    if (sessionId == null) return;
    final weights = await _configRepo.getWeightConfig();
    final scale = await _configRepo.getGradeScale();
    await _gradeRepo.recalculate(sessionId, weights, scale);
    _showSnack('Grades recalculated');
  }

  void _showSnack(String msg) => _update((s) => s.copyWith(snackMessage: () => msg));
  void clearSnack() => _update((s) => s.copyWith(snackMessage: () => null));

  void _update(GradesUiState Function(GradesUiState) updater) {
    _state = updater(_state);
    notifyListeners();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _sessionSub?.cancel();
    _studentsSub?.cancel();
    super.dispose();
  }
}
