import chalk from 'chalk';
import { input, confirm } from '@inquirer/prompts';
import { updateConfig } from '../config.js';
import { t } from '../i18n.js';

export async function attachCommand(integration: string): Promise<void> {
  switch (integration.toLowerCase()) {
    case 'telegram':
      await attachTelegram();
      break;
    case 'discord':
      await attachDiscord();
      break;
    default:
      console.error(chalk.red(`${t('attach_unknown')} ${integration}`));
      console.log(chalk.dim(t('attach_available')));
      process.exit(1);
  }
}

async function attachTelegram(): Promise<void> {
  console.log(chalk.blue.bold(`\n${t('attach_tg_title')}\n`));
  console.log(chalk.dim(t('attach_tg_step1')));
  console.log(chalk.dim(t('attach_tg_step2') + '\n'));

  const botToken = await input({
    message: t('attach_tg_token'),
    validate: (val) => val.includes(':') ? true : t('attach_tg_token_invalid'),
  });

  const addAllowList = await confirm({
    message: t('attach_tg_allowlist_prompt'),
    default: true,
  });

  let allowedChatIds: number[] | undefined;

  if (addAllowList) {
    console.log(chalk.dim(t('attach_tg_allowlist_hint')));
    const chatIdInput = await input({ message: t('attach_tg_allowlist_input') });
    allowedChatIds = chatIdInput
      .split(',')
      .map((s) => parseInt(s.trim(), 10))
      .filter(Boolean);
  }

  updateConfig({ telegram: { botToken, allowedChatIds } });

  console.log(chalk.green(t('attach_tg_saved')));
  console.log(chalk.dim(t('attach_tg_next')));
  console.log(chalk.dim(t('attach_tg_commands_title')));
  console.log(chalk.dim(t('attach_tg_cmd_run')));
  console.log(chalk.dim(t('attach_tg_cmd_status')));
  console.log(chalk.dim(t('attach_tg_cmd_stop')));
  console.log(chalk.dim(t('attach_tg_cmd_session') + '\n'));
}

async function attachDiscord(): Promise<void> {
  console.log(chalk.blue.bold(`\n${t('attach_dc_title')}\n`));
  console.log(chalk.dim(t('attach_dc_step1')));
  console.log(chalk.dim(t('attach_dc_step2')));
  console.log(chalk.dim(t('attach_dc_step3') + '\n'));

  const botToken = await input({
    message: t('attach_dc_token'),
    validate: (val) => val.length > 20 ? true : t('attach_dc_token_invalid'),
  });

  const guildId = await input({
    message: t('attach_dc_guild'),
    validate: (val) => /^\d+$/.test(val) ? true : t('attach_dc_id_invalid'),
  });

  const channelId = await input({
    message: t('attach_dc_channel'),
    validate: (val) => /^\d+$/.test(val) ? true : t('attach_dc_id_invalid'),
  });

  updateConfig({ discord: { botToken, guildId, channelId } });

  console.log(chalk.green(t('attach_dc_saved')));
  console.log(chalk.dim(t('attach_dc_next')));
  console.log(chalk.dim(t('attach_dc_commands_title')));
  console.log(chalk.dim(t('attach_dc_cmd_run')));
  console.log(chalk.dim(t('attach_dc_cmd_status') + '\n'));
}
