import chalk from 'chalk';
import { select, number } from '@inquirer/prompts';
import { writeSettings } from '../firebase/command.js';
import { initFirebase } from '../firebase/client.js';
import { getConfig } from '../config.js';
import { t } from '../i18n.js';

const BROWSER_OPTIONS = ['Chrome', 'Samsung Internet', 'Firefox', 'Default Browser'];
const LANGUAGE_OPTIONS = ['English', '한국어'];

export async function settingCommand(sessionIdOverride?: string): Promise<void> {
  const config = getConfig();
  const sessionId = sessionIdOverride ?? config.sessionId;

  if (!sessionId) {
    console.error(chalk.red(t('setting_no_session')));
    process.exit(1);
  }

  console.log(chalk.blue.bold(`\n${t('setting_title')}\n`));

  const maxSteps = await number({
    message: t('setting_max_steps_prompt'),
    default: 15,
    min: 5,
    max: 30,
  });

  const stepDelayMs = await number({
    message: t('setting_delay_prompt'),
    default: 1500,
    min: 500,
    max: 3000,
  });

  const defaultBrowser = await select({
    message: t('setting_browser_prompt'),
    choices: BROWSER_OPTIONS.map((b) => ({ value: b, name: b })),
  });

  const language = await select({
    message: t('setting_lang_prompt'),
    choices: LANGUAGE_OPTIONS.map((l) => ({ value: l, name: l })),
  });

  // Firebase init if called as standalone command (not from REPL)
  if (!sessionIdOverride) {
    await initFirebase();
  }

  await writeSettings(sessionId, {
    maxSteps: maxSteps ?? 15,
    stepDelayMs: stepDelayMs ?? 1500,
    defaultBrowser,
    language,
  });

  console.log(chalk.green(t('setting_saved')));
}
