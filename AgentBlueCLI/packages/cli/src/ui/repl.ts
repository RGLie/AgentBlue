import readline from 'readline';
import chalk from 'chalk';
import {
  sendCommand,
  listenAgentState,
  listenCommandResult,
  requestCancel,
  type AgentState,
} from '../firebase/command.js';
import { renderAgentState } from './status.js';
import { t } from '../i18n.js';
import { settingCommand } from '../commands/setting.js';
import { modelCommand } from '../commands/model.js';

const PROMPT = chalk.blue('> ');

const SLASH_COMMANDS = ['/stop', '/setting', '/model', '/help'];

export async function startRepl(sessionId: string): Promise<void> {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: PROMPT,
    terminal: true,
  });

  console.log(chalk.dim(t('repl_hint')));
  console.log(chalk.dim(t('repl_slash_help')));
  rl.prompt();

  rl.on('line', async (line) => {
    const input = line.trim();

    if (!input) {
      rl.prompt();
      return;
    }

    if (input === 'exit' || input === 'quit') {
      console.log(chalk.dim(t('repl_exit')));
      rl.close();
      process.exit(0);
    }

    // Slash commands
    if (input.startsWith('/')) {
      rl.pause();
      try {
        await handleSlashCommand(input, sessionId);
      } catch (err) {
        console.error(chalk.red(`Error: ${err}`));
      }
      rl.resume();
      rl.prompt();
      return;
    }

    rl.pause();
    try {
      await executeCommandInteractive(sessionId, input);
    } catch (err) {
      console.error(chalk.red(`Error: ${err}`));
    }
    rl.resume();
    rl.prompt();
  });

  rl.on('SIGINT', () => {
    console.log(chalk.dim(t('repl_exit')));
    process.exit(0);
  });

  rl.on('close', () => {
    process.exit(0);
  });

  await new Promise<void>(() => {
    // keep alive until rl closes
  });
}

async function handleSlashCommand(input: string, sessionId: string): Promise<void> {
  console.log();
  switch (input) {
    case '/stop':
      await requestCancel(sessionId);
      console.log(chalk.yellow(t('stop_requested')));
      break;

    case '/setting':
      await settingCommand(sessionId);
      break;

    case '/model':
      await modelCommand(sessionId);
      break;

    case '/help':
      console.log(chalk.dim(t('repl_slash_help')));
      break;

    default:
      console.log(chalk.red(`Unknown slash command: ${input}`));
      console.log(chalk.dim(`Available: ${SLASH_COMMANDS.join('  ')}`));
  }
  console.log();
}

async function executeCommandInteractive(sessionId: string, command: string): Promise<void> {
  console.log();

  const commandId = await sendCommand(sessionId, command);

  let prevStepCount = 0;
  let resolved = false;

  return new Promise<void>((resolve) => {
    const unsubState = listenAgentState(sessionId, (state: AgentState) => {
      if (resolved) return;

      const { lines, newStepCount } = renderAgentState(state, prevStepCount);
      prevStepCount = newStepCount;

      for (const line of lines) {
        process.stdout.write(line + '\n');
      }
    });

    const unsubResult = listenCommandResult(sessionId, commandId, (status, result) => {
      if (resolved) return;
      resolved = true;

      unsubState();
      unsubResult();

      const msg = status === 'completed' ? t('cmd_completed') : t('cmd_failed');
      console.log(chalk.bold(msg) + (result ? ` ${chalk.dim(result)}` : ''));
      console.log();
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
}
