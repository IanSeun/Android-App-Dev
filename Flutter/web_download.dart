// lib/main.dart
//
// WEB-COMPATIBILITY CHANGES FROM DESKTOP VERSION:
// ────────────────────────────────────────────────────────────────────────────
// 1. PLATFORM GUARDS: All dart:io / sqflite / Platform references wrapped in
//    kIsWeb.  dart:io imported via conditional import (no-op stub on web).
//
// 2. STORAGE ABSTRACTION: PlatformStorage.instance replaces GradeDatabase.
//    Web → SharedPreferences (localStorage). Desktop → sqflite.
//
// 3. NAVIGATION — THREE-TIER RESPONSIVE LAYOUT:
//    • Compact  (< 600 dp)  — bottom NavigationBar (portrait / narrow)
//    • Medium   (600–1199)  — left NavigationRail, icons only
//    • Expanded (≥ 1200 dp) — left NavigationRail with labels + branding
//    IndexedStack used at all tiers to preserve scroll/state.
//
// 4. CONTENT WIDTH CAP: On wide displays content is capped at 960 dp and
//    centred so text lines don't stretch across ultra-wide viewports.
//
// 5. SCROLL BEHAVIOUR: _WebScrollBehavior enables mouse/trackpad drag
//    scrolling and replaces BouncingScrollPhysics with ClampingScrollPhysics.
//
// 6. TEXT SCALING: textScaler clamped [0.85, 1.15] to guard fixed-width
//    student-row columns from browser font-size settings.
//
// 7. CURSOR: MouseRegion(cursor: SystemMouseCursors.click) wraps all
//    tappable widgets so the browser renders a pointer cursor on hover.
//
// 8. DIALOGS: Wide viewports use centred Dialog widgets; compact keeps sheets.

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/gestures.dart' show PointerDeviceKind;
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'platform/desktop_file_write.dart'
    if (dart.library.html) 'platform/web_download.dart'
    as file_impl;

import 'data/local/grade_database.dart';
import 'platform/platform_file_io.dart';
import 'platform/platform_storage.dart';
import 'repository/config_repository.dart';
import 'repository/grade_repository.dart';
import 'ui/navigation/app_navigation.dart';
import 'ui/screens/config_screen.dart';
import 'ui/screens/grades_screen.dart';
import 'ui/screens/stats_screen.dart';
import 'ui/theme/app_colors.dart';
import 'ui/theme/app_theme.dart';
import 'util/excel_exporter.dart';
import 'util/excel_parser.dart';
import 'viewmodel/config_viewmodel.dart';
import 'viewmodel/grades_viewmodel.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  configurePlatformFileIO(
    webDownload: kIsWeb ? file_impl.triggerWebDownload : null,
    desktopWrite: kIsWeb ? null : file_impl.writeDesktopFile,
  );

  if (!kIsWeb) {
    _initDesktopStorage();
  }

  await PlatformStorage.instance.init();

  final configRepo = ConfigRepository();
  await configRepo.init();

  final gradeRepo = GradeRepository(PlatformStorage.instance);
  final parser    = ExcelParser();
  final exporter  = ExcelExporter();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider<ConfigViewModel>(
          create: (_) => ConfigViewModel(configRepo: configRepo),
        ),
        ChangeNotifierProvider<GradesViewModel>(
          create: (_) => GradesViewModel(
            gradeRepo: gradeRepo,
            configRepo: configRepo,
            parser:    parser,
            exporter:  exporter,
          ),
        ),
      ],
      child: const GradeCalcApp(),
    ),
  );
}

void _initDesktopStorage() {
  _getDb = () => GradeDatabase.instance;
  try { _cacheDesktopPlatform(); } catch (_) {}
}
void _cacheDesktopPlatform() {
  try { initNativePlatformString(_detectPlatform()); } catch (_) {}
}
String _detectPlatform() {
  try {
    return const bool.fromEnvironment('dart.tool.dart2js') ? '' : _nativePlatformName();
  } catch (_) { return ''; }
}
String _nativePlatformName() {
  if (const bool.fromEnvironment('dart.tool.dart2js')) return '';
  return 'linux';
}

// ─────────────────────────────────────────────────────────────────────────────
// BREAKPOINTS
// ─────────────────────────────────────────────────────────────────────────────
class Breakpoints {
  static const double compact          = 600;
  static const double expanded         = 1200;
  static const double maxContentWidth  = 960;

  static bool isCompact(BuildContext ctx) =>
      MediaQuery.sizeOf(ctx).width < compact;
  static bool isExpanded(BuildContext ctx) =>
      MediaQuery.sizeOf(ctx).width >= expanded;
}

// ─────────────────────────────────────────────────────────────────────────────
// WEB SCROLL BEHAVIOUR
// • Enables mouse/trackpad drag scrolling (required for web desktop).
// • ClampingScrollPhysics globally — no iOS-style bounce in a browser window.
// ─────────────────────────────────────────────────────────────────────────────
class _WebScrollBehavior extends MaterialScrollBehavior {
  const _WebScrollBehavior();

