#!/usr/bin/env node
import { Command } from 'commander';
import { initCommand } from './commands/init.js';
import { startCommand } from './commands/start.js';
import { sendCommand } from './commands/send.js';
import { attachCommand } from './commands/attach.js';

const program = new Command();

program
  .name('agentblue')
  .description('AgentBlue CLI — 터미널에서 Android 기기를 제어하세요')
  .version('2.0.0');

program
  .command('init')
  .description('AgentBlue 초기 설정 (최초 1회 실행)')
  .action(initCommand);

program
  .command('start')
  .description('Android 기기와 대화형 세션 시작')
  .option('-s, --session <code>', '기존 세션 코드로 재연결')
  .action(startCommand);

program
  .command('send <command>')
  .description('Android 기기에 단일 명령 전송 (비대화형)')
  .action(sendCommand);

program
  .command('attach <integration>')
  .description('메시징 통합 설정 (telegram | discord)')
  .action(attachCommand);

program.parse();
