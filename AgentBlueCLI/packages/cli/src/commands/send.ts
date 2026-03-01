import chalk from 'chalk';
import ora from 'ora';
import { initFirebase } from '../firebase/client.js';
import { checkSessionPaired } from '../firebase/session.js';
import { sendCommand as fbSendCommand, listenAgentState, listenCommandResult, type AgentState } from '../firebase/command.js';
import { getConfig } from '../config.js';
import { renderAgentState } from '../ui/status.js';

export async function sendCommand(command: string): Promise<void> {
  const config = getConfig();

  if (!config.sessionId) {
    console.error(chalk.red('오류: 활성 세션이 없습니다.'));
    console.log(chalk.dim("'agentblue start'를 먼저 실행하여 기기와 연결하세요."));
    process.exit(1);
  }

  const spinner = ora('Firebase에 연결 중...').start();

  try {
    await initFirebase();
    spinner.succeed('연결됨');
  } catch (err) {
    spinner.fail(`연결 실패: ${err}`);
    process.exit(1);
  }

  const sessionId = config.sessionId;
  const isPaired = await checkSessionPaired(sessionId);

  if (!isPaired) {
    console.error(chalk.red('오류: 기기가 연결되어 있지 않습니다.'));
    console.log(chalk.dim("'agentblue start'로 새 세션을 시작하세요."));
    process.exit(1);
  }

  console.log(chalk.dim(`\n세션: ${config.sessionCode}`));
  console.log(`> ${chalk.white(command)}\n`);

  const commandId = await fbSendCommand(sessionId, command);
  let prevStepCount = 0;
  let resolved = false;

  await new Promise<void>((resolve) => {
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
      resolve();
    });

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

  process.exit(0);
}
