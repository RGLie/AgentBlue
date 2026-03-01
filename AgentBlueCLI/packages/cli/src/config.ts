import { homedir } from 'os';
import { join } from 'path';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'fs';

export interface FirebaseConfig {
  apiKey: string;
  authDomain: string;
  projectId: string;
  storageBucket: string;
  messagingSenderId: string;
  appId: string;
}

export interface AgentBlueConfig {
  firebase: FirebaseConfig;
  language?: 'en' | 'ko';
  sessionId?: string;
  sessionCode?: string;
}

// 공유 Firebase 프로젝트 기본값 (agentblue-d83e5)
// 자체 Firebase를 사용하려면 `agentblue init --custom-firebase` 실행
export const DEFAULT_FIREBASE_CONFIG: FirebaseConfig = {
  apiKey: 'AIzaSyDwkFstODaKooMpZCSVzNMFSXWuVkVjrzk',
  authDomain: 'agentblue-d83e5.firebaseapp.com',
  projectId: 'agentblue-d83e5',
  storageBucket: 'agentblue-d83e5.firebasestorage.app',
  messagingSenderId: '1057609791463',
  appId: '1:1057609791463:web:18d77c59f14336f6ee6f42',
};

const CONFIG_DIR = join(homedir(), '.agentblue');
const CONFIG_FILE = join(CONFIG_DIR, 'config.json');

export function getConfig(): AgentBlueConfig {
  if (!existsSync(CONFIG_FILE)) {
    return { firebase: DEFAULT_FIREBASE_CONFIG };
  }
  try {
    return JSON.parse(readFileSync(CONFIG_FILE, 'utf-8')) as AgentBlueConfig;
  } catch {
    return { firebase: DEFAULT_FIREBASE_CONFIG };
  }
}

export function saveConfig(config: AgentBlueConfig): void {
  if (!existsSync(CONFIG_DIR)) {
    mkdirSync(CONFIG_DIR, { recursive: true });
  }
  writeFileSync(CONFIG_FILE, JSON.stringify(config, null, 2));
}

export function updateConfig(partial: Partial<AgentBlueConfig>): void {
  const current = getConfig();
  saveConfig({ ...current, ...partial });
}

export function getConfigDir(): string {
  return CONFIG_DIR;
}
