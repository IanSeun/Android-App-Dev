# WEB_COMPATIBILITY.md
# GradeCalc Flutter → Web Refactor Documentation

## Overview

This document records all substantial changes made to the Flutter/Dart codebase
to ensure GradeCalc runs correctly and responsively in standard web browsers.
The original codebase targeted Linux/Windows desktop with Jetpack Compose idioms
translated to Flutter desktop widgets. The web target requires different scroll
physics, layout strategies, navigation patterns, dialog styles, file I/O, and
cursor behaviours.

---

## 1. Navigation — Three-Tier Responsive Layout

### Before (desktop-only)
```
Always: bottom NavigationBar
```

### After (web-responsive)
| Viewport width | Layout |
|---|---|
| < 600 dp | Bottom `NavigationBar` — portrait phone / narrow browser |
| 600 – 1199 dp | Left `NavigationRail`, **icon-only** — tablet / medium browser |
| ≥ 1200 dp | Left `NavigationRail` with **labels + app branding** — desktop web |

**Files changed:** `lib/main.dart`  
**Key widgets added:**
- `_Sidebar` — extracted stateless widget managing logo + rail items
- `_WebTopBar` — 48 dp top bar showing section name and version badge
- `_AppBarTitle` — app bar content for compact layout
- `_RailItem` — animated rail destination with `MouseRegion` cursor

**Why:** Bottom navigation is mobile-centric and hidden by the browser's own
status bar on many devices. A persistent sidebar rail is the standard web SaaS
navigation pattern (Notion, Linear, Figma, etc.).

---

## 2. Content Width Cap — `_CentredContent`

### Before
Content stretched to fill the full browser window width.

### After
```dart
class _CentredContent extends StatelessWidget {
  // Caps content at 960 dp and centres it on wider viewports.
  // Below 960 dp: renders normally (no overhead).
}
```

**Breakpoint:** `Breakpoints.maxContentWidth = 960`  
**Files changed:** `lib/main.dart`

**Why:** Reading text or scanning table rows that span a 2560 px ultra-wide
monitor is ergonomically poor. 960 dp keeps content in a readable column
while using the remaining canvas for the sidebar and breathing room.

---

## 3. Scroll Behaviour — `_WebScrollBehavior`

### Before
Default `MaterialScrollBehavior` — touch-only drag, `BouncingScrollPhysics`.

### After
```dart
class _WebScrollBehavior extends MaterialScrollBehavior {
  @override
  Set<PointerDeviceKind> get dragDevices => {
    PointerDeviceKind.touch,
    PointerDeviceKind.mouse,    // ← enables mouse-drag scrolling
    PointerDeviceKind.trackpad, // ← enables trackpad drag scrolling
  };

  @override
  ScrollPhysics getScrollPhysics(BuildContext context) =>
      const ClampingScrollPhysics(); // ← no bounce in browsers
}
```

Applied globally via `MaterialApp(scrollBehavior: const _WebScrollBehavior())`.  
Individual `ListView` / `SingleChildScrollView` widgets also specify
`physics: const ClampingScrollPhysics()` explicitly.

**Files changed:** `lib/main.dart`, `lib/ui/screens/grades_screen.dart`,
`lib/ui/screens/config_screen.dart`, `lib/ui/screens/stats_screen.dart`

**Why:** iOS-style bounce physics looks broken in a browser scrollable area.
Mouse-drag scrolling is expected by desktop web users.

---

## 4. Text Scaling — clamped `textScaler`

### Before
No clamping — system font-size changes could break fixed-width column layouts.

### After
```dart
// In GradeCalcApp.builder:
final clamped = mq.textScaler.scale(1.0).clamp(0.85, 1.15);
return MediaQuery(
  data: mq.copyWith(textScaler: TextScaler.linear(clamped)),
  child: child!,
);
```

**Files changed:** `lib/main.dart`

**Why:** Browsers let users change their default font size. The student-row
layout has fixed-flex proportions that overflow when text is scaled beyond
~115 %. Clamping guards against this while still respecting mild preferences.

---

## 5. Mouse Cursor — `MouseRegion`

### Before
No mouse cursor customisation — browser always shows text/arrow cursor.

