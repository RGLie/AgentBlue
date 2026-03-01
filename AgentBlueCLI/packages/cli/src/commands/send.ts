import chalk from 'chalk';
import ora from 'ora';
import { initFirebase } from '../firebase/client.js';
import { checkSessionPaired } from '../firebase/session.js';
import {
  sendCommand as fbSendCommand,
  listenAgentState,
  listenCommandResult,
  type AgentState,
} from '../firebase/command.js';
import { getConfig } from '../config.js';
import { renderAgentState } from '../ui/status.js';
import { t } from '../i18n.js';

export async function sendCommand(command: string): Promise<void> {
  const config = getConfig();

  if (!config.sessionId) {
    console.error(chalk.red(t('cmd_no_session')));
    console.log(chalk.dim(t('cmd_no_session_hint')));
    process.exit(1);
  }

  const spinner = ora(t('firebase_connecting')).start();

  try {
    await initFirebase();
    spinner.succeed(t('firebase_connected'));
  } catch (err) {
    spinner.fail(`${t('firebase_failed')}: ${err}`);
    process.exit(1);
  }

  const sessionId = config.sessionId;
  const isPaired = await checkSessionPaired(sessionId);

  if (!isPaired) {
    console.error(chalk.red(t('cmd_not_paired')));
    console.log(chalk.dim(t('cmd_not_paired_hint')));
    process.exit(1);
  }

  console.log(chalk.dim(`\n${t('cmd_session_label')} ${config.sessionCode}`));
  console.log(`> ${chalk.white(command)}\n`);

  const commandId = await fbSendCommand(sessionId, command);
  let prevStepCount = 0;
  let resolved = false;

  await new Promise<void>((resolve) => {
    const unsubState = listenAgentState(sessionId, (state: AgentState) => {
      if (resolved) return;
      const { lines, newStepCount } = renderAgentState(state, prevStepCount);
      prevStepCount = newStepCount;
      for (const line of lines) process.stdout.write(line + '\n');
    });

    const unsubResult = listenCommandResult(sessionId, commandId, (status, result) => {
      if (resolved) return;
      resolved = true;
      unsubState();
      unsubResult();

      const msg = status === 'completed' ? t('cmd_completed') : t('cmd_failed');
      console.log(chalk.bold(msg) + (result ? ` ${chalk.dim(result)}` : ''));
      resolve();
    });

    setTimeout(() => {
      if (!resolved) {
        resolved = true;
        unsubState();
        unsubResult();
        console.log(chalk.yellow(t('cmd_timeout')));
        resolve();
      }
    }, 5 * 60 * 1000);
  });

  process.exit(0);
}
