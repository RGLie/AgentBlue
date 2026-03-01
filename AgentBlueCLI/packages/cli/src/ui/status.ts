import chalk, { type ChalkInstance } from 'chalk';
import type { AgentState, StepLog } from '../firebase/command.js';

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
    const progress = `Step ${state.currentStep}/${state.maxSteps}`;
    lines.push(chalk.blue(`⠸ Processing... ${chalk.dim(progress)}`));

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
    lines.push(color.bold('✓ 완료!'));
  } else if (state.status === 'FAILED') {
    lines.push(color.bold('✗ 실패'));
  } else if (state.status === 'CANCELLED') {
    lines.push(color.bold('⊘ 취소됨'));
  }

  return { lines, newStepCount: state.liveSteps.length };
}

export function printDivider(): void {
  console.log(chalk.dim('─'.repeat(44)));
}

export function printHeader(code: string, connected: boolean): void {
  console.log(chalk.blue.bold('\nAgentBlue v2.0.0'));
  printDivider();
  console.log(`Session Code: ${chalk.yellow.bold(code)}`);
  if (connected) {
    console.log(`Status: ${chalk.green('● Connected')}`);
  } else {
    console.log(`Status: ${chalk.gray('○ Waiting for device...')}`);
  }
  printDivider();
  console.log();
}