### After
All tappable, interactive elements are wrapped:
```dart
MouseRegion(
  cursor: SystemMouseCursors.click,
  child: GestureDetector(onTap: ..., child: ...),
)
```

Applies to: import banner, empty state banner, rail items, FAB, sliders,
action buttons.

**Files changed:** `lib/main.dart`, `lib/ui/components/components.dart`,
`lib/ui/screens/grades_screen.dart`, `lib/ui/screens/config_screen.dart`

**Why:** Without `MouseRegion`, clicking on Cards and GestureDetectors shows
a text-cursor in the browser, which is confusing. Standard web UX requires
a pointer cursor over clickable elements.

---

## 6. Dialogs vs Bottom-Sheets (Viewport-Aware)

### Before
All modals use `showModalBottomSheet` — slides up from the bottom.

### After
```dart
bool _isWideCtx(BuildContext ctx) => !Breakpoints.isCompact(ctx);

Future<bool?> showImportPreviewSheet(BuildContext ctx, ImportPreview preview) {
  if (_isWideCtx(ctx)) {
    return showDialog<bool>(   // ← centred Dialog on wide viewports
      builder: (_) => Dialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        child: SizedBox(width: 440, child: _ImportPreviewContent(...)),
      ),
    );
  }
  return showModalBottomSheet<bool>(...); // ← kept for compact viewports
}
```

Applies to: Import Preview, Export Options.  
Error dialogs use `showDialog` universally (already correct for web).

**Files changed:** `lib/ui/screens/grades_screen.dart`

**Why:** Bottom-sheets slide up from the bottom edge of the screen. On a
desktop browser this looks like a native mobile app transplanted into a
browser — jarring and unexpected. Centred Dialog widgets are the standard web
modal pattern.

---

## 7. Grades Screen — Responsive Student Row

### Before
Every student row shows: Name | HW bar | Midterm bar | Final bar | Score | Badge.
All items present regardless of viewport width, causing overflow on narrow screens.

### After
```dart
class _StudentRow extends StatelessWidget {
  final bool isWide; // passed from parent GradesScreen

  Widget build(BuildContext context) {
    return ..Row(
      children: [
        Expanded(flex: isWide ? 18 : 24, child: _nameColumn),
        if (isWide) ...[
          ScoreBar(score: student.homeworkScore),
          ScoreBar(score: student.midtermScore),
          ScoreBar(score: student.finalExamScore),
        ],
        Text(finalScore),
        LetterGradeBadge(letter: grade),
      ],
    );
  }
}
```

**Files changed:** `lib/ui/screens/grades_screen.dart`

**Why:** Three `ScoreBar` columns at ≈44 dp each plus name and badge doesn't fit
below ≈500 dp. Hiding the score bars on compact viewports keeps the row
readable on small browser windows and portrait phones.

---

## 8. Config Screen — Two-Column Layout

### Before
Weights card and Scale card stacked vertically always.

### After
```
Viewport ≥ 860 dp:   [ Weights card ] [ Scale card ]   (side by side)
Viewport < 860 dp:   [ Weights card ]
                     [ Scale card   ]                   (stacked)
```

Action buttons are right-aligned on wide viewports (web form convention) and
full-width on compact (touch convention).

**Files changed:** `lib/ui/screens/config_screen.dart`

**Why:** Stacking two medium-height cards vertically on a wide browser leaves
most of the screen empty. Side-by-side use makes the form feel like a web
dashboard settings page rather than a scrollable mobile list.

---

## 9. Stats Screen — Two-Column Layout

### Before
Pass-rate ring, stat tiles, dist chart, and range breakdown always stacked.

### After
```
Viewport ≥ 860 dp:   [ Pass Rate ring + stat tiles ]   [ Dist chart ]
                     [ Score range breakdown            (full width) ]
Viewport < 860 dp:   All cards stacked vertically
```

**Files changed:** `lib/ui/screens/stats_screen.dart`

**Why:** Same rationale as Config Screen — uses horizontal space effectively
on wide browser windows.

---

## 10. GradeDistChart — Adaptive Bar Height

### Before
Fixed `SizedBox(height: 52)` for chart bars.

### After
```dart
LayoutBuilder(builder: (_, constraints) {
  final barH = (constraints.maxWidth * 0.10).clamp(52.0, 96.0);
  return SizedBox(height: barH, child: ...);
})
```

