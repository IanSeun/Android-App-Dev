// lib/repository/grade_repository.dart
//
// WEB-COMPATIBILITY CHANGE FROM DESKTOP VERSION:
// ─────────────────────────────────────────────────────────────────────────────
// GradeDatabase replaced with PlatformStorage — the unified abstract interface
// that delegates to sqflite on desktop and SharedPreferences JSON on web.
// No dart:io references.  All DAO method calls are identical — PlatformStorage
// exposes the same methods as GradeDatabase.

import 'dart:async';
import '../data/model/models.dart';
import '../platform/platform_storage.dart';
import '../util/grade_engine.dart';

class GradeRepository {
  final PlatformStorage _db;

  GradeRepository(this._db);

  // ── Streams ────────────────────────────────────────────────────────────
  final _sessionCtrl = StreamController<ImportSession?>.broadcast();
  Stream<ImportSession?> get activeSessionStream => _sessionCtrl.stream;

  final _studentsCtrl = StreamController<List<Student>>.broadcast();
  Stream<List<Student>> get studentsStream => _studentsCtrl.stream;

  // ── Sessions ───────────────────────────────────────────────────────────
  Future<List<ImportSession>> getAllSessions() => _db.getAllSessions();
  Future<ImportSession?> getActiveSession() => _db.getActiveSession();

  Future<int> createSession({
    required String fileName,
    required int studentCount,
    required int warningCount,
  }) async {
    await _db.deactivateAllSessions();
    final id = await _db.insertSession(ImportSession.create(
      fileName: fileName,
      studentCount: studentCount,
      warningCount: warningCount,
    ));
    final session = await _db.getActiveSession();
    _sessionCtrl.add(session);
    return id;
  }

  Future<void> activateSession(int id) async {
    await _db.deactivateAllSessions();
    await _db.activateSession(id);
    final session = await _db.getActiveSession();
    _sessionCtrl.add(session);
  }

  // ── Students ───────────────────────────────────────────────────────────
  Future<List<Student>> getStudentsBySession(int sessionId) =>
      _db.getStudentsBySession(sessionId);

  Future<List<Student>> searchStudents(int sessionId, String query) =>
      _db.searchStudents(sessionId, query);

  Future<void> saveStudents(List<Student> students) async {
    await _db.insertAllStudents(students);
    if (students.isNotEmpty) {
      final updated = await _db.getStudentsBySession(students.first.sessionId);
      _studentsCtrl.add(updated);
    }
  }

  Future<void> recalculate(int sessionId, WeightConfig weights, GradeScale scale) async {
    final current = await _db.getStudentsBySession(sessionId);
    final recalc = GradeEngine.calculate(current, weights, scale);
    for (final s in recalc) {
      await _db.updateGrade(s.id, s.finalScore, s.letterGrade);
    }
    _studentsCtrl.add(recalc);
  }

  Future<void> refreshStudents(int sessionId) async {
    final students = await _db.getStudentsBySession(sessionId);
    _studentsCtrl.add(students);
  }

  void dispose() {
    _sessionCtrl.close();
    _studentsCtrl.close();
  }
}
