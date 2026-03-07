// lib/platform/_sqflite_ffi_stub.dart
//
// Web stub that satisfies the conditional import of sqflite_common_ffi.
// On web, sqflite_common_ffi is never called (kIsWeb guards prevent it),
// but Dart needs the symbols to resolve at compile time.
//
// This file exports no-op implementations of the symbols referenced in
// grade_database.dart so the web build compiles without errors.

// ignore_for_file: non_constant_identifier_names

// sqfliteFfiInit() no-op
void sqfliteFfiInit() {}

// databaseFactoryFfi no-op object
final databaseFactoryFfi = _StubDatabaseFactory();

// Minimal stub to satisfy the type reference
class _StubDatabaseFactory {}
