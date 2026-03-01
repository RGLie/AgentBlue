import chalk from 'chalk';
import ora from 'ora';
import { initFirebase } from '../firebase/client.js';
import {
  createSession,
  listenSessionStatus,
  disconnectSession,
} from '../firebase/session.js';
import { updateConfig, getConfig } from '../config.js';
import { printHeader } from '../ui/status.js';
import { startRepl } from '../ui/repl.js';
import { startTelegramIntegration } from '../integrations/telegram.js';
import { startDiscordIntegration } from '../integrations/discord.js';

interface StartOptions {
  session?: string;
}

export async function startCommand(options: StartOptions): Promise<void> {
  const spinner = ora('Firebase에 연결 중...').start();

  let db, user;
  try {
    ({ db, user } = await initFirebase());
    spinner.succeed('Firebase 연결 완료');
  } catch (err) {
    spinner.fail(`Firebase 연결 실패: ${err}`);
    console.log(chalk.dim("'agentblue init'을 먼저 실행해 설정을 확인하세요."));
    process.exit(1);
  }

  const sessionSpinner = ora('세션 생성 중...').start();
  let sessionId: string;
  let code: string;

  try {
    const session = await createSession();
    sessionId = session.sessionId;
    code = session.code;
    sessionSpinner.succeed(`세션 생성됨: ${chalk.yellow.bold(code)}`);
  } catch (err) {
    sessionSpinner.fail(`세션 생성 실패: ${err}`);
    process.exit(1);
  }

  updateConfig({ sessionId, sessionCode: code });

  printHeader(code, false);
  console.log(chalk.dim('Android 앱을 열고 메인 화면의 세션 코드 입력창에 위 코드를 입력하세요.\n'));

  const waitSpinner = ora('기기 연결 대기 중...').start();

  await new Promise<void>((resolve) => {
    const unsub = listenSessionStatus(
      sessionId,
      (_androidUid) => {
        waitSpinner.succeed(chalk.green('기기가 연결되었습니다!'));
        unsub();
        resolve();
      },
      () => {
        waitSpinner.fail('세션이 해제되었습니다.');
        process.exit(0);
      },
    );
  });

  printHeader(code, true);

  const config = getConfig();

  // 설정된 통합 시작
  if (config.telegram?.botToken) {
    try {
      await startTelegramIntegration(sessionId, config.telegram);
      console.log(chalk.dim('✓ Telegram 봇 연결됨'));
    } catch {
      console.log(chalk.yellow('⚠ Telegram 봇 시작 실패 (설정을 확인하세요)'));
    }
  }

  if (config.discord?.botToken) {
    try {
      await startDiscordIntegration(sessionId, config.discord);
      console.log(chalk.dim('✓ Discord 봇 연결됨'));
    } catch {
      console.log(chalk.yellow('⚠ Discord 봇 시작 실패 (설정을 확인하세요)'));
    }
  }

  // 종료 시 세션 해제
  process.on('SIGINT', async () => {
    console.log(chalk.dim('\n\n세션을 종료합니다...'));
    await disconnectSession(sessionId);
    process.exit(0);
  });

  process.on('SIGTERM', async () => {
    await disconnectSession(sessionId);
    process.exit(0);
  });

  // 대화형 REPL 시작
  await startRepl(sessionId);
}
