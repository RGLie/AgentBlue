import chalk from 'chalk';
import { select, input, confirm } from '@inquirer/prompts';
import { saveConfig, getConfig, DEFAULT_FIREBASE_CONFIG, type FirebaseConfig } from '../config.js';
import { t, setLangCache, type Lang } from '../i18n.js';

export async function initCommand(): Promise<void> {
  // Language selection happens first so the rest of the wizard is in the chosen language
  const language = await select<Lang>({
    message: 'Language / 언어',
    choices: [
      { value: 'en', name: 'English' },
      { value: 'ko', name: '한국어 (Korean)' },
    ],
  });
  setLangCache(language);

  console.log(chalk.blue.bold(`\n${t('init_title')}\n`));

  const mode = await select({
    message: t('init_firebase_prompt'),
    choices: [
      {
        value: 'default',
        name: t('init_firebase_shared'),
        description: t('init_firebase_shared_desc'),
      },
      {
        value: 'custom',
        name: t('init_firebase_custom'),
        description: t('init_firebase_custom_desc'),
      },
    ],
  });

  let firebaseConfig: FirebaseConfig = DEFAULT_FIREBASE_CONFIG;

  if (mode === 'custom') {
    console.log(chalk.dim(t('init_firebase_custom_hint')));
    firebaseConfig = {
      apiKey: await input({ message: t('init_firebase_api_key') }),
      authDomain: await input({ message: t('init_firebase_auth_domain') }),
      projectId: await input({ message: t('init_firebase_project_id') }),
      storageBucket: await input({ message: t('init_firebase_storage_bucket') }),
      messagingSenderId: await input({ message: t('init_firebase_sender_id') }),
      appId: await input({ message: t('init_firebase_app_id') }),
    };
  }

  const current = getConfig();
  saveConfig({ ...current, firebase: firebaseConfig, language });

  console.log(chalk.green(t('init_saved')));
  console.log(chalk.dim(t('init_next')));
}
