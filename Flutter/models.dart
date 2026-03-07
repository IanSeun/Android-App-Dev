// lib/repository/config_repository.dart
// Translated from: com.g.gradeapp.repository.ConfigRepository.kt
//
// TRANSLATION NOTES:
//   • DataStore<Preferences>  →  shared_preferences (SharedPreferences)
//   • Kotlin Flow<WeightConfig>  →  Dart Stream<WeightConfig> backed by
//     a StreamController that re-emits on every write
//   • doublePreferencesKey / stringPreferencesKey  →  prefs.getDouble / getString
//   • store.edit { prefs[key] = value }  →  prefs.setDouble / setString

import 'dart:async';
import 'package:shared_preferences/shared_preferences.dart';

import '../data/model/models.dart';

class ConfigRepository {
  static const _hwKey = 'hw_weight';
  static const _qzKey = 'qz_weight';
  static const _midKey = 'mid_weight';
  static const _finKey = 'fin_weight';
  static const _scaleKey = 'grade_scale';

  // ── Reactive streams (replace Kotlin Flow from DataStore) ─────────────
  final _weightCtrl = StreamController<WeightConfig>.broadcast();
  final _scaleCtrl = StreamController<GradeScale>.broadcast();

  Stream<WeightConfig> get weightConfig => _weightCtrl.stream;
  Stream<GradeScale> get gradeScale => _scaleCtrl.stream;

  // ── Initial load ──────────────────────────────────────────────────────
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _weightCtrl.add(_readWeights(prefs));
    _scaleCtrl.add(_readScale(prefs));
  }

  // ── Read helpers ──────────────────────────────────────────────────────
  WeightConfig _readWeights(SharedPreferences prefs) => WeightConfig(
        homeworkWeight: prefs.getDouble(_hwKey) ?? 0.20,
        quizWeight: prefs.getDouble(_qzKey) ?? 0.10,
        midtermWeight: prefs.getDouble(_midKey) ?? 0.30,
        finalExamWeight: prefs.getDouble(_finKey) ?? 0.40,
      );

  GradeScale _readScale(SharedPreferences prefs) {
    final raw = prefs.getString(_scaleKey);
    if (raw == null || raw.isEmpty) return GradeScale();
    final thresholds = raw.split(',').map((entry) {
      final parts = entry.split(':');
      if (parts.length == 2) {
        final score = double.tryParse(parts[1]);
        if (score != null) return GradeThreshold(parts[0], score);
      }
      return null;
    }).whereType<GradeThreshold>().toList();
    return thresholds.isEmpty ? GradeScale() : GradeScale(thresholds: thresholds);
  }

  // ── Current value getters (used by ViewModel for one-shot reads) ──────
  Future<WeightConfig> getWeightConfig() async {
    final prefs = await SharedPreferences.getInstance();
    return _readWeights(prefs);
  }

  Future<GradeScale> getGradeScale() async {
    final prefs = await SharedPreferences.getInstance();
    return _readScale(prefs);
  }

  // ── Save weight config ─────────────────────────────────────────────────
  Future<void> saveWeightConfig(WeightConfig config) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble(_hwKey, config.homeworkWeight);
    await prefs.setDouble(_qzKey, config.quizWeight);
    await prefs.setDouble(_midKey, config.midtermWeight);
    await prefs.setDouble(_finKey, config.finalExamWeight);
    _weightCtrl.add(config);
  }

  // ── Save grade scale ───────────────────────────────────────────────────
  Future<void> saveGradeScale(GradeScale scale) async {
    final prefs = await SharedPreferences.getInstance();
    final encoded =
        scale.thresholds.map((t) => '${t.letter}:${t.minScore}').join(',');
    await prefs.setString(_scaleKey, encoded);
    _scaleCtrl.add(scale);
  }

  // ── Reset to defaults ──────────────────────────────────────────────────
  Future<void> resetToDefaults() async {
    await saveWeightConfig(const WeightConfig());
    await saveGradeScale(GradeScale(thresholds: defaultGradeScale()));
  }

  void dispose() {
    _weightCtrl.close();
    _scaleCtrl.close();
  }
}
