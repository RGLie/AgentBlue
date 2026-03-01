import chalk from 'chalk';
import ora from 'ora';
import { initFirebase } from '../firebase/client.js';
import {
  createSession,
  listenSessionStatus,
  disconnectSession,
} from '../firebase/session.js';
import { updateConfig } from '../config.js';
import { printHeader } from '../ui/status.js';
import { startRepl } from '../ui/repl.js';
import { t } from '../i18n.js';

interface StartOptions {
  session?: string;
}

export async function startCommand(options: StartOptions): Promise<void> {
  const spinner = ora(t('firebase_connecting')).start();

  try {
    await initFirebase();
    spinner.succeed(t('firebase_connected'));
  } catch (err) {
    spinner.fail(`${t('firebase_failed')}: ${err}`);
    console.log(chalk.dim(t('firebase_hint')));
    process.exit(1);
  }

  const sessionSpinner = ora(t('session_creating')).start();
  let sessionId: string;
  let code: string;

  try {
    const session = await createSession();
    sessionId = session.sessionId;
    code = session.code;
    sessionSpinner.succeed(`${t('session_created')}: ${chalk.yellow.bold(code)}`);
  } catch (err) {
    sessionSpinner.fail(`${t('session_failed')}: ${err}`);
    process.exit(1);
  }

  updateConfig({ sessionId, sessionCode: code });

  printHeader(code, false);
  console.log(chalk.dim(t('session_prompt_android') + '\n'));

  const waitSpinner = ora(t('session_waiting')).start();

  await new Promise<void>((resolve) => {
    const unsub = listenSessionStatus(
      sessionId,
      (_androidUid) => {
        waitSpinner.succeed(chalk.green(t('session_connected')));
        unsub();
        resolve();
      },
      () => {
        waitSpinner.fail(t('session_disconnected'));
        process.exit(0);
      },
    );
  });

  printHeader(code, true);

  process.on('SIGINT', async () => {
    console.log(chalk.dim(`\n\n${t('repl_exit')}`));
    await disconnectSession(sessionId);
    process.exit(0);
  });

  process.on('SIGTERM', async () => {
    await disconnectSession(sessionId);
    process.exit(0);
  });

  await startRepl(sessionId);
}
