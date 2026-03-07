// lib/platform/grade_database_stub.dart
//
// Web stub satisfying the conditional import in platform_storage.dart.
// On web, _NativeStorageBridge is never instantiated (kIsWeb guard prevents it),
// but Dart's conditional import requires both branches to export the same symbols.

import '../data/model/models.dart';

class GradeDatabase {
  static GradeDatabase get instance =>
      throw UnsupportedError('GradeDatabase is not available on web');

  Future<List<Student>> getStudentsBySession(int sessionId) async => [];
  Future<List<Student>> searchStudents(int sessionId, String query) async => [];
  Future<void> insertAllStudents(List<Student> students) async {}
  Future<void> deleteStudentsBySession(int sessionId) async {}
  Future<void> updateGrade(int id, double score, String grade) async {}
  Future<List<ImportSession>> getAllSessions() async => [];
  Future<ImportSession?> getActiveSession() async => null;
  Future<int> insertSession(ImportSession session) async => 0;
  Future<void> deactivateAllSessions() async {}
  Future<void> activateSession(int id) async {}
  Future<void> deleteSession(int id) async {}
}
