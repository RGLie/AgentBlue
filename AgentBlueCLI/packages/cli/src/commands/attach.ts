import chalk from 'chalk';
import { input, number, confirm } from '@inquirer/prompts';
import { updateConfig, getConfig } from '../config.js';

export async function attachCommand(integration: string): Promise<void> {
  switch (integration.toLowerCase()) {
    case 'telegram':
      await attachTelegram();
      break;
    case 'discord':
      await attachDiscord();
      break;
    default:
      console.error(chalk.red(`알 수 없는 통합: ${integration}`));
      console.log(chalk.dim('사용 가능한 통합: telegram, discord'));
      process.exit(1);
  }
}

async function attachTelegram(): Promise<void> {
  console.log(chalk.blue.bold('\nTelegram 봇 설정\n'));
  console.log(chalk.dim('1. Telegram에서 @BotFather를 찾아 /newbot 명령으로 봇을 생성하세요.'));
  console.log(chalk.dim('2. BotFather가 제공한 토큰을 아래에 입력하세요.\n'));

  const botToken = await input({
    message: 'Bot Token (예: 7123456789:AAFxx...):',
    validate: (val) => val.includes(':') ? true : '유효하지 않은 토큰 형식입니다.',
  });

  const addAllowList = await confirm({
    message: '특정 채팅 ID만 허용하시겠습니까? (보안 강화 — 미설정 시 누구나 봇 사용 가능)',
    default: true,
  });

  let allowedChatIds: number[] | undefined;

  if (addAllowList) {
    console.log(chalk.dim('\n봇에게 메시지를 보낸 후 https://api.telegram.org/bot{TOKEN}/getUpdates 에서 chat.id를 확인하세요.'));
    const chatIdInput = await input({
      message: '허용할 Chat ID (복수 입력 시 쉼표로 구분, 예: 123456789,987654321):',
    });
    allowedChatIds = chatIdInput.split(',').map((s) => parseInt(s.trim(), 10)).filter(Boolean);
  }

  updateConfig({
    telegram: { botToken, allowedChatIds },
  });

  console.log(chalk.green('\n✓ Telegram 봇 설정이 저장되었습니다.'));
  console.log(chalk.dim("'agentblue start' 실행 시 Telegram 봇이 자동으로 시작됩니다."));
  console.log(chalk.dim('\n봇에서 사용 가능한 명령어:'));
  console.log(chalk.dim('  /run <명령>   — Android 기기에 명령 전송'));
  console.log(chalk.dim('  /status       — 현재 실행 상태 확인'));
  console.log(chalk.dim('  /stop         — 실행 중인 작업 취소 요청'));
  console.log(chalk.dim('  /session      — 세션 코드 확인\n'));
}

async function attachDiscord(): Promise<void> {
  console.log(chalk.blue.bold('\nDiscord 봇 설정\n'));
  console.log(chalk.dim('1. https://discord.com/developers/applications 에서 애플리케이션을 생성하세요.'));
  console.log(chalk.dim('2. Bot 탭에서 토큰을 복사하세요.'));
  console.log(chalk.dim('3. OAuth2 탭에서 bot + applications.commands 권한으로 서버에 초대하세요.\n'));

  const botToken = await input({
    message: 'Bot Token:',
    validate: (val) => val.length > 20 ? true : '유효하지 않은 토큰입니다.',
  });

  const guildId = await input({
    message: 'Server (Guild) ID: (Discord 개발자 모드 활성화 후 서버 우클릭 → ID 복사)',
    validate: (val) => /^\d+$/.test(val) ? true : '숫자만 입력하세요.',
  });

  const channelId = await input({
    message: 'Channel ID: (명령을 수신할 채널 우클릭 → ID 복사)',
    validate: (val) => /^\d+$/.test(val) ? true : '숫자만 입력하세요.',
  });

  updateConfig({
    discord: { botToken, guildId, channelId },
  });

  console.log(chalk.green('\n✓ Discord 봇 설정이 저장되었습니다.'));
  console.log(chalk.dim("'agentblue start' 실행 시 Discord 봇이 자동으로 시작됩니다."));
  console.log(chalk.dim('\n봇에서 사용 가능한 슬래시 커맨드:'));
  console.log(chalk.dim('  /run <명령>   — Android 기기에 명령 전송'));
  console.log(chalk.dim('  /status       — 현재 실행 상태 확인'));
  console.log(chalk.dim('  /stop         — 실행 중인 작업 취소 요청\n'));
}
