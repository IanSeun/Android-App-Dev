// lib/platform/desktop_file_write.dart
//
// Desktop-only file: writes bytes to a file path using dart:io.
// This file is ONLY compiled on non-web builds.
//
// The conditional import in main.dart ensures this is never loaded on web.

import 'dart:io';
import 'dart:typed_data';

Future<void> writeDesktopFile(String path, Uint8List bytes) async {
  await File(path).writeAsBytes(bytes);
}

// Stub that matches web_download.dart's exported symbol so both files
// can be used interchangeably via conditional import.
void triggerWebDownload(Uint8List bytes, String fileName) {
  // No-op on desktop — this symbol is never called, but must exist
  // so the conditional import resolves cleanly.
}
