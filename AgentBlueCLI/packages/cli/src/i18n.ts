import { getConfig } from './config.js';

export type Lang = 'en' | 'ko';

const translations = {
  en: {
    // init
    init_title: 'AgentBlue Setup',
    init_firebase_prompt: 'Choose your Firebase backend:',
    init_firebase_shared: 'Shared server (recommended) — no setup required',
    init_firebase_shared_desc: 'Use the developer-hosted Firebase project. Ready to use immediately.',
    init_firebase_custom: 'Self-hosted — use your own Firebase project',
    init_firebase_custom_desc: 'Create your own Firebase project for full independence.',
    init_firebase_custom_hint: '\nFind your config in Firebase Console → Project Settings → Your apps → Config.\n',
    init_firebase_api_key: 'API Key:',
    init_firebase_auth_domain: 'Auth Domain (e.g. myproject.firebaseapp.com):',
    init_firebase_project_id: 'Project ID:',
    init_firebase_storage_bucket: 'Storage Bucket (e.g. myproject.firebasestorage.app):',
    init_firebase_sender_id: 'Messaging Sender ID:',
    init_firebase_app_id: 'App ID:',
    init_lang_prompt: 'Preferred language:',
    init_lang_en: 'English',
    init_lang_ko: '한국어 (Korean)',
    init_saved: '\n✓ Config saved to ~/.agentblue/config.json',
    init_next: "Run 'agentblue start' to connect your Android device.\n",

    // firebase
    firebase_connecting: 'Connecting to Firebase...',
    firebase_connected: 'Firebase connected',
    firebase_failed: 'Firebase connection failed',
    firebase_hint: "Run 'agentblue init' to check your configuration.",

    // session
    session_creating: 'Creating session...',
    session_created: 'Session created',
    session_failed: 'Session creation failed',
    session_waiting: 'Waiting for device...',
    session_connected: 'Device connected!',
    session_disconnected: 'Session disconnected.',
    session_prompt_android: 'Open the AgentBlue app and enter this code in the Session field.',
    session_label: 'Session Code:',
    session_status_connected: '● Connected',
    session_status_waiting: '○ Waiting for device...',

    // repl
    repl_hint: "Type a command. 'exit' or Ctrl+C to quit.\n",
    repl_exit: '\nDisconnecting.',

    // command
    cmd_completed: '\n✓ Done!',
    cmd_failed: '\n✗ Failed',
    cmd_timeout: '\n⏱ Timeout: no response (5 min)',
    cmd_no_session: 'Error: no active session.',
    cmd_no_session_hint: "Run 'agentblue start' first to pair a device.",
    cmd_not_paired: 'Error: device is not connected.',
    cmd_not_paired_hint: "Run 'agentblue start' to create a new session.",
    cmd_session_label: 'Session:',

    // stop
    stop_requested: 'Cancel requested. Waiting for device to stop...',
    stop_no_session: 'No active session to cancel.',

    // setting command
    setting_title: 'Agent Settings',
    setting_max_steps_prompt: 'Max steps (5–30):',
    setting_delay_prompt: 'Step delay in ms (500–3000):',
    setting_browser_prompt: 'Default browser:',
    setting_lang_prompt: 'Agent response language:',
    setting_saved: '✓ Settings sent to device.',
    setting_no_session: 'No active session. Run agentblue start first.',

    // model command
    model_title: 'AI Model Settings',
    model_provider_prompt: 'AI Provider:',
    model_model_prompt: 'Model:',
    model_apikey_prompt: 'API Key:',
    model_apikey_note: '(leave blank to keep existing key on device)',
    model_saved: '✓ Model settings sent to device.',
    model_no_session: 'No active session. Run agentblue start first.',

    // repl help
    repl_slash_help: "  /stop      — cancel running task\n  /setting   — change agent settings\n  /model     — change AI model settings\n  exit       — disconnect\n",

    // status display
    status_processing: '⠸ Processing...',
    status_done: '✓ Done!',
    status_failed: '✗ Failed',
    status_cancelled: '⊘ Cancelled',
    status_step: 'Step',
  },

  ko: {
    // init
    init_title: 'AgentBlue 초기 설정',
    init_firebase_prompt: 'Firebase 설정 방식을 선택하세요:',
    init_firebase_shared: '기본 공유 서버 사용 (추천) — 별도 설정 불필요',
    init_firebase_shared_desc: '개발자가 운영하는 Firebase 프로젝트를 공유합니다. 즉시 사용 가능합니다.',
    init_firebase_custom: '내 Firebase 프로젝트 사용 (고급) — 완전한 셀프호스팅',
    init_firebase_custom_desc: '직접 Firebase 프로젝트를 생성하여 완전히 독립적으로 운영합니다.',
    init_firebase_custom_hint: '\nFirebase 콘솔(https://console.firebase.google.com) → 프로젝트 설정 → 일반 → 내 앱 → 구성 에서 확인하세요.\n',
    init_firebase_api_key: 'API Key:',
    init_firebase_auth_domain: 'Auth Domain (예: myproject.firebaseapp.com):',
    init_firebase_project_id: 'Project ID:',
    init_firebase_storage_bucket: 'Storage Bucket (예: myproject.firebasestorage.app):',
    init_firebase_sender_id: 'Messaging Sender ID:',
    init_firebase_app_id: 'App ID:',
    init_lang_prompt: '사용 언어를 선택하세요:',
    init_lang_en: 'English',
    init_lang_ko: '한국어 (Korean)',
    init_saved: '\n✓ 설정이 저장되었습니다. (~/.agentblue/config.json)',
    init_next: "'agentblue start'를 실행하여 Android 기기와 연결하세요.\n",

    // firebase
    firebase_connecting: 'Firebase에 연결 중...',
    firebase_connected: 'Firebase 연결 완료',
    firebase_failed: 'Firebase 연결 실패',
    firebase_hint: "'agentblue init'을 먼저 실행해 설정을 확인하세요.",

    // session
    session_creating: '세션 생성 중...',
    session_created: '세션 생성됨',
    session_failed: '세션 생성 실패',
    session_waiting: '기기 연결 대기 중...',
    session_connected: '기기가 연결되었습니다!',
    session_disconnected: '세션이 해제되었습니다.',
    session_prompt_android: 'Android 앱을 열고 메인 화면의 세션 코드 입력창에 위 코드를 입력하세요.',
    session_label: 'Session Code:',
    session_status_connected: '● 연결됨',
    session_status_waiting: '○ 기기 대기 중...',

    // repl
    repl_hint: "명령을 입력하세요. 'exit' 또는 Ctrl+C로 종료.\n",
    repl_exit: '\n연결을 종료합니다.',

    // command
    cmd_completed: '\n✓ 완료!',
    cmd_failed: '\n✗ 실패',
    cmd_timeout: '\n⏱ 타임아웃: 응답 없음 (5분)',
    cmd_no_session: '오류: 활성 세션이 없습니다.',
    cmd_no_session_hint: "'agentblue start'를 먼저 실행하여 기기와 연결하세요.",
    cmd_not_paired: '오류: 기기가 연결되어 있지 않습니다.',
    cmd_not_paired_hint: "'agentblue start'로 새 세션을 시작하세요.",
    cmd_session_label: '세션:',

    // stop
    stop_requested: '취소 요청을 전송했습니다. 기기가 멈출 때까지 기다려주세요...',
    stop_no_session: '취소할 활성 세션이 없습니다.',

    // setting command
    setting_title: '에이전트 설정',
    setting_max_steps_prompt: '최대 스텝 수 (5–30):',
    setting_delay_prompt: '스텝 딜레이 ms (500–3000):',
    setting_browser_prompt: '기본 브라우저:',
    setting_lang_prompt: '에이전트 응답 언어:',
    setting_saved: '✓ 설정을 기기에 전송했습니다.',
    setting_no_session: '활성 세션이 없습니다. agentblue start를 먼저 실행하세요.',

    // model command
    model_title: 'AI 모델 설정',
    model_provider_prompt: 'AI 프로바이더:',
    model_model_prompt: '모델:',
    model_apikey_prompt: 'API 키:',
    model_apikey_note: '(비워두면 기기의 기존 키를 유지합니다)',
    model_saved: '✓ 모델 설정을 기기에 전송했습니다.',
    model_no_session: '활성 세션이 없습니다. agentblue start를 먼저 실행하세요.',

    // repl help
    repl_slash_help: "  /stop      — 실행 중인 작업 취소\n  /setting   — 에이전트 설정 변경\n  /model     — AI 모델 설정 변경\n  exit       — 세션 종료\n",

    // status display
    status_processing: '⠸ 처리 중...',
    status_done: '✓ 완료!',
    status_failed: '✗ 실패',
    status_cancelled: '⊘ 취소됨',
    status_step: 'Step',
  },
} as const;

type TranslationKey = keyof typeof translations.en;

let _cachedLang: Lang | null = null;

export function getLang(): Lang {
  if (_cachedLang) return _cachedLang;
  try {
    const config = getConfig();
    _cachedLang = config.language ?? 'en';
  } catch {
    _cachedLang = 'en';
  }
  return _cachedLang;
}

export function setLangCache(lang: Lang): void {
  _cachedLang = lang;
}

export function t(key: TranslationKey): string {
  const lang = getLang();
  return (translations[lang] as Record<string, string>)[key] ?? translations.en[key];
}
