// lib/platform/file_reader_stub.dart
// Web stub — file reading is not available; bytes come from FilePicker directly.
import 'dart:typed_data';

Future<Uint8List> readFileBytes(String path) async {
  throw UnsupportedError('readFileBytes() is not available on web');
}