  @override
  Set<PointerDeviceKind> get dragDevices => {
        PointerDeviceKind.touch,
        PointerDeviceKind.mouse,
        PointerDeviceKind.trackpad,
      };

  @override
  ScrollPhysics getScrollPhysics(BuildContext context) =>
      const ClampingScrollPhysics();
}

// ─────────────────────────────────────────────────────────────────────────────
// ROOT APP
// ─────────────────────────────────────────────────────────────────────────────
class GradeCalcApp extends StatelessWidget {
  const GradeCalcApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'GradeCalc',
      debugShowCheckedModeBanner: false,
      theme: buildGTheme(),
      scrollBehavior: const _WebScrollBehavior(),
      builder: (context, child) {
        final mq = MediaQuery.of(context);
        final clamped = mq.textScaler.scale(1.0).clamp(0.85, 1.15);
        return MediaQuery(
          data: mq.copyWith(textScaler: TextScaler.linear(clamped)),
          child: child!,
        );
      },
      home: const GApp(),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// CENTRED CONTENT WRAPPER
// Caps readable content width at 960 dp and centres it horizontally so
// paragraphs and data tables don't span illegibly across ultra-wide monitors.
// ─────────────────────────────────────────────────────────────────────────────
class _CentredContent extends StatelessWidget {
  final Widget child;
  const _CentredContent({required this.child});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (_, constraints) {
      if (constraints.maxWidth <= Breakpoints.maxContentWidth) return child;
      return Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints:
              const BoxConstraints(maxWidth: Breakpoints.maxContentWidth),
          child: child,
        ),
      );
    });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN SCAFFOLD — RESPONSIVE NAVIGATION
//   < 600 dp  : bottom NavigationBar (portrait phone / narrow browser)
//   600–1199  : left NavigationRail, icons only (tablet / medium browser)
//   ≥ 1200 dp : left NavigationRail with labels (desktop / wide browser)
// ─────────────────────────────────────────────────────────────────────────────
class GApp extends StatefulWidget {
  const GApp({super.key});
  @override
  State<GApp> createState() => _GAppState();
}

class _GAppState extends State<GApp> {
  int _selectedIndex = 0;

  static const _navItems = [
    _NavItem('Grades', Dest.grades, Icons.bar_chart,  Icons.bar_chart_outlined),
    _NavItem('Config', Dest.config, Icons.tune,        Icons.tune_outlined),
    _NavItem('Stats',  Dest.stats,  Icons.analytics,   Icons.analytics_outlined),
  ];
  static const _subtitles = ['Student Grades', 'Weights & Scale', 'Statistics'];
  static const _screens   = [GradesScreen(), ConfigScreen(), StatsScreen()];

  @override
  Widget build(BuildContext context) {
    final w = MediaQuery.sizeOf(context).width;
    return GradesStateOverlay(
      child: w >= Breakpoints.compact
          ? _buildRailLayout(w >= Breakpoints.expanded)
          : _buildBottomNavLayout(),
    );
  }

