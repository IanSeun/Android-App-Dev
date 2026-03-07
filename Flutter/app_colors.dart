// lib/viewmodel/config_viewmodel.dart
// Translated from: com.g.gradeapp.viewmodel.ConfigViewModel.kt
//
// TRANSLATION NOTES:
//   • @HiltViewModel  →  plain ChangeNotifier, injected by Provider
//   • MutableStateFlow<ConfigUiState>  →  _state field + notifyListeners()
//   • configRepo.weightConfig.onEach { }.launchIn()  →  StreamSubscription.listen()
//   • viewModelScope.launch { }  →  unawaited( Future )

import 'dart:async';
import 'package:flutter/foundation.dart';

import '../data/model/models.dart';
import '../repository/config_repository.dart';

class ConfigUiState {
  final WeightConfig weights;
  final List<GradeThreshold> gradeScale;
  final bool isSaved;
  final String? errorMsg;

  const ConfigUiState({
    this.weights = const WeightConfig(),
    List<GradeThreshold>? gradeScale,
    this.isSaved = false,
    this.errorMsg,
  }) : gradeScale = gradeScale ?? const [];

  ConfigUiState copyWith({
    WeightConfig? weights,
    List<GradeThreshold>? gradeScale,
    bool? isSaved,
    String? Function()? errorMsg,
  }) =>
      ConfigUiState(
        weights: weights ?? this.weights,
        gradeScale: gradeScale ?? this.gradeScale,
        isSaved: isSaved ?? this.isSaved,
        errorMsg: errorMsg != null ? errorMsg() : this.errorMsg,
      );
}

class ConfigViewModel extends ChangeNotifier {
  final ConfigRepository _configRepo;

  ConfigUiState _state = ConfigUiState(gradeScale: defaultGradeScale());
  ConfigUiState get state => _state;

  StreamSubscription<WeightConfig>? _weightSub;
  StreamSubscription<GradeScale>? _scaleSub;

  ConfigViewModel({required ConfigRepository configRepo})
      : _configRepo = configRepo {
    _init();
  }

  void _init() {
    _weightSub = _configRepo.weightConfig.listen((w) {
      _update((s) => s.copyWith(weights: w));
    });
    _scaleSub = _configRepo.gradeScale.listen((scale) {
      _update((s) => s.copyWith(gradeScale: List.from(scale.thresholds)));
    });
  }

  // ── Weight updates ────────────────────────────────────────────────────
  void setHomework(double v) => _update((s) => s.copyWith(
      weights: s.weights.copyWith(homeworkWeight: v)));

  void setQuiz(double v) => _update((s) => s.copyWith(
      weights: s.weights.copyWith(quizWeight: v)));

  void setMidterm(double v) => _update((s) => s.copyWith(
      weights: s.weights.copyWith(midtermWeight: v)));

  void setFinalExam(double v) => _update((s) => s.copyWith(
      weights: s.weights.copyWith(finalExamWeight: v)));

  // ── Grade scale updates ───────────────────────────────────────────────
  void updateThreshold(int index, double newMin) {
    final updated = List<GradeThreshold>.from(_state.gradeScale);
    if (index < updated.length) {
      updated[index] = updated[index].copyWith(minScore: newMin);
      _update((s) => s.copyWith(gradeScale: updated));
    }
  }

  // ── Save ──────────────────────────────────────────────────────────────
  Future<void> save() async {
    if (!_state.weights.isValid) {
      _update((s) => s.copyWith(errorMsg: () => 'Weights must total 100%'));
      return;
    }
    await _configRepo.saveWeightConfig(_state.weights);
    await _configRepo.saveGradeScale(GradeScale(thresholds: _state.gradeScale));
    _update((s) => s.copyWith(isSaved: true, errorMsg: () => null));
  }

  Future<void> resetToDefaults() async {
    await _configRepo.resetToDefaults();
    _update((s) => s.copyWith(isSaved: false, errorMsg: () => null));
  }

  void clearError() => _update((s) => s.copyWith(errorMsg: () => null));
  void clearSaved() => _update((s) => s.copyWith(isSaved: false));

  void _update(ConfigUiState Function(ConfigUiState) updater) {
    _state = updater(_state);
    notifyListeners();
  }

  @override
  void dispose() {
    _weightSub?.cancel();
    _scaleSub?.cancel();
    super.dispose();
  }
}
