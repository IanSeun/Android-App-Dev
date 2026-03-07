// lib/ui/navigation/app_navigation.dart
// Translated from: com.g.gradeapp.ui.navigation.NavGraph.kt
//
// TRANSLATION NOTES:
//   • sealed class Dest  →  Dart enum with a route string getter
//   • NavHost / NavHostController / composable()  →  Flutter IndexedStack
//     with an integer selectedIndex driving which screen is visible.
//     This is the idiomatic desktop pattern: tab-switching without a
//     Navigator stack, preserving each screen's scroll/state like
//     saveState = true / restoreState = true did in the Kotlin version.

enum Dest {
  grades('grades'),
  config('config'),
  stats('stats');

  final String route;
  const Dest(this.route);
}
