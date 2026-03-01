import readline from 'readline';
import chalk from 'chalk';
import { sendCommand, listenAgentState, listenCommandResult, type AgentState } from '../firebase/command.js';
import { renderAgentState } from './status.js';

const PROMPT = chalk.blue('> ');

export async function startRepl(sessionId: string): Promise<void> {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: PROMPT,
    terminal: true,
  });

  console.log(chalk.dim("명령을 입력하세요. 종료하려면 'exit' 또는 Ctrl+C\n"));
  rl.prompt();

  rl.on('line', async (line) => {
    const input = line.trim();

    if (!input) {
      rl.prompt();
      return;
    }

    if (input === 'exit' || input === 'quit') {
      console.log(chalk.dim('\n연결을 종료합니다.'));
      rl.close();
      process.exit(0);
    }

    rl.pause();

    try {
      await executeCommandInteractive(sessionId, input);
    } catch (err) {
      console.error(chalk.red(`오류: ${err}`));
    }

    rl.resume();
    rl.prompt();
  });

  rl.on('SIGINT', () => {
    console.log(chalk.dim('\n\n연결을 종료합니다.'));
    process.exit(0);
  });

  rl.on('close', () => {
    process.exit(0);
  });

  await new Promise<void>(() => {
    // rl이 닫힐 때까지 대기
  });
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

      if (status === 'completed') {
        console.log(chalk.green.bold('\n✓ 완료!') + (result ? ` ${chalk.dim(result)}` : ''));
      } else {
        console.log(chalk.red.bold('\n✗ 실패') + (result ? ` ${chalk.dim(result)}` : ''));
      }
      console.log();
      resolve();
    });

    // 타임아웃 — 5분 후 강제 종료
    setTimeout(() => {
      if (!resolved) {
        resolved = true;
        unsubState();
        unsubResult();
        console.log(chalk.yellow('\n⏱ 타임아웃: 응답 없음 (5분)'));
        resolve();
      }
    }, 5 * 60 * 1000);
  });
}