Bars also get a `Tooltip(message: '$grade: $count')` for mouse hover info.

**Files changed:** `lib/ui/components/components.dart`

**Why:** 52 dp is appropriate for a 360 dp wide phone but looks flat and
miniature in a 960 dp wide content column. Proportional scaling makes the
chart fill its space more naturally across viewport sizes.

---

## 11. ScoreBar — Flexible Width

### Before
Fixed `SizedBox(width: 44)` bar — cannot adapt to container width.

### After
```dart
LayoutBuilder(builder: (_, constraints) {
  final barW = constraints.maxWidth.clamp(28.0, 64.0);
  return SizedBox(width: barW, ...);
})
```

**Files changed:** `lib/ui/components/components.dart`

**Why:** Fixed-width bars caused `RenderFlex` overflow errors when the student
row's available width shrank below ≈480 dp. Flexible bars are safe at any width.

---

## 12. EmptyImportBanner — Adaptive Copy

### Before
Always: "Tap to upload .xlsx or .xls"

### After
```dart
final isWide = MediaQuery.sizeOf(context).width >= 600;
Text(
  isWide
    ? 'Drag & drop an .xlsx file here or click to browse'
    : 'Tap to upload .xlsx or .xls',
)
```

**Files changed:** `lib/ui/components/components.dart`

**Why:** "Tap" implies a touch device. Desktop browser users expect "click"
and benefit from the drag-and-drop hint (even if actual drag-and-drop is
handled by `file_picker` behind the scenes on web).

---

## 13. SnackBar Behaviour — Floating

### Before
`showSnackBar(SnackBar(content: Text(...)))` — bottom-fixed, may overlap nav bar.

### After
```dart
showSnackBar(SnackBar(
  content: Text(...),
  behavior: SnackBarBehavior.floating,
))
```

Also added `hideCurrentSnackBar()` before showing a new one to prevent
stacking.

**Files changed:** `lib/ui/screens/grades_screen.dart`,
`lib/ui/screens/config_screen.dart`

**Why:** Fixed snackbars render behind the bottom `NavigationBar` on compact
layouts. Floating snackbars appear above all chrome and are the correct
behaviour for web.

---

## 14. Platform Storage — Web-Aware

No UI changes — the `PlatformStorage` interface already separates web
(`SharedPreferences`) from desktop (`sqflite`). The init path is guarded:

```dart
if (!kIsWeb) { _initDesktopStorage(); }
await PlatformStorage.instance.init();
```

**Files:** `lib/platform/platform_storage.dart` (unchanged from previous iteration)

---

## 15. File I/O — Web Bytes vs Desktop Path

No UI changes in this iteration — `ExcelParser` receives `PlatformFileResult`
(bytes on web, path on desktop) and `ExcelExporter` returns `Uint8List`.
The `platform_file_io.dart` conditional import dispatches to the correct
implementation automatically.

---

## Breakpoint Summary

| Constant | Value | Usage |
|---|---|---|
| `Breakpoints.compact` | 600 dp | Switch bottom-nav ↔ rail; show/hide score bars |
| `Breakpoints.expanded` | 1200 dp | Switch icon-only rail ↔ labelled rail |
| `Breakpoints.maxContentWidth` | 960 dp | Centre-clamp content column |
| `_kConfigSidePanel` | 860 dp | Config two-column threshold |
| `_kStatsSidePanel` | 860 dp | Stats two-column threshold |

---

## Files Changed Summary

| File | Changes |
|---|---|
| `lib/main.dart` | Three-tier nav, _CentredContent, _WebScrollBehavior, text scaler, _Sidebar, _RailItem with MouseRegion |
| `lib/ui/components/components.dart` | MouseRegion on banners, ScoreBar flexible width, GradeDistChart adaptive height + tooltip, EmptyImportBanner adaptive copy |
| `lib/ui/screens/grades_screen.dart` | Responsive student row, dialog-vs-sheet, floating snackbar, isWide passed to rows, ClampingScrollPhysics |
| `lib/ui/screens/config_screen.dart` | Two-column layout, right-aligned buttons on wide, floating snackbar, ConstrainedBox input width |
| `lib/ui/screens/stats_screen.dart` | Two-column layout, ClampingScrollPhysics, max-width cap |
