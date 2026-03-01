import TelegramBot, { type Message } from 'node-telegram-bot-api';
import type { Firestore } from 'firebase/firestore';
import {
  collection,
  addDoc,
  doc,
  onSnapshot,
  serverTimestamp,
} from 'firebase/firestore';

export async function handleTelegramMessage(
  bot: TelegramBot,
  db: Firestore,
  sessionId: string,
  msg: Message,
  allowedChatIds: number[],
): Promise<void> {
  const chatId = msg.chat.id;
  const text = msg.text ?? '';

  if (allowedChatIds.length > 0 && !allowedChatIds.includes(chatId)) {
    await bot.sendMessage(chatId, '❌ 접근이 허용되지 않은 계정입니다.');
    return;
  }

  if (text === '/start' || text === '/help') {
    await bot.sendMessage(
      chatId,
      '🤖 *AgentBlue* — Android 자동화 에이전트\n\n' +
      '`/run <명령>` — 명령 전송\n' +
      '`/status` — 상태 확인\n' +
      '`/help` — 도움말',
      { parse_mode: 'Markdown' },
    );
    return;
  }

  if (text.startsWith('/run ') || text.startsWith('/r ')) {
    const command = text.replace(/^\/(run|r)\s+/, '').trim();
    if (!command) {
      await bot.sendMessage(chatId, '사용법: /run <명령어>');
      return;
    }
    await runCommand(bot, db, sessionId, chatId, command);
    return;
  }

  if (text === '/status') {
    await statusCommand(bot, db, sessionId, chatId);
    return;
  }
}

async function runCommand(
  bot: TelegramBot,
  db: Firestore,
  sessionId: string,
  chatId: number,
  command: string,
): Promise<void> {
  const statusMsg = await bot.sendMessage(chatId, `⏳ 처리 중...\n명령: ${command}`);
  const statusMsgId = statusMsg.message_id;

  const commandRef = await addDoc(collection(db, 'sessions', sessionId, 'commands'), {
    command,
    status: 'pending',
    deviceId: 'telegram-bot',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  const commandId = commandRef.id;

  let prevStepCount = 0;
  let resolved = false;

  const unsubState = onSnapshot(
    doc(db, 'sessions', sessionId, 'agentState', 'current'),
    async (snap) => {
      if (resolved || !snap.exists()) return;
      const state = snap.data();
      if (state?.['status'] !== 'RUNNING') return;

      const steps: Array<{ actionType: string; targetText?: string; success: boolean }> =
        (state?.['liveSteps'] as Array<{ actionType: string; targetText?: string; success: boolean }> ?? []).slice(prevStepCount);
      prevStepCount = state?.['liveSteps']?.length ?? 0;
      if (steps.length === 0) return;

      const stepText = steps
        .map((s) => `${s.success ? '✅' : '❌'} [${s.actionType}]${s.targetText ? ` "${s.targetText}"` : ''}`)
        .join('\n');

      const progress = `Step ${state?.['currentStep']}/${state?.['maxSteps']}`;
      await bot.editMessageText(
        `⏳ *처리 중...* (${progress})\n명령: ${command}\n\n${stepText}`,
        { chat_id: chatId, message_id: statusMsgId, parse_mode: 'Markdown' },
      ).catch(() => {});
    },
  );

  const unsubResult = onSnapshot(
    doc(db, 'sessions', sessionId, 'commands', commandId),
    async (snap) => {
      if (resolved || !snap.exists()) return;
      const data = snap.data();
      const status = data?.['status'];
      if (status !== 'completed' && status !== 'failed') return;

      resolved = true;
      unsubState();
      unsubResult();

      const result = data?.['result'] as string | undefined;
      const finalText = status === 'completed'
        ? `✅ *완료!*\n명령: ${command}${result ? '\n\n결과: ' + result : ''}`
        : `❌ *실패*\n명령: ${command}${result ? '\n\n사유: ' + result : ''}`;

      await bot.editMessageText(finalText, {
        chat_id: chatId,
        message_id: statusMsgId,
        parse_mode: 'Markdown',
      }).catch(() => bot.sendMessage(chatId, finalText, { parse_mode: 'Markdown' }));
    },
  );

  setTimeout(() => {
    if (!resolved) {
      resolved = true;
      unsubState();
      unsubResult();
      bot.editMessageText(`⏱ 타임아웃: 응답 없음\n명령: ${command}`, {
        chat_id: chatId,
        message_id: statusMsgId,
      }).catch(() => {});
    }
  }, 5 * 60 * 1000);
}

async function statusCommand(
  bot: TelegramBot,
  db: Firestore,
  sessionId: string,
  chatId: number,
): Promise<void> {
  const unsub = onSnapshot(doc(db, 'sessions', sessionId, 'agentState', 'current'), async (snap) => {
    unsub();
    if (!snap.exists()) {
      await bot.sendMessage(chatId, '에이전트 상태 정보가 없습니다.');
      return;
    }
    const state = snap.data();
    const status = state?.['status'] as string;
    const emoji: Record<string, string> = {
      IDLE: '💤', RUNNING: '⚙️', COMPLETED: '✅', FAILED: '❌', CANCELLED: '⊘',
    };
    let text = `${emoji[status] ?? '•'} 상태: *${status}*`;
    if (state?.['currentCommand']) text += `\n마지막 명령: ${state['currentCommand']}`;
    if (status === 'RUNNING') text += `\n진행: Step ${state?.['currentStep']}/${state?.['maxSteps']}`;
    await bot.sendMessage(chatId, text, { parse_mode: 'Markdown' });
  });
}
