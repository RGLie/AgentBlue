import 'package:flutter/material.dart';

class AppColors {
  AppColors._();

  // Brand - Lavender/Purple (synced with AgentBlue Android)
  static const agentBlue = Color(0xFFB39DDB);
  static const agentBlueDark = Color(0xFF9575CD);
  static const agentBlueLight = Color(0xFFF3E5F5);

  // Status Text Colors
  static const statusRunning = Color(0xFF00897B);
  static const statusIdle = Color(0xFF546E7A);
  static const statusFailed = Color(0xFFD81B60);
  static const statusCancelled = Color(0xFFF57C00);
  static const statusCompleted = Color(0xFF5E35B1);

  // Status Background Colors (light mode originals)
  static const statusRunningBg = Color(0xFFE0F2F1);
  static const statusIdleBg = Color(0xFFECEFF1);
  static const statusFailedBg = Color(0xFFFCE4EC);
  static const statusCancelledBg = Color(0xFFFFF3E0);
  static const statusCompletedBg = Color(0xFFF3E5F5);

  // Step Colors
  static const stepSuccess = Color(0xFF00897B);
  static const stepFailed = Color(0xFFD81B60);
  static const stepError = Color(0xFFF57C00);

  // Surfaces & Backgrounds
  static const surfaceCard = Color(0xFFF8F9FA);
  static const surfaceCardDark = Color(0xFF1E1E2E);
  static const background = Color(0xFF16181D);
  static const border = Color(0xFF2C3340);
  static const textMuted = Color(0xFF8C96A8);
  static const textSecondary = Color(0xFF7A869C);

  static Color statusColor(String status) {
    switch (status.toUpperCase()) {
      case 'RUNNING':
      case 'PROCESSING':
        return statusRunning;
      case 'COMPLETED':
        return statusCompleted;
      case 'FAILED':
        return statusFailed;
      case 'CANCELLED':
        return statusCancelled;
      case 'PENDING':
        return const Color(0xFFFFCA28);
      default:
        return statusIdle;
    }
  }

  static Color statusBgColor(String status) {
    switch (status.toUpperCase()) {
      case 'RUNNING':
      case 'PROCESSING':
        return statusRunning.withOpacity(0.15);
      case 'COMPLETED':
        return statusCompleted.withOpacity(0.15);
      case 'FAILED':
        return statusFailed.withOpacity(0.15);
      case 'CANCELLED':
        return statusCancelled.withOpacity(0.15);
      case 'PENDING':
        return const Color(0xFFFFCA28).withOpacity(0.15);
      default:
        return statusIdle.withOpacity(0.15);
    }
  }
}
