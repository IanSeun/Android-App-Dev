# GradeCalc Flutter — Web Compatibility Changes

This document catalogues every substantive change made to port the Flutter
desktop application to the browser.  Changes are grouped by layer.

---

## 1  Platform / I/O Layer

### 1.1  `dart:io` Eliminated from Core Code

| File (before) | Problem | Resolution |
|---|---|---|
| `grades_viewmodel.dart` | `import 'dart:io'` at top level; `File`, `file.copy()` | Removed. `ExportDone` now carries `Uint8List` instead of `File`. |
| `excel_exporter.dart` | Returns `dart:io File`; uses `path_provider` + `File.writeAsBytes()` | Returns `Uint8List` directly. Removed `path_provider` dependency. |
| `excel_parser.dart` | `File(filePath).readAsBytes()` | Reads bytes from `PlatformFileResult` (FilePicker supplies bytes on web). |
| `grade_repository.dart` | Imported `GradeDatabase` directly | Now imports `PlatformStorage` (the abstract interface). |

### 1.2  New Platform Helpers

| File | Purpose |
|---|---|
| `lib/platform/file_reader_stub.dart` | Web stub for `readFileBytes()` |
| `lib/platform/file_reader_io.dart` | Desktop impl of `readFileBytes()` using `dart:io File` |
| `lib/platform/grade_database_stub.dart` | Web stub for `GradeDatabase` (conditional import from `platform_storage.dart`) |

### 1.3  `platform_file_io.dart` — Unified API

Two public functions replace all platform-divergent file code:

```dart
Future<PlatformFileResult?> pickExcelFile()
// → withData: true on web  (FilePicker returns Uint8List)
// → withData: false on desktop (FilePicker returns path)

Future<String> saveExcelBytes(Uint8List bytes, String suggestedName)
// → on web:     triggers <a download> via dart:html Blob anchor
// → on desktop: opens native saveFile() dialog, writes bytes via dart:io
```

### 1.4  `platform_storage.dart` — Fixed Conditional Import

The broken `_GradeDatabaseAccessor._getDb()` function-pointer pattern was
replaced with a proper conditional import:

```dart
import 'grade_database_stub.dart'
    if (dart.library.io) '../data/local/grade_database.dart' as db_impl;
```

On web, the stub satisfies the type references without pulling in
`sqflite` or `dart:io`.  On desktop, the real `GradeDatabase` is used.

### 1.5  `excel_exporter.dart` — In-Memory Export

Removed the temp-file pattern entirely.  `export()` now:
1. Builds the `Excel` workbook in memory.
2. Calls `excel.encode()` → `Uint8List`.
3. Returns bytes to the caller.

No `getTemporaryDirectory()`, no `File.writeAsBytes()`, no `path_provider`.

---

## 2  Navigation & Shell (`main.dart`)

### 2.1  Responsive Navigation Shell

The desktop version always used a bottom `NavigationBar`.  The web version
selects a layout based on viewport width:

| Viewport width | Layout |
|---|---|
| `< 600 dp` | Bottom `NavigationBar` (portrait mobile / very narrow) |
| `600–1199 dp` | Left `NavigationRail` with icons only |
| `≥ 1200 dp` | Left `NavigationRail` with icons **and labels** |

`IndexedStack` is retained for all three layouts so screen state is
preserved when switching tabs (no re-mount, no scroll-position loss).

### 2.2  `Breakpoints` Helper Class

```dart
class Breakpoints {
  static const double compact  = 600;   // bottom nav → rail threshold
  static const double medium   = 900;   // (used by screens)
  static const double expanded = 1200;  // rail icons → rail with labels
}
```

### 2.3  Text Scaling Clamped

```dart
textScaler: TextScaler.linear(
  mediaQuery.textScaler.scale(1.0).clamp(0.85, 1.15),
),
```

Prevents browser default-font-size changes from breaking the fixed-width
student-row column layout.

### 2.4  `_initDesktopStorage()` Cleaned Up

Removed 60 lines of dead code (`_cacheDesktopPlatform`, `_callInitPlatformString`,
`_detectPlatform`, `_nativePlatformName`, `_platformStringFromIo`) that
referenced undefined functions.  Desktop init is now a no-op comment —
`PlatformStorage.instance` auto-selects the right implementation.

---

## 3  Grades Screen (`grades_screen.dart`)

### 3.1  Responsive Two-Column Layout

At `≥ 860 dp`:
- **Left (flex 2):** scrollable student list + search + import banner.
- **Right (flex 1):** fixed summary panel — export button, stat tiles, distribution chart.

At `< 860 dp`:
- Single-column stack (legacy behaviour) with floating `FAB`.

### 3.2  Bottom Sheets → Web Dialogs

All `showModalBottomSheet()` calls replaced with centred `Dialog` widgets
constrained to `maxWidth: 480 dp`.

