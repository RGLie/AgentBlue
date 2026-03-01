import {
  Client,
  GatewayIntentBits,
  REST,
  Routes,
  SlashCommandBuilder,
  EmbedBuilder,
  type ChatInputCommandInteraction,
  type Message,
} from 'discord.js';
import chalk from 'chalk';
import { sendCommand, listenAgentState, listenCommandResult, type AgentState } from '../firebase/command.js';
import type { DiscordConfig } from '../config.js';

let client: Client | null = null;

const commands = [
  new SlashCommandBuilder()
    .setName('run')
    .setDescription('Android 기기에 명령을 전송합니다.')
    .addStringOption((opt) =>
      opt.setName('command').setDescription('실행할 명령어').setRequired(true),
    ),
  new SlashCommandBuilder()
    .setName('status')
    .setDescription('현재 에이전트 실행 상태를 확인합니다.'),
  new SlashCommandBuilder()
    .setName('session')
    .setDescription('현재 세션 정보를 확인합니다.'),
];

export async function startDiscordIntegration(
  sessionId: string,
  config: DiscordConfig,
): Promise<void> {
  const rest = new REST().setToken(config.botToken);
  await rest.put(Routes.applicationGuildCommands('me', config.guildId), {
    body: commands.map((c) => c.toJSON()),
  });

  client = new Client({ intents: [GatewayIntentBits.Guilds] });

  client.on('ready', () => {
    console.log(chalk.dim(`Discord 봇 준비됨: ${client!.user?.tag}`));
  });

  client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    if (interaction.channelId !== config.channelId) return;

    const { commandName } = interaction;

    if (commandName === 'run') {
      const command = interaction.options.getString('command', true);
      await handleRunCommand(interaction, sessionId, command);
    } else if (commandName === 'status') {
      await handleStatusCommand(interaction, sessionId);
    } else if (commandName === 'session') {
      await interaction.reply({
        content: `현재 세션 ID: \`${sessionId}\``,
        ephemeral: true,
      });
    }
  });

  await client.login(config.botToken);
}

async function handleRunCommand(
  interaction: ChatInputCommandInteraction,
  sessionId: string,
  command: string,
): Promise<void> {
  const embed = new EmbedBuilder()
    .setColor(0x3b82f6)
    .setTitle('⏳ 처리 중...')
    .setDescription(`명령: ${command}`)
    .setTimestamp();

  await interaction.reply({ embeds: [embed] });

  let prevStepCount = 0;
  let resolved = false;
  const commandId = await sendCommand(sessionId, command);

  const unsubState = listenAgentState(sessionId, async (state: AgentState) => {
    if (resolved || state.status !== 'RUNNING') return;

    const newSteps = state.liveSteps.slice(prevStepCount);
    prevStepCount = state.liveSteps.length;

    if (newSteps.length === 0) return;

    const stepText = newSteps
      .map((s) => `${s.success ? '✅' : '❌'} \`[${s.actionType}]\` ${s.targetText ? `"${s.targetText}"` : ''}`)
      .join('\n');

    const updatedEmbed = new EmbedBuilder()
      .setColor(0x3b82f6)
      .setTitle(`⏳ 처리 중... (Step ${state.currentStep}/${state.maxSteps})`)
      .setDescription(`명령: ${command}\n\n${stepText}`)
      .setTimestamp();

    await interaction.editReply({ embeds: [updatedEmbed] }).catch(() => {});
  });

  const unsubResult = listenCommandResult(sessionId, commandId, async (status, result) => {
    if (resolved) return;
    resolved = true;
    unsubState();
    unsubResult();

    const finalEmbed = new EmbedBuilder()
      .setColor(status === 'completed' ? 0x22c55e : 0xef4444)
      .setTitle(status === 'completed' ? '✅ 완료!' : '❌ 실패')
      .setDescription(`명령: ${command}${result ? `\n\n결과: ${result}` : ''}`)
      .setTimestamp();

    await interaction.editReply({ embeds: [finalEmbed] }).catch(() => {});
  });

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

  const unsubState = listenAgentState(sessionId, async (state: AgentState) => {
    unsubState();
    const embed = new EmbedBuilder()
      .setColor(COLOR_MAP[state.status] ?? 0x94a3b8)
      .setTitle(`에이전트 상태: ${state.status}`)
      .setTimestamp();

    if (state.currentCommand) {
      embed.addFields({ name: '최근 명령', value: state.currentCommand });
    }
    if (state.status === 'RUNNING') {
      embed.addFields({ name: '진행', value: `Step ${state.currentStep}/${state.maxSteps}` });
    }

    await interaction.editReply({ embeds: [embed] }).catch(() => {});
  });
}

export function stopDiscordIntegration(): void {
  client?.destroy();
  client = null;
}
