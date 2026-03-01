#!/usr/bin/env node
/**
 * AgentBlue Discord Daemon
 * 독립 실행형 Discord 봇 — CLI 없이 서버에서 단독으로 운영할 때 사용합니다.
 *
 * 환경 변수:
 *   AGENTBLUE_BOT_TOKEN          — Discord Bot Token
 *   AGENTBLUE_SESSION_ID         — Firestore 세션 ID
 *   AGENTBLUE_GUILD_ID           — Discord 서버 ID
 *   AGENTBLUE_CHANNEL_ID         — 명령을 수신할 채널 ID
 *   AGENTBLUE_CLIENT_ID          — Discord 애플리케이션 Client ID (슬래시 커맨드 등록용)
 *   AGENTBLUE_FIREBASE_API_KEY   — Firebase API Key (선택: 기본값은 공유 서버)
 */

import { initializeApp } from 'firebase/app';
import { getAuth, signInAnonymously } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import {
  Client,
  GatewayIntentBits,
  REST,
  Routes,
  SlashCommandBuilder,
} from 'discord.js';
import chalk from 'chalk';
import { handleDiscordInteraction } from './handler.js';

const required = (key: string): string => {
  const val = process.env[key];
  if (!val) {
    console.error(chalk.red(`오류: 환경 변수 ${key}가 필요합니다.`));
    process.exit(1);
  }
  return val;
};

const BOT_TOKEN = required('AGENTBLUE_BOT_TOKEN');
const SESSION_ID = required('AGENTBLUE_SESSION_ID');
const GUILD_ID = required('AGENTBLUE_GUILD_ID');
const CHANNEL_ID = required('AGENTBLUE_CHANNEL_ID');
const CLIENT_ID = required('AGENTBLUE_CLIENT_ID');

const firebaseConfig = {
  apiKey: process.env['AGENTBLUE_FIREBASE_API_KEY'] ?? 'AIzaSyDwkFstODaKooMpZCSVzNMFSXWuVkVjrzk',
  authDomain: process.env['AGENTBLUE_FIREBASE_AUTH_DOMAIN'] ?? 'agentblue-d83e5.firebaseapp.com',
  projectId: process.env['AGENTBLUE_FIREBASE_PROJECT_ID'] ?? 'agentblue-d83e5',
  storageBucket: process.env['AGENTBLUE_FIREBASE_BUCKET'] ?? 'agentblue-d83e5.firebasestorage.app',
  messagingSenderId: process.env['AGENTBLUE_FIREBASE_SENDER_ID'] ?? '1057609791463',
  appId: process.env['AGENTBLUE_FIREBASE_APP_ID'] ?? '1:1057609791463:web:18d77c59f14336f6ee6f42',
};

const slashCommands = [
  new SlashCommandBuilder()
    .setName('run')
    .setDescription('Android 기기에 명령을 전송합니다.')
    .addStringOption((opt) =>
      opt.setName('command').setDescription('실행할 명령어').setRequired(true),
    ),
  new SlashCommandBuilder()
    .setName('status')
    .setDescription('현재 에이전트 실행 상태를 확인합니다.'),
];

async function main() {
  const app = initializeApp(firebaseConfig);
  const db = getFirestore(app);
  const auth = getAuth(app);

  console.log(chalk.dim('Firebase에 연결 중...'));
  await signInAnonymously(auth);
  console.log(chalk.green('✓ Firebase 연결됨'));

  const rest = new REST().setToken(BOT_TOKEN);
  await rest.put(Routes.applicationGuildCommands(CLIENT_ID, GUILD_ID), {
    body: slashCommands.map((c) => c.toJSON()),
  });
  console.log(chalk.green('✓ Discord 슬래시 커맨드 등록됨'));

  const client = new Client({ intents: [GatewayIntentBits.Guilds] });

  client.on('ready', () => {
    console.log(chalk.green(`✓ Discord 봇 준비됨: ${client.user?.tag} (세션: ${SESSION_ID})`));
    console.log(chalk.dim('Ctrl+C로 종료\n'));
  });

  client.on('interactionCreate', async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    if (interaction.channelId !== CHANNEL_ID) return;
    await handleDiscordInteraction(interaction, db, SESSION_ID);
  });

  await client.login(BOT_TOKEN);

  process.on('SIGINT', () => {
    client.destroy();
    console.log(chalk.dim('\n봇이 종료되었습니다.'));
    process.exit(0);
  });
}

main().catch((err) => {
  console.error(chalk.red('시작 실패:'), err);
  process.exit(1);
});