**Why:** Bottom sheets in web browsers often:
- Appear behind the browser's own bottom chrome on mobile.
- Don't scroll correctly inside a fixed-height `<iframe>`.
- Feel visually wrong in a desktop browser.

New helper:
```dart
Future<T?> showWebDialog<T>({
  required BuildContext context,
  required Widget child,
  double maxWidth = 480,
})
```

### 3.3  FAB in Wide Layout

On wide viewports, the `FloatingActionButton.extended` is replaced with an
`ElevatedButton.icon` in the summary panel header.  This prevents the FAB
from obscuring the last student row in the scrollable list.

### 3.4  Score Bar — Flexible Width

`ScoreBar` previously contained a hard-coded `SizedBox(width: 44)`.  On
narrow browser windows this caused horizontal overflow.

**Fix:** Removed the fixed `SizedBox`; bar now fills 100% of available
width (parent `Row` uses `Expanded(flex: 10)` to size it).

### 3.5  Narrow-Viewport Column Hiding

Student rows conditionally hide the three `ScoreBar` columns when
`MediaQuery width < 480 dp`:

```dart
final showScoreBars = width >= 480;
if (showScoreBars) ...[
  Expanded(flex: 10, child: ScoreBar(score: student.homeworkScore)),
  ...
]
```

### 3.6  Scroll Physics

`ListView` uses `ClampingScrollPhysics` instead of the default
`BouncingScrollPhysics`.  Bouncing physics looks wrong inside a browser
scroll container.

---

## 4  Config Screen (`config_screen.dart`)

### 4.1  Two-Column Wide Layout

At `≥ 860 dp`: Assessment Weights card (left) + Grade Scale card (right)
side by side.  Action buttons span full width below both cards.

### 4.2  Max-Width Constraint

Content is wrapped in `ConstrainedBox(maxWidth: 900)` + `Center` so the
layout does not stretch to fill an ultra-wide viewport.

### 4.3  Threshold Row Narrow Adaptation

`_GradeThresholdRow` hides the `"Min score:"` label when viewport `< 420 dp`
to prevent the row from overflowing:

```dart
final showLabel = MediaQuery.sizeOf(context).width >= 420;
if (showLabel) const SizedBox(width: 66, child: Text('Min score:', …))
```

The trailing hint text is wrapped in `Flexible` + `TextOverflow.ellipsis`.

---

## 5  Stats Screen (`stats_screen.dart`)

### 5.1  Two-Column Wide Layout

At `≥ 860 dp`:
- **Left:** Pass-rate ring + summary stat tiles.
- **Right:** Grade distribution chart + score range breakdown.

### 5.2  Max-Width + Scroll Physics

Same `900 dp` max-width constraint and `ClampingScrollPhysics` as config screen.

---

## 6  Web Entry Point (`web/index.html`)

- `viewport` meta with `width=device-width` prevents mobile zoom on text input focus.
- `theme-color: #060C18` colours the browser chrome to match the app shell.
- Pre-Flutter loading screen avoids a flash of white before the canvas mounts.
- `flutter-first-frame` event hides the loading overlay as soon as Flutter paints.
- `web/manifest.json` added for PWA installability.

---

## 7  Summary of File Changes

| File | Change type |
|---|---|
| `lib/viewmodel/grades_viewmodel.dart` | Rewritten — removed `dart:io`, `FilePicker` direct calls |
| `lib/util/excel_exporter.dart` | Rewritten — returns `Uint8List`, removed `path_provider` |
| `lib/util/excel_parser.dart` | Rewritten — accepts `PlatformFileResult`, conditional file read |
| `lib/repository/grade_repository.dart` | Updated — imports `PlatformStorage` not `GradeDatabase` |
| `lib/platform/platform_storage.dart` | Rewritten — proper conditional import, fixed accessor pattern |
| `lib/platform/platform_file_io.dart` | Updated — cleaner `saveExcelBytes`, removed dead code |
| `lib/platform/web_download.dart` | Minor — added comment about dart:html vs dart:js_interop |
| `lib/ui/screens/grades_screen.dart` | Rewritten — responsive layout, dialogs, ScoreBar fix |
| `lib/ui/screens/config_screen.dart` | Rewritten — two-column wide layout, narrow adaptations |
| `lib/ui/screens/stats_screen.dart` | Rewritten — two-column wide layout, ClampingScrollPhysics |
| `lib/ui/components/components.dart` | `ScoreBar` fixed (removed `SizedBox(width: 44)`) |
| `lib/main.dart` | Updated — removed dead init code, breakpoints class |
| `lib/platform/file_reader_stub.dart` | **New** — web stub for conditional file read import |
| `lib/platform/file_reader_io.dart` | **New** — desktop impl of `readFileBytes()` |
| `lib/platform/grade_database_stub.dart` | **New** — web stub for `GradeDatabase` conditional import |
| `web/index.html` | **New** — Flutter web entry point |
| `web/manifest.json` | **New** — PWA manifest |
| `pubspec.yaml` | Updated — removed `google_fonts` (unused), clarified comments |
