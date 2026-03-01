import chalk from 'chalk';
import { select, input, confirm } from '@inquirer/prompts';
import { saveConfig, getConfig, DEFAULT_FIREBASE_CONFIG, type FirebaseConfig } from '../config.js';

export async function initCommand(): Promise<void> {
  console.log(chalk.blue.bold('\nAgentBlue 초기 설정\n'));

  const mode = await select({
    message: 'Firebase 설정 방식을 선택하세요:',
    choices: [
      {
        value: 'default',
        name: '기본 공유 서버 사용 (추천) — 별도 설정 불필요',
        description: '개발자가 운영하는 Firebase 프로젝트를 공유합니다. 즉시 사용 가능합니다.',
      },
      {
        value: 'custom',
        name: '내 Firebase 프로젝트 사용 (고급) — 완전한 셀프호스팅',
        description: 'Firebase 콘솔에서 프로젝트를 직접 생성하고 자격 증명을 입력합니다.',
      },
    ],
  });

  let firebaseConfig: FirebaseConfig = DEFAULT_FIREBASE_CONFIG;

  if (mode === 'custom') {
    console.log(
      chalk.dim(
        '\nFirebase 콘솔(https://console.firebase.google.com)에서\n' +
        '프로젝트 설정 > 일반 > 내 앱 > 구성 값을 확인하세요.\n',
      ),
    );

    firebaseConfig = {
      apiKey: await input({ message: 'API Key:' }),
      authDomain: await input({ message: 'Auth Domain (예: myproject.firebaseapp.com):' }),
      projectId: await input({ message: 'Project ID:' }),
      storageBucket: await input({ message: 'Storage Bucket (예: myproject.firebasestorage.app):' }),
      messagingSenderId: await input({ message: 'Messaging Sender ID:' }),
      appId: await input({ message: 'App ID:' }),
    };
  }

  const current = getConfig();
  saveConfig({ ...current, firebase: firebaseConfig });

  console.log(chalk.green('\n✓ 설정이 저장되었습니다. (~/.agentblue/config.json)'));
  console.log(chalk.dim("'agentblue start'를 실행하여 Android 기기와 연결하세요.\n"));
}
