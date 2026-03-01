import chalk, { type ChalkInstance } from 'chalk';
import type { AgentState, StepLog } from '../firebase/command.js';
import { t } from '../i18n.js';

const ACTION_ICON: Record<string, string> = {
  CLICK: '👆',
  TYPE: '⌨️',
  SCROLL: '📜',
  BACK: '◀',
  HOME: '🏠',
  DONE: '✅',
};

function actionIcon(type: string): string {
  return ACTION_ICON[type] ?? '•';
}

function statusColor(status: string): ChalkInstance {
  switch (status) {
    case 'RUNNING': return chalk.blue;
    case 'COMPLETED': return chalk.green;
    case 'FAILED': return chalk.red;
    case 'CANCELLED': return chalk.yellow;
    default: return chalk.gray;
  }
}

export function renderStepLog(step: StepLog): string {
  const icon = actionIcon(step.actionType);
  const target = step.targetText ? chalk.dim(`"${step.targetText}"`) : '';
  const result = step.success ? chalk.green('SUCCESS') : chalk.red('FAILED');
  return `  ${icon} [${step.actionType}] ${target} → ${result}`;
}

export function renderAgentState(state: AgentState, prevStepCount: number): {
  lines: string[];
  newStepCount: number;
} {
  const lines: string[] = [];
  const color = statusColor(state.status);

  if (state.status === 'RUNNING') {
    const progress = `${t('status_step')} ${state.currentStep}/${state.maxSteps}`;
    lines.push(chalk.blue(`${t('status_processing')} ${chalk.dim(progress)}`));

    if (state.currentReasoning) {
      const truncated = state.currentReasoning.length > 80
        ? state.currentReasoning.slice(0, 77) + '...'
        : state.currentReasoning;
      lines.push(chalk.dim(`  → ${truncated}`));
    }

    const newSteps = state.liveSteps.slice(prevStepCount);
    for (const step of newSteps) {
      lines.push(renderStepLog(step));
    }
  } else if (state.status === 'COMPLETED') {
    lines.push(color.bold(t('status_done')));
  } else if (state.status === 'FAILED') {
    lines.push(color.bold(t('status_failed')));
  } else if (state.status === 'CANCELLED') {
    lines.push(color.bold(t('status_cancelled')));
  }

  return { lines, newStepCount: state.liveSteps.length };
}

export function printDivider(): void {
  console.log(chalk.dim('─'.repeat(44)));
}

export function printHeader(code: string, connected: boolean): void {
  console.log(chalk.blue.bold('\nAgentBlue v2.0.0'));
  printDivider();
  console.log(`${t('session_label')} ${chalk.yellow.bold(code)}`);
  if (connected) {
    console.log(`Status: ${chalk.green(t('session_status_connected'))}`);
  } else {
    console.log(`Status: ${chalk.gray(t('session_status_waiting'))}`);
  }
  printDivider();
  console.log();
}
