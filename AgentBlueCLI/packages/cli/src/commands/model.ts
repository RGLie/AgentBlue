import chalk from 'chalk';
import { select, password } from '@inquirer/prompts';
import { writeSettings } from '../firebase/command.js';
import { initFirebase } from '../firebase/client.js';
import { getConfig } from '../config.js';
import { t } from '../i18n.js';

interface ProviderDef {
  name: string;
  value: string;
  models: Array<{ id: string; displayName: string }>;
}

const PROVIDERS: ProviderDef[] = [
  {
    name: 'OpenAI',
    value: 'OPENAI',
    models: [
      { id: 'gpt-5-mini', displayName: 'GPT-5 Mini' },
      { id: 'gpt-5-nano', displayName: 'GPT-5 Nano' },
      { id: 'gpt-4.1-mini', displayName: 'GPT-4.1 Mini' },
      { id: 'gpt-4o-mini', displayName: 'GPT-4o Mini' },
      { id: 'gpt-4o', displayName: 'GPT-4o' },
      { id: 'o3-mini', displayName: 'o3-mini' },
    ],
  },
  {
    name: 'Google Gemini',
    value: 'GEMINI',
    models: [
      { id: 'gemini-2.5-flash-preview-04-17', displayName: 'Gemini 2.5 Flash' },
      { id: 'gemini-2.0-flash', displayName: 'Gemini 2.0 Flash' },
      { id: 'gemini-2.0-flash-lite', displayName: 'Gemini 2.0 Flash Lite' },
      { id: 'gemini-1.5-flash', displayName: 'Gemini 1.5 Flash' },
      { id: 'gemini-1.5-pro', displayName: 'Gemini 1.5 Pro' },
    ],
  },
  {
    name: 'Anthropic Claude',
    value: 'CLAUDE',
    models: [
      { id: 'claude-sonnet-4-20250514', displayName: 'Claude Sonnet 4' },
      { id: 'claude-3-5-sonnet-20241022', displayName: 'Claude 3.5 Sonnet' },
      { id: 'claude-3-5-haiku-20241022', displayName: 'Claude 3.5 Haiku' },
    ],
  },
  {
    name: 'DeepSeek',
    value: 'DEEPSEEK',
    models: [
      { id: 'deepseek-chat', displayName: 'DeepSeek V3' },
      { id: 'deepseek-reasoner', displayName: 'DeepSeek R1' },
    ],
  },
];

export async function modelCommand(sessionIdOverride?: string): Promise<void> {
  const config = getConfig();
  const sessionId = sessionIdOverride ?? config.sessionId;

  if (!sessionId) {
    console.error(chalk.red(t('model_no_session')));
    process.exit(1);
  }

  console.log(chalk.blue.bold(`\n${t('model_title')}\n`));

  const providerValue = await select({
    message: t('model_provider_prompt'),
    choices: PROVIDERS.map((p) => ({ value: p.value, name: p.name })),
  });

  const provider = PROVIDERS.find((p) => p.value === providerValue)!;

  const modelId = await select({
    message: t('model_model_prompt'),
    choices: provider.models.map((m) => ({ value: m.id, name: m.displayName })),
  });

  const apiKey = await password({
    message: `${t('model_apikey_prompt')} ${chalk.dim(t('model_apikey_note'))}`,
    mask: '*',
  });

  // Firebase init if called as standalone command (not from REPL)
  if (!sessionIdOverride) {
    await initFirebase();
  }

  const settings: Record<string, unknown> = {
    provider: providerValue,
    model: modelId,
  };
  if (apiKey.trim()) {
    settings['apiKey'] = apiKey.trim();
  }

  await writeSettings(sessionId, settings);
  console.log(chalk.green(t('model_saved')));
}
