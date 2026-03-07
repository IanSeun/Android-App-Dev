// lib/platform/file_reader_io.dart
// Desktop implementation — reads a file from the file system.
import 'dart:io';
import 'dart:typed_data';

Future<Uint8List> readFileBytes(String path) async {
  return await File(path).readAsBytes();
}
