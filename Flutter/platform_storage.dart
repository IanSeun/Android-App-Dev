// lib/platform/platform_file_io.dart
//
// WEB-COMPATIBILITY LAYER — File I/O
// ─────────────────────────────────────────────────────────────────────────────
// Provides two public helpers consumed across the app:
//
//   pickExcelFile()     — opens a native or browser file picker
//   saveExcelBytes()    — triggers download or native save dialog
//
// Both work identically on web and desktop via kIsWeb guards +
// late-bound function pointers injected by main.dart.

import 'dart:typed_data';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

// ── Unified file pick result ───────────────────────────────────────────────
class PlatformFileResult {
  final String name;
  final Uint8List? bytes;  // non-null on web
  final String? path;      // non-null on desktop

  const PlatformFileResult({
    required this.name,
    this.bytes,
    this.path,
  });

  bool get isWeb => bytes != null;
}

// ── File picker ────────────────────────────────────────────────────────────
Future<PlatformFileResult?> pickExcelFile() async {
  final result = await FilePicker.platform.pickFiles(
    type: FileType.custom,
    allowedExtensions: ['xlsx', 'xls'],
    dialogTitle: 'Select Grade Sheet',
    // withData: true loads bytes into memory on web; ignored on desktop.
    withData: kIsWeb,
  );

  if (result == null || result.files.isEmpty) return null;
  final file = result.files.first;

  if (kIsWeb) {
    if (file.bytes == null) return null;
    return PlatformFileResult(name: file.name, bytes: file.bytes!);
  } else {
    if (file.path == null) return null;
    return PlatformFileResult(name: file.name, path: file.path!);
  }
}

// ── File saver / downloader ────────────────────────────────────────────────
Future<String> saveExcelBytes(Uint8List bytes, String suggestedName) async {
  if (kIsWeb) {
    _webDownload(bytes, suggestedName);
    return 'Downloading $suggestedName…';
  } else {
    return _saveOnDesktop(bytes, suggestedName);
  }
}

Future<String> _saveOnDesktop(Uint8List bytes, String fileName) async {
  final savePath = await FilePicker.platform.saveFile(
    dialogTitle: 'Save Grade Export',
    fileName: fileName,
    type: FileType.custom,
    allowedExtensions: ['xlsx'],
  );
  if (savePath == null) return 'Save cancelled';
  await _writeDesktopFile(savePath, bytes);
  return 'Saved to $savePath';
}

// ── Late-bound platform implementations ────────────────────────────────────
// Replaced at startup by configurePlatformFileIO() in main.dart.

void Function(Uint8List bytes, String fileName) _webDownload =
    (bytes, fileName) {
  // Default no-op — replaced before any export is triggered.
};

Future<void> Function(String path, Uint8List bytes) _writeDesktopFile =
    (path, bytes) async {
  // Default no-op — replaced before any export is triggered.
};

/// Called from main.dart to inject platform implementations.
void configurePlatformFileIO({
  void Function(Uint8List, String)? webDownload,
  Future<void> Function(String, Uint8List)? desktopWrite,
}) {
  if (webDownload != null) _webDownload = webDownload;
  if (desktopWrite != null) _writeDesktopFile = desktopWrite;
}
