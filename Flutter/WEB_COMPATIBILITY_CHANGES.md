# GradeCalc — Kotlin → Flutter/Dart Translation Challenges

This document records every significant architectural, framework, and
functional challenge encountered during the translation of all 21 Kotlin
source files to functionally equivalent Dart/Flutter desktop code.

---

## 1. Dependency Injection: Hilt → Manual Provider

**Kotlin original:** Hilt (`@HiltAndroidApp`, `@HiltViewModel`,
`@AndroidEntryPoint`, `@Module @InstallIn(SingletonComponent)`, `@Provides
@Singleton`, `@Inject constructor`).  
**Dart translation:** `provider` package (`MultiProvider`,
`ChangeNotifierProvider`, `Provider.of<T>(context)`).

**Challenge:** Hilt performs compile-time code generation and wires the
entire DI graph automatically. Flutter/Dart has no equivalent compile-time
DI framework with the same ergonomics. `get_it` (service locator) or
`riverpod` (reactive DI) are the common alternatives; `provider` was chosen
for its close conceptual mapping to the ViewModel pattern and zero
code-generation requirement.

**Resolution:** All singletons (`GradeDatabase`, `ConfigRepository`,
`GradeRepository`, `ExcelParser`, `ExcelExporter`) are created eagerly in
`main()` and threaded through `MultiProvider`. ViewModels are
`ChangeNotifierProvider`, eliminating `@HiltViewModel` entirely.

---

## 2. Reactive State: Kotlin Flow / StateFlow → Dart Streams + ChangeNotifier

**Kotlin original:**
- `MutableStateFlow<UiState>` with `.update {}` lambdas
- `StateFlow.collectAsStateWithLifecycle()` in Composables
- Room DAOs returning `Flow<List<Student>>`
- `combine()`, `flatMapLatest()`, `debounce(250)` operators

**Dart translation:**
- `ChangeNotifier` with a `_state` field + `notifyListeners()`
- `Consumer<ViewModel>` wrapping widgets
- `StreamController.broadcast()` for repository→ViewModel notifications
- Manual `Timer(250ms)` for debounce
- `StreamSubscription.listen()` replacing `launchIn(viewModelScope)`

**Challenge:** Kotlin coroutine Flows are *cold*, lazily-collected, and
compose via operators. Dart `Stream`s are typically hot (broadcast) or
single-subscription. The `combine()` operator merging search-query debounce
with the student list had no direct single-line Dart equivalent. The
`flatMapLatest` (switch-map) on the active-session stream to the student
stream was also non-trivial.

**Resolution:** The repository exposes `StreamController.broadcast()` streams
that emit on every write. The ViewModel subscribes to the session stream;
when a session arrives it cancels any existing student subscription and
starts a new one (manually implementing `flatMapLatest`). In-memory filtering
with a 250ms `Timer` replaces `debounce`.

---

## 3. Database Layer: Room + DAOs → sqflite + Raw SQL

**Kotlin original:** Room `@Database`, `@Dao`, `@Query`, `@Insert`, `@Update`,
`@Entity`, `@PrimaryKey(autoGenerate = true)`.  
**Dart translation:** `sqflite` + `sqflite_common_ffi` (desktop), raw SQL in
`grade_database.dart`.

**Challenge:**
- Room generates all boilerplate at compile time. Dart requires handwritten
  `toMap()` / `fromMap()` serialisation on every model class.
- Room DAOs return reactive `Flow<T>`. `sqflite` returns `Future<T>` only —
  no built-in reactivity.
- Room operates on Android's SQLite via the Android framework. On desktop,
  `sqflite_common_ffi` must be explicitly initialised **before** any database
  call (`sqfliteFfiInit()` + `databaseFactory = databaseFactoryFfi`).
- Boolean columns: SQLite has no native boolean; Room serialises them
  automatically. Dart must manually map `bool ↔ int (0/1)`.

**Resolution:** `GradeDatabase` is a hand-rolled singleton wrapping a
`sqflite` `Database`. The reactivity gap is bridged by `StreamController`s in
`GradeRepository` that are triggered manually after each mutating operation.

---

## 4. Preferences: DataStore\<Preferences\> → SharedPreferences

**Kotlin original:** `DataStore<Preferences>` with `store.data.map {}` Flow,
`store.edit {}` coroutine.  
**Dart translation:** `shared_preferences` (`SharedPreferences.getInstance()`).

**Challenge:** DataStore is reactive by design — the Flow emits on every
`edit`. `SharedPreferences` is pull-based. Reactivity must be reinstated
manually.

**Resolution:** `ConfigRepository` maintains two `StreamController.broadcast()`
streams (`weightConfig`, `gradeScale`). Every `save*` method writes to
SharedPreferences *and* pushes to the relevant stream. An `init()` async
method (called in `main()`) pre-seeds both streams from stored values.

