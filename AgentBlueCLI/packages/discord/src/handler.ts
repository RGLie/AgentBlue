import type { ChatInputCommandInteraction } from 'discord.js';
import { EmbedBuilder } from 'discord.js';
import type { Firestore } from 'firebase/firestore';
import {
  collection,
  addDoc,
  doc,
  onSnapshot,
  serverTimestamp,
} from 'firebase/firestore';

export async function handleDiscordInteraction(
  interaction: ChatInputCommandInteraction,
  db: Firestore,
  sessionId: string,
): Promise<void> {
  if (interaction.commandName === 'run') {
    const command = interaction.options.getString('command', true);
    await handleRunCommand(interaction, db, sessionId, command);
  } else if (interaction.commandName === 'status') {
    await handleStatusCommand(interaction, db, sessionId);
  }
}

async function handleRunCommand(
  interaction: ChatInputCommandInteraction,
  db: Firestore,
  sessionId: string,
  command: string,
): Promise<void> {
  const embed = new EmbedBuilder()
    .setColor(0x3b82f6)
    .setTitle('⏳ 처리 중...')
    .setDescription(`명령: ${command}`)
    .setTimestamp();

  await interaction.reply({ embeds: [embed] });

  const commandRef = await addDoc(collection(db, 'sessions', sessionId, 'commands'), {
    command,
    status: 'pending',
    deviceId: 'discord-bot',
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

      const allSteps = (state?.['liveSteps'] as Array<{ actionType: string; targetText?: string; success: boolean }> ?? []);
      const newSteps = allSteps.slice(prevStepCount);
      prevStepCount = allSteps.length;
      if (newSteps.length === 0) return;

      const stepText = newSteps
        .map((s) => `${s.success ? '✅' : '❌'} \`[${s.actionType}]\`${s.targetText ? ` "${s.targetText}"` : ''}`)
        .join('\n');

      const updatedEmbed = new EmbedBuilder()
        .setColor(0x3b82f6)
        .setTitle(`⏳ 처리 중... (Step ${state?.['currentStep']}/${state?.['maxSteps']})`)
        .setDescription(`명령: ${command}\n\n${stepText}`)
        .setTimestamp();

      await interaction.editReply({ embeds: [updatedEmbed] }).catch(() => {});
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
      const finalEmbed = new EmbedBuilder()
        .setColor(status === 'completed' ? 0x22c55e : 0xef4444)
        .setTitle(status === 'completed' ? '✅ 완료!' : '❌ 실패')
        .setDescription(`명령: ${command}${result ? `\n\n결과: ${result}` : ''}`)
        .setTimestamp();

      await interaction.editReply({ embeds: [finalEmbed] }).catch(() => {});
    },
  );

  setTimeout(() => {
    if (!resolved) {
      resolved = true;
      unsubState();
      unsubResult();
      const timeoutEmbed = new EmbedBuilder()
        .setColor(0xf59e0b)
        .setTitle('⏱ 타임아웃')
        .setDescription(`명령: ${command}\n\n5분 내 응답 없음`)
        .setTimestamp();
      interaction.editReply({ embeds: [timeoutEmbed] }).catch(() => {});
    }
  }, 5 * 60 * 1000);
}

async function handleStatusCommand(
  interaction: ChatInputCommandInteraction,
  db: Firestore,
  sessionId: string,
): Promise<void> {
  await interaction.deferReply({ ephemeral: true });

  const COLOR_MAP: Record<string, number> = {
    IDLE: 0x94a3b8,
    RUNNING: 0x3b82f6,
    COMPLETED: 0x22c55e,
    FAILED: 0xef4444,
    CANCELLED: 0xf59e0b,
  };

  const unsub = onSnapshot(
    doc(db, 'sessions', sessionId, 'agentState', 'current'),
    async (snap) => {
      unsub();
      if (!snap.exists()) {
        await interaction.editReply({ content: '에이전트 상태 정보가 없습니다.' });
        return;
      }
      const state = snap.data();
      const status = state?.['status'] as string;
      const embed = new EmbedBuilder()
        .setColor(COLOR_MAP[status] ?? 0x94a3b8)
        .setTitle(`에이전트 상태: ${status}`)
        .setTimestamp();

      if (state?.['currentCommand']) {
        embed.addFields({ name: '최근 명령', value: state['currentCommand'] as string });
      }
      if (status === 'RUNNING') {
        embed.addFields({ name: '진행', value: `Step ${state?.['currentStep']}/${state?.['maxSteps']}` });
      }
      await interaction.editReply({ embeds: [embed] }).catch(() => {});
    },
  );
}
