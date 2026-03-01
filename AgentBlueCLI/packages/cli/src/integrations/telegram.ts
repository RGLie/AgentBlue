import TelegramBot from 'node-telegram-bot-api';
import chalk from 'chalk';
import { sendCommand, listenAgentState, listenCommandResult, type AgentState } from '../firebase/command.js';
import type { TelegramConfig } from '../config.js';

let bot: TelegramBot | null = null;

export async function startTelegramIntegration(
  sessionId: string,
  config: TelegramConfig,
): Promise<void> {
  bot = new TelegramBot(config.botToken, { polling: true });

  bot.on('message', async (msg) => {
    const chatId = msg.chat.id;
    const text = msg.text ?? '';

    if (config.allowedChatIds && config.allowedChatIds.length > 0) {
      if (!config.allowedChatIds.includes(chatId)) {
        await bot!.sendMessage(chatId, '❌ 접근이 허용되지 않은 계정입니다.');
        return;
      }
    }

    if (text.startsWith('/run ') || text.startsWith('/r ')) {
      const command = text.replace(/^\/(run|r)\s+/, '').trim();
      if (!command) {
        await bot!.sendMessage(chatId, '사용법: /run <명령어>\n예시: /run YouTube에서 BTS 검색해줘');
        return;
      }
      await handleRunCommand(chatId, sessionId, command);
    } else if (text === '/status') {
      await handleStatusCommand(chatId, sessionId);
    } else if (text === '/stop') {
      await bot!.sendMessage(chatId, '⚠️ 중지 기능은 Android 앱의 중지 버튼을 사용해 주세요.');
    } else if (text === '/session') {
      await bot!.sendMessage(chatId, `현재 세션 ID: \`${sessionId}\``, { parse_mode: 'Markdown' });
    } else if (text === '/start' || text === '/help') {
      await bot!.sendMessage(
        chatId,
        '🤖 *AgentBlue* — Android 자동화 에이전트\n\n' +
        '사용 가능한 명령어:\n' +
        '`/run <명령>` — Android 기기에 명령 전송\n' +
        '`/status` — 현재 실행 상태 확인\n' +
        '`/session` — 세션 정보 확인\n' +
        '`/help` — 도움말',
        { parse_mode: 'Markdown' },
      );
    }
  });

  console.log(chalk.dim('Telegram 봇 폴링 시작됨'));
}

async function handleRunCommand(chatId: number, sessionId: string, command: string): Promise<void> {
  const statusMsg = await bot!.sendMessage(chatId, `⏳ 처리 중...\n명령: ${command}`);
  const statusMsgId = statusMsg.message_id;

  let prevStepCount = 0;
  let lastStatusText = '';
  let resolved = false;

  const commandId = await sendCommand(sessionId, command);

  const unsubState = listenAgentState(sessionId, async (state: AgentState) => {
    if (resolved) return;
    if (state.status !== 'RUNNING') return;

    const newSteps = state.liveSteps.slice(prevStepCount);
    prevStepCount = state.liveSteps.length;

    const progress = `Step ${state.currentStep}/${state.maxSteps}`;
    const stepLines = newSteps
      .map((s) => `${s.success ? '✅' : '❌'} [${s.actionType}]${s.targetText ? ` "${s.targetText}"` : ''}`)
      .join('\n');

    const newText = `⏳ *처리 중...* (${progress})\n명령: ${command}${stepLines ? '\n\n' + stepLines : ''}`;

    if (newText !== lastStatusText) {
      lastStatusText = newText;
      try {
        await bot!.editMessageText(newText, {
          chat_id: chatId,
          message_id: statusMsgId,
          parse_mode: 'Markdown',
        });
      } catch {
        // 메시지 변경 실패 무시 (너무 빠른 업데이트)
      }
    }
  });

  const unsubResult = listenCommandResult(sessionId, commandId, async (status, result) => {
    if (resolved) return;
    resolved = true;
    unsubState();
    unsubResult();

    const finalText = status === 'completed'
      ? `✅ *완료!*\n명령: ${command}${result ? '\n\n결과: ' + result : ''}`
      : `❌ *실패*\n명령: ${command}${result ? '\n\n사유: ' + result : ''}`;

    try {
      await bot!.editMessageText(finalText, {
        chat_id: chatId,
        message_id: statusMsgId,
        parse_mode: 'Markdown',
      });
    } catch {
      await bot!.sendMessage(chatId, finalText, { parse_mode: 'Markdown' });
    }
  });

  // 5분 타임아웃
  setTimeout(() => {
    if (!resolved) {
      resolved = true;
      unsubState();
      unsubResult();
      bot!.editMessageText(`⏱ 타임아웃: 응답 없음\n명령: ${command}`, {
        chat_id: chatId,
        message_id: statusMsgId,
      }).catch(() => {});
    }
  }, 5 * 60 * 1000);
}

async function handleStatusCommand(chatId: number, sessionId: string): Promise<void> {
  const unsubState = listenAgentState(sessionId, async (state: AgentState) => {
    unsubState();
    const statusEmoji: Record<string, string> = {
      IDLE: '💤',
      RUNNING: '⚙️',
      COMPLETED: '✅',
      FAILED: '❌',
      CANCELLED: '⊘',
    };
    const emoji = statusEmoji[state.status] ?? '•';
    let text = `${emoji} 상태: *${state.status}*`;
    if (state.currentCommand) {
      text += `\n마지막 명령: ${state.currentCommand}`;
    }
    if (state.status === 'RUNNING') {
      text += `\n진행: Step ${state.currentStep}/${state.maxSteps}`;
    }
    await bot!.sendMessage(chatId, text, { parse_mode: 'Markdown' });
  });
}

export function stopTelegramIntegration(): void {
  bot?.stopPolling();
  bot = null;
}