---

## 5. File I/O: Android SAF Uri → Desktop File Path

**Kotlin original:**
- `ActivityResultContracts.OpenDocument()` + `ContentResolver.openInputStream(uri)`
- `FileProvider.getUriForFile()` + `Intent.ACTION_SEND` for export share

**Dart translation:**
- `file_picker` package — `FilePicker.platform.pickFiles()` opens a native
  OS file chooser dialog, returns a file system path.
- Export: `FilePicker.platform.saveFile()` opens a native save-file dialog.

**Challenge:** Android's Storage Access Framework is permission-based and
returns content URIs, not file paths. Desktop platforms use traditional file
paths. The entire Uri pipeline (`ContentResolver`, `OpenableColumns`,
`FileProvider`) had no meaning on desktop.

**Resolution:** `ExcelParser.parse()` accepts a `String filePath` instead of
`android.net.Uri`. `ExcelExporter.export()` writes to a temp path via
`getTemporaryDirectory()`, then the ViewModel's `confirmExport()` calls
`FilePicker.platform.saveFile()` to let the user choose a permanent
destination.

---

## 6. Excel I/O: Apache POI → excel Package

**Kotlin original:** Apache POI `XSSFWorkbook`, `WorkbookFactory`, `Sheet`,
`Row`, `Cell`, `CellType`, `CellStyle`, `Font`, `IndexedColors`,
`FillPatternType`.  
**Dart translation:** `excel` package (v4).

**Challenge:**
- POI's `CellType` enum (`NUMERIC`, `STRING`, `BLANK`, etc.) maps to excel's
  `Data` subclasses (`IntCellValue`, `DoubleCellValue`, `TextCellValue`,
  `null`).
- POI's indexed colour constants (`IndexedColors.DARK_BLUE`,
  `IndexedColors.ROSE`, `IndexedColors.SEA_GREEN`) map to `HexColor` strings
  in the excel package.
- POI's `sheet.autoSizeColumn(i)` has no equivalent in the excel package —
  column widths are not auto-sized in the Dart output.
- POI's `setAutoFilter` has no equivalent.
- Cell style inheritance (applying the same `failStyle()` to multiple cells in
  a row) works identically in both libraries.

**Resolution:** All POI-specific APIs were substituted with the closest excel
package equivalent. Auto-filter and auto-size are silently omitted (functional
parity maintained; cosmetic difference only).

---

## 7. Jetpack Compose UI → Flutter Widget Tree

This is the highest-effort translation area. Mapping notes:

| Compose concept | Flutter equivalent |
|---|---|
| `@Composable fun` | `StatelessWidget.build()` or `Widget` method |
| `Modifier.weight(1f)` | `Expanded(flex: 1)` wrapper |
| `Modifier.fillMaxWidth()` | `SizedBox(width: double.infinity)` / `Expanded` |
| `Modifier.clip(RoundedCornerShape)` | `ClipRRect(borderRadius:…)` |
| `Modifier.background(color)` | `Container(color:…)` |
| `Modifier.border(1.dp, color, shape)` | `Container(decoration: BoxDecoration(border:…))` |
| `Modifier.padding(n.dp)` | `Padding(padding: EdgeInsets.all(n))` |
| `Modifier.size(w, h)` | `SizedBox(width: w, height: h)` |
| `Modifier.drawBehind { drawLine(…) }` | `Container` + `PreferredSize` divider |
| `Arrangement.spacedBy(n.dp)` | `SizedBox(width/height: n)` between children |
| `LazyColumn + items(key=)` | `ListView.builder` / `Column` with mapped children |
| `ModalBottomSheet` | `showModalBottomSheet<T>()` |
| `SnackbarHost + LaunchedEffect` | `ScaffoldMessenger.of(ctx).showSnackBar` in `addPostFrameCallback` |
| `AlertDialog` | `showDialog<void>(builder: (_) => AlertDialog(…))` |
| `OutlinedTextField` | `TextField` with `InputDecoration(border: OutlineInputBorder)` |
| `Slider(steps=19)` | `Slider(divisions: 20)` |
| `Switch` | `Switch` (identical API) |
| `CircularProgressIndicator(progress={f})` | `CircularProgressIndicator(value: f)` |
| `LinearProgressIndicator(progress={f})` | `LinearProgressIndicator(value: f)` |
| `HorizontalDivider` | `Divider(height:1, thickness:1)` |
| `Surface(shape, color)` | `Container(decoration: BoxDecoration(…))` |
| `Card(onClick=)` | `GestureDetector(onTap:) + Card` |
| `FontFamily.Monospace` | `fontFamily: 'monospace'` |
| `NavigationBar + NavigationBarItem` | `NavigationBar + NavigationDestination` |
| `TopAppBar + TopAppBarDefaults` | `AppBar` in `ThemeData.appBarTheme` |

