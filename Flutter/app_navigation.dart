// lib/platform/platform_storage.dart
//
// WEB-COMPATIBILITY LAYER — Unified storage abstraction
// ─────────────────────────────────────────────────────────────────────────────
// On web:     _WebStorage — stores all data as JSON in SharedPreferences
//             (localStorage).  No dart:io, sqflite, or file system.
//
// On desktop: _NativeStorageBridge — delegates to GradeDatabase (sqflite).
//             GradeDatabase is imported via a conditional import so its
//             dart:io / sqflite references are never compiled on web.

import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:shared_preferences/shared_preferences.dart';

import '../data/model/models.dart';

// Conditional import: on web the stub resolves; on desktop GradeDatabase is used.
import 'grade_database_stub.dart'
    if (dart.library.io) '../data/local/grade_database.dart' as db_impl;

// ── Abstract interface ─────────────────────────────────────────────────────
abstract class PlatformStorage {
  static PlatformStorage? _instance;

  static PlatformStorage get instance {
    _instance ??= kIsWeb ? _WebStorage() : _NativeStorageBridge();
    return _instance!;
  }

  Future<void> init();

  // ── Students ───────────────────────────────────────────────────────────
  Future<List<Student>> getStudentsBySession(int sessionId);
  Future<List<Student>> searchStudents(int sessionId, String query);
  Future<void> insertAllStudents(List<Student> students);
  Future<void> deleteStudentsBySession(int sessionId);
  Future<void> updateGrade(int id, double score, String grade);

  // ── Sessions ───────────────────────────────────────────────────────────
  Future<List<ImportSession>> getAllSessions();
  Future<ImportSession?> getActiveSession();
  Future<int> insertSession(ImportSession session);
  Future<void> deactivateAllSessions();
  Future<void> activateSession(int id);
  Future<void> deleteSession(int id);
}

// ── Web implementation (SharedPreferences / JSON) ──────────────────────────
class _WebStorage extends PlatformStorage {
  static const _studentsKey = 'web_students';
  static const _sessionsKey = 'web_sessions';
  static const _nextIdKey   = 'web_next_id';

  late SharedPreferences _prefs;
  int _nextId = 1;

