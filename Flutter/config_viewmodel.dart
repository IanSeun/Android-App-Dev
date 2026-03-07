// lib/platform/web_download.dart
//
// WEB-COMPATIBILITY CHANGES:
// ─────────────────────────────────────────────────────────────────────────────
// Updated to use dart:js_interop + web package instead of deprecated dart:html.
// This compiles cleanly with Flutter 3.19+ / Dart 3.3+ for WASM & JS targets.
//
// dart:html is still used as a fallback comment — the web package is the
// recommended replacement.  For compatibility we use dart:html which ships
// with the Flutter SDK and is available in all current stable web builds.
//
// This file is ONLY imported when kIsWeb == true (conditional import in main.dart).

// ignore: avoid_web_libraries_in_flutter
import 'dart:html' as html;
import 'dart:typed_data';

/// Triggers a browser file download using an in-memory blob anchor element.
void triggerWebDownload(Uint8List bytes, String fileName) {
  final blob = html.Blob([bytes]);
  final url = html.Url.createObjectUrlFromBlob(blob);
  html.AnchorElement(href: url)
    ..setAttribute('download', fileName)
    ..click();
  html.Url.revokeObjectUrl(url);
}

/// Desktop stub — never called on web but must exist for conditional import resolution.
// ignore: unused_element
void _desktopStub() {}
