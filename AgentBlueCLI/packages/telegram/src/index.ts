#!/usr/bin/env node
/**
 * AgentBlue Telegram Daemon
 * 독립 실행형 Telegram 봇 — CLI 없이 서버에서 단독으로 운영할 때 사용합니다.
 *
 * 환경 변수:
 *   AGENTBLUE_BOT_TOKEN   — Telegram Bot Token
 *   AGENTBLUE_SESSION_ID  — Firestore 세션 ID
 *   AGENTBLUE_FIREBASE_API_KEY       — Firebase API Key (선택: 기본값은 공유 서버)
 *   AGENTBLUE_FIREBASE_PROJECT_ID    — Firebase Project ID
 *   AGENTBLUE_FIREBASE_AUTH_DOMAIN   — Firebase Auth Domain
 *   AGENTBLUE_FIREBASE_APP_ID        — Firebase App ID
 *   AGENTBLUE_FIREBASE_SENDER_ID     — Firebase Messaging Sender ID
 *   AGENTBLUE_FIREBASE_BUCKET        — Firebase Storage Bucket
 *   AGENTBLUE_ALLOWED_CHAT_IDS       — 허용할 Chat ID (쉼표 구분, 선택)
 */

import { initializeApp } from 'firebase/app';
import { getAuth, signInAnonymously } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import TelegramBot from 'node-telegram-bot-api';
import chalk from 'chalk';
import { handleTelegramMessage } from './handler.js';

const BOT_TOKEN = process.env['AGENTBLUE_BOT_TOKEN'];
const SESSION_ID = process.env['AGENTBLUE_SESSION_ID'];

if (!BOT_TOKEN || !SESSION_ID) {
  console.error(chalk.red('오류: AGENTBLUE_BOT_TOKEN 및 AGENTBLUE_SESSION_ID 환경 변수가 필요합니다.'));
  console.log(chalk.dim('사용법: AGENTBLUE_BOT_TOKEN=xxx AGENTBLUE_SESSION_ID=yyy agentblue-telegram'));
  process.exit(1);
}

const firebaseConfig = {
  apiKey: process.env['AGENTBLUE_FIREBASE_API_KEY'] ?? 'AIzaSyDwkFstODaKooMpZCSVzNMFSXWuVkVjrzk',
  authDomain: process.env['AGENTBLUE_FIREBASE_AUTH_DOMAIN'] ?? 'agentblue-d83e5.firebaseapp.com',
  projectId: process.env['AGENTBLUE_FIREBASE_PROJECT_ID'] ?? 'agentblue-d83e5',
  storageBucket: process.env['AGENTBLUE_FIREBASE_BUCKET'] ?? 'agentblue-d83e5.firebasestorage.app',
  messagingSenderId: process.env['AGENTBLUE_FIREBASE_SENDER_ID'] ?? '1057609791463',
  appId: process.env['AGENTBLUE_FIREBASE_APP_ID'] ?? '1:1057609791463:web:18d77c59f14336f6ee6f42',
};

const allowedChatIds = process.env['AGENTBLUE_ALLOWED_CHAT_IDS']
  ? process.env['AGENTBLUE_ALLOWED_CHAT_IDS'].split(',').map(Number).filter(Boolean)
  : [];

async function main() {
  const app = initializeApp(firebaseConfig);
  const db = getFirestore(app);
  const auth = getAuth(app);

  console.log(chalk.dim('Firebase에 연결 중...'));
  await signInAnonymously(auth);
  console.log(chalk.green('✓ Firebase 연결됨'));

  const bot = new TelegramBot(BOT_TOKEN!, { polling: true });
  console.log(chalk.green(`✓ Telegram 봇 시작됨 (세션: ${SESSION_ID})`));
  console.log(chalk.dim('Ctrl+C로 종료\n'));

  bot.on('message', (msg) => handleTelegramMessage(bot, db, SESSION_ID!, msg, allowedChatIds));

  process.on('SIGINT', () => {
    bot.stopPolling();
    console.log(chalk.dim('\n봇이 종료되었습니다.'));
    process.exit(0);
  });
}

main().catch((err) => {
  console.error(chalk.red('시작 실패:'), err);
  process.exit(1);
});