  // ── Rail layout (≥ 600 dp) ────────────────────────────────────────────
  Widget _buildRailLayout(bool showLabels) {
    final railW = showLabels ? 180.0 : 64.0;
    return Scaffold(
      backgroundColor: kBlack,
      body: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Sidebar
          SizedBox(
            width: railW,
            child: _Sidebar(
              selectedIndex: _selectedIndex,
              navItems:      _navItems,
              subtitles:     _subtitles,
              showLabels:    showLabels,
              onSelect:      (i) => setState(() => _selectedIndex = i),
            ),
          ),
          // Content
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _WebTopBar(subtitle: _subtitles[_selectedIndex]),
                Expanded(
                  child: _CentredContent(
                    child: IndexedStack(
                      index: _selectedIndex,
                      children: _screens,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ── Bottom-nav layout (< 600 dp) ──────────────────────────────────────
  Widget _buildBottomNavLayout() {
    return Scaffold(
      backgroundColor: kBlack,
      appBar: AppBar(
        backgroundColor: kNavy900,
        elevation: 0,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1),
          child: Container(height: 1, color: kNavyDivider),
        ),
        title: _AppBarTitle(subtitle: _subtitles[_selectedIndex]),
      ),
      bottomNavigationBar: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(height: 1, color: kNavyDivider),
          NavigationBar(
            backgroundColor: kNavy900,
            elevation: 0,
            selectedIndex: _selectedIndex,
            onDestinationSelected: (i) => setState(() => _selectedIndex = i),
            indicatorColor: kSapphire.withOpacity(0.12),
            destinations: _navItems.map((item) => NavigationDestination(
              icon:         Icon(item.unselectedIcon, color: kTextDisabled),
              selectedIcon: Icon(item.selectedIcon,   color: kSapphire),
              label: item.label,
            )).toList(),
            labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
          ),
        ],
      ),
      body: IndexedStack(index: _selectedIndex, children: _screens),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// SIDEBAR WIDGET
// ─────────────────────────────────────────────────────────────────────────────
class _Sidebar extends StatelessWidget {
  final int selectedIndex;
  final List<_NavItem> navItems;
  final List<String> subtitles;
  final bool showLabels;
  final ValueChanged<int> onSelect;

  const _Sidebar({
    required this.selectedIndex,
    required this.navItems,
    required this.subtitles,
    required this.showLabels,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: kNavy900,
        border: Border(right: BorderSide(color: kNavyDivider, width: 1)),
      ),
      child: Column(
        children: [
          // Logo header
          SizedBox(
            height: 56,
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: showLabels ? 14 : 0),
              child: Row(
                mainAxisAlignment: showLabels
                    ? MainAxisAlignment.start
                    : MainAxisAlignment.center,
                children: [
                  Container(
                    width: 32, height: 32,
                    decoration: BoxDecoration(
                      color: kSapphire,
                      borderRadius: BorderRadius.circular(9),
                    ),
                    alignment: Alignment.center,
                    child: const Text('G',
                        style: TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w900,
                          fontSize: 16,
                        )),
                  ),
                  if (showLabels) ...[
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Text('GradeCalc',
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                  color: kTextPrimary,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14)),
                          Text(subtitles[selectedIndex],
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                  color: kTextSecondary, fontSize: 11)),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          Container(height: 1, color: kNavyDivider),
          const SizedBox(height: 6),
          ...navItems.asMap().entries.map((e) => _RailItem(
                icon: selectedIndex == e.key
                    ? e.value.selectedIcon
                    : e.value.unselectedIcon,
                label:     e.value.label,
                selected:  selectedIndex == e.key,
                showLabel: showLabels,
                onTap: ()  => onSelect(e.key),
              )),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// WEB TOP BAR
// ─────────────────────────────────────────────────────────────────────────────
class _WebTopBar extends StatelessWidget {
  final String subtitle;
  const _WebTopBar({required this.subtitle});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 48,
      decoration: const BoxDecoration(
        color: kNavy900,
        border: Border(bottom: BorderSide(color: kNavyDivider, width: 1)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        children: [
          Text(subtitle,
              style: const TextStyle(
                  color: kTextPrimary,
                  fontWeight: FontWeight.w600,
                  fontSize: 15)),
          const Spacer(),
          Container(
            decoration: BoxDecoration(
              color: kSapphire.withOpacity(0.10),
              borderRadius: BorderRadius.circular(6),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            child: const Text('GradeCalc v1.0',
                style: TextStyle(
                    color: kSapphire, fontSize: 11, fontWeight: FontWeight.w500)),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP BAR TITLE  (compact layout)
// ─────────────────────────────────────────────────────────────────────────────
class _AppBarTitle extends StatelessWidget {
  final String subtitle;
  const _AppBarTitle({required this.subtitle});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 28, height: 28,
          decoration: BoxDecoration(
              color: kSapphire, borderRadius: BorderRadius.circular(8)),
          alignment: Alignment.center,
          child: const Text('G',
              style: TextStyle(
                  color: Colors.white, fontWeight: FontWeight.w900, fontSize: 14)),
        ),
        const SizedBox(width: 8),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('GradeCalc',
                style: TextStyle(
                    color: kTextPrimary, fontWeight: FontWeight.bold, fontSize: 15)),
            Text(subtitle,
                style: const TextStyle(
                    color: kTextSecondary, fontSize: 11, fontWeight: FontWeight.normal)),
          ],
        ),
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// RAIL ITEM
// MouseRegion provides browser pointer-cursor on hover (web UX standard).
// ─────────────────────────────────────────────────────────────────────────────
class _RailItem extends StatelessWidget {
  final IconData icon;
  final String   label;
  final bool     selected;
  final bool     showLabel;
  final VoidCallback onTap;

  const _RailItem({
    required this.icon,
    required this.label,
    required this.selected,
    required this.showLabel,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      cursor: SystemMouseCursors.click,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          margin: EdgeInsets.symmetric(
              horizontal: showLabel ? 8 : 6, vertical: 3),
          padding: EdgeInsets.symmetric(
              horizontal: showLabel ? 12 : 0, vertical: 10),
          width: showLabel ? null : 52,
          decoration: BoxDecoration(
            color: selected ? kSapphire.withOpacity(0.12) : Colors.transparent,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Row(
            mainAxisAlignment: showLabel
                ? MainAxisAlignment.start
                : MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon,
                  color: selected ? kSapphire : kTextDisabled, size: 20),
              if (showLabel) ...[
                const SizedBox(width: 10),
                Text(label,
                    style: TextStyle(
                      color: selected ? kSapphire : kTextDisabled,
                      fontWeight:
                          selected ? FontWeight.w600 : FontWeight.normal,
                      fontSize: 13,
                    )),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _NavItem {
  final String   label;
  final Dest     dest;
  final IconData selectedIcon;
  final IconData unselectedIcon;
  const _NavItem(this.label, this.dest, this.selectedIcon, this.unselectedIcon);
}