---

## 8. Navigation: NavHost / NavController → IndexedStack

**Kotlin original:** Jetpack Navigation Compose with `NavHost`, `composable()`,
`navController.navigate(route) { popUpTo … saveState … restoreState }`.  
**Dart translation:** `IndexedStack` with `_selectedIndex` integer.

**Challenge:** Desktop apps do not typically have a back-stack for top-level
tabs. `IndexedStack` is the idiomatic Flutter equivalent — all three screens
are instantiated once and remain in memory, mirroring `saveState = true` /
`restoreState = true` exactly (scroll positions, text field content, etc. are
preserved when switching tabs). A full `Navigator` 2.0 / `go_router` setup
would add unnecessary complexity for three flat tabs.

---

## 9. ViewModel Lifecycle: viewModelScope → Manual Subscriptions

**Kotlin original:** `viewModelScope.launch {}` — coroutines automatically
cancelled when the ViewModel is cleared.  
**Dart translation:** `StreamSubscription` + `Timer` fields manually cancelled
in `dispose()`.

**Challenge:** Flutter `ChangeNotifier.dispose()` is called when the provider
is removed from the tree. All stream subscriptions and timers must be
explicitly cancelled to avoid memory leaks.

**Resolution:** Both `GradesViewModel` and `ConfigViewModel` override
`dispose()` and cancel all `StreamSubscription` and `Timer` references.

---

## 10. Android-only APIs with No Desktop Equivalent

The following Android APIs were removed or replaced:

| Android API | Disposition |
|---|---|
| `android.net.Uri` | Replaced with `String` file path |
| `ContentResolver.openInputStream(uri)` | Replaced with `dart:io File.readAsBytes()` |
| `OpenableColumns.DISPLAY_NAME` | Replaced with `FilePickerResult.files.first.name` |
| `FileProvider.getUriForFile()` | Removed — desktop uses direct file path |
| `Intent.ACTION_SEND` | Replaced with `FilePicker.platform.saveFile()` |
| `FLAG_GRANT_READ_URI_PERMISSION` | Not applicable on desktop |
| `Activity.window.statusBarColor` | Not applicable on desktop |
| `Activity.window.navigationBarColor` | Not applicable on desktop |
| `WindowCompat.getInsetsController` | Not applicable on desktop |
| `CoreSplashScreen.installSplashScreen()` | Not applicable on desktop |
| `enableEdgeToEdge()` | Not applicable on desktop |
| `SplashScreen` | Not applicable on desktop |

---

## 11. Sealed Class Mapping (ImportState / ExportState)

**Kotlin:** `sealed class` with `object`, `data class` subtypes.  
**Dart 3:** `sealed class` keyword (Dart 3.0+) is supported, enabling
exhaustive `switch` expressions.

**Challenge:** Dart's sealed hierarchy requires all subtypes in the same
library file. The Kotlin pattern of `is ImportState.Parsing` becomes
`imp is ImportParsing` in Dart. The default constant sentinels
(`_IdleImport`, `_IdleExport`) were needed to satisfy Dart's non-nullable
`const` constructor for `GradesUiState`.

---

## 12. DecimalFormat("#.#") → Dart String Formatting

**Kotlin:** `DecimalFormat("#.#").format(value)` — omits trailing `.0`.  
**Dart:** `value.toStringAsFixed(1)` always emits one decimal place.  
**Resolution:** A local `_fmt(double v)` helper strips `.0` when present,
matching Kotlin's `#.#` pattern exactly.

---

## 13. Desktop Window Minimum Size

On desktop, Flutter windows can be resized to arbitrarily small dimensions,
breaking the fixed-width layout (especially the student row with
`Modifier.weight()` columns). The Kotlin app ran only on phones with fixed
screen densities.

**Recommendation:** Add `windowManager.setMinimumSize(Size(800, 600))` using
the `window_manager` package (not included to keep dependencies minimal, but
trivial to add).

---

## Summary: Dependency Mapping

| Kotlin / Android library | Dart / Flutter replacement |
|---|---|
| Hilt 2.52 | provider ^6.1.2 |
| Room 2.6.1 | sqflite ^2.3.3 + sqflite_common_ffi ^2.3.3 |
| DataStore Preferences 1.1.1 | shared_preferences ^2.3.2 |
| Apache POI 5.2.5 | excel ^4.0.3 |
| ActivityResultContracts (SAF) | file_picker ^8.1.2 |
| Jetpack Navigation Compose | IndexedStack (no extra package) |
| Kotlinx Coroutines + Flow | Dart async/await + Stream + Timer |
| Material 3 (Compose) | Flutter Material 3 (useMaterial3: true) |
| viewModelScope | ChangeNotifier + dispose() |
