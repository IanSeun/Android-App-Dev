name: gradeapp
description: GradeCalc — Student grading calculator. Web-compatible Flutter app.
publish_to: 'none'
version: 1.0.0+1

environment:
  sdk: '>=3.3.0 <4.0.0'
  flutter: '>=3.19.0'

dependencies:
  flutter:
    sdk: flutter

  # State management
  provider: ^6.1.2

  # SQLite — desktop only (guarded by kIsWeb at runtime, compiled out on web
  # via conditional imports in grade_database.dart / _sqflite_ffi_stub.dart)
  sqflite: ^2.3.3+1
  sqflite_common_ffi: ^2.3.3+2

  # path_provider — desktop only (no longer used by excel_exporter on web
  # because we removed the temp-file pattern; kept for desktop parity)
  path_provider: ^2.1.3
  path: ^1.9.0

  # SharedPreferences — works on web (uses localStorage) and desktop
  shared_preferences: ^2.3.2

  # File picker — works on web (withData:true returns Uint8List) and desktop
  file_picker: ^8.1.2

  # Excel I/O — pure Dart, works on web and desktop
  excel: ^4.0.3

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0

flutter:
  uses-material-design: true
