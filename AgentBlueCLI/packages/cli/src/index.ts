#!/usr/bin/env node
import { Command } from 'commander';
import { initCommand } from './commands/init.js';
import { startCommand } from './commands/start.js';
import { sendCommand } from './commands/send.js';
import { attachCommand } from './commands/attach.js';
import { settingCommand } from './commands/setting.js';
import { modelCommand } from './commands/model.js';

const program = new Command();

program
  .name('agentblue')
  .description('AgentBlue CLI — control your Android device from the terminal')
  .version('2.0.0');

program
  .command('init')
  .description('First-time setup (Firebase backend + language)')
  .action(initCommand);

program
  .command('start')
  .description('Start an interactive session with your Android device')
  .option('-s, --session <code>', 'Resume using an existing session code')
  .action(startCommand);

program
  .command('send <command>')
  .description('Send a single command to your Android device (non-interactive)')
  .action(sendCommand);

program
  .command('attach <integration>')
  .description('Configure a messaging integration: telegram | discord')
  .action(attachCommand);

program
  .command('setting')
  .description('Change agent behavior settings on your paired Android device')
  .action(settingCommand);

program
  .command('model')
  .description('Change AI model settings on your paired Android device')
  .action(modelCommand);

program.parse();