  @override
  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
    _nextId = _prefs.getInt(_nextIdKey) ?? 1;
  }

  int _allocId() {
    final id = _nextId++;
    _prefs.setInt(_nextIdKey, _nextId);
    return id;
  }

  List<Map<String, dynamic>> _loadStudents() {
    final raw = _prefs.getString(_studentsKey);
    if (raw == null) return [];
    return (jsonDecode(raw) as List).cast<Map<String, dynamic>>();
  }

  void _saveStudents(List<Map<String, dynamic>> list) {
    _prefs.setString(_studentsKey, jsonEncode(list));
  }

  List<Map<String, dynamic>> _loadSessions() {
    final raw = _prefs.getString(_sessionsKey);
    if (raw == null) return [];
    return (jsonDecode(raw) as List).cast<Map<String, dynamic>>();
  }

  void _saveSessions(List<Map<String, dynamic>> list) {
    _prefs.setString(_sessionsKey, jsonEncode(list));
  }

  @override
  Future<List<Student>> getStudentsBySession(int sessionId) async {
    return _loadStudents()
        .where((m) => m['sessionId'] == sessionId)
        .map(Student.fromMap)
        .toList()
      ..sort((a, b) => a.name.compareTo(b.name));
  }

  @override
  Future<List<Student>> searchStudents(int sessionId, String query) async {
    final q = query.toLowerCase();
    final all = await getStudentsBySession(sessionId);
    return all.where((s) =>
        s.name.toLowerCase().contains(q) ||
        s.studentId.toLowerCase().contains(q)).toList();
  }

  @override
  Future<void> insertAllStudents(List<Student> students) async {
    final existing = _loadStudents();
    if (students.isNotEmpty) {
      existing.removeWhere((m) => m['sessionId'] == students.first.sessionId);
    }
    for (final s in students) {
      final m = s.toMap();
      m['id'] = _allocId();
      existing.add(m);
    }
    _saveStudents(existing);
  }

  @override
  Future<void> deleteStudentsBySession(int sessionId) async {
    final all = _loadStudents();
    all.removeWhere((m) => m['sessionId'] == sessionId);
    _saveStudents(all);
  }

  @override
  Future<void> updateGrade(int id, double score, String grade) async {
    final all = _loadStudents();
    for (var i = 0; i < all.length; i++) {
      if (all[i]['id'] == id) {
        all[i] = {...all[i], 'finalScore': score, 'letterGrade': grade};
      }
    }
    _saveStudents(all);
  }

  @override
  Future<List<ImportSession>> getAllSessions() async {
    return _loadSessions().map(ImportSession.fromMap).toList()
      ..sort((a, b) => b.importedAt.compareTo(a.importedAt));
  }

  @override
  Future<ImportSession?> getActiveSession() async {
    final all = _loadSessions();
    final active = all.where((m) => (m['isActive'] as int) == 1).toList();
    if (active.isEmpty) return null;
    active.sort((a, b) => (b['importedAt'] as int).compareTo(a['importedAt'] as int));
    return ImportSession.fromMap(active.first);
  }

  @override
  Future<int> insertSession(ImportSession session) async {
    final all = _loadSessions();
    final id = _allocId();
    final m = session.toMap();
    m['id'] = id;
    all.add(m);
    _saveSessions(all);
    return id;
  }

  @override
  Future<void> deactivateAllSessions() async {
    final all = _loadSessions();
    for (var i = 0; i < all.length; i++) {
      all[i] = {...all[i], 'isActive': 0};
    }
    _saveSessions(all);
  }

  @override
  Future<void> activateSession(int id) async {
    final all = _loadSessions();
    for (var i = 0; i < all.length; i++) {
      if (all[i]['id'] == id) all[i] = {...all[i], 'isActive': 1};
    }
    _saveSessions(all);
  }

  @override
  Future<void> deleteSession(int id) async {
    final all = _loadSessions();
    all.removeWhere((m) => m['id'] == id);
    _saveSessions(all);
  }
}

// ── Desktop (native) bridge — wraps GradeDatabase via conditional import ────
class _NativeStorageBridge extends PlatformStorage {
  // db_impl resolves to GradeDatabase on desktop, stub on web.
  // Since this class is only ever instantiated when kIsWeb == false,
  // GradeDatabase and its dart:io/sqflite dependencies are always available.
  late final dynamic _db;

  @override
  Future<void> init() async {
    _db = db_impl.GradeDatabase.instance;
    // GradeDatabase opens lazily on first query.
  }

  @override
  Future<List<Student>> getStudentsBySession(int sessionId) =>
      _db.getStudentsBySession(sessionId);

  @override
  Future<List<Student>> searchStudents(int sessionId, String query) =>
      _db.searchStudents(sessionId, query);

  @override
  Future<void> insertAllStudents(List<Student> students) =>
      _db.insertAllStudents(students);

  @override
  Future<void> deleteStudentsBySession(int sessionId) =>
      _db.deleteStudentsBySession(sessionId);

  @override
  Future<void> updateGrade(int id, double score, String grade) =>
      _db.updateGrade(id, score, grade);

  @override
  Future<List<ImportSession>> getAllSessions() => _db.getAllSessions();

  @override
  Future<ImportSession?> getActiveSession() => _db.getActiveSession();

  @override
  Future<int> insertSession(ImportSession session) => _db.insertSession(session);

  @override
  Future<void> deactivateAllSessions() => _db.deactivateAllSessions();

  @override
  Future<void> activateSession(int id) => _db.activateSession(id);

  @override
  Future<void> deleteSession(int id) => _db.deleteSession(id);
}
