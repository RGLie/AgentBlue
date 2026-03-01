# AgentBlueCLI

터미널과 메시징 플랫폼에서 Android 기기를 AI로 자동 제어하는 오픈소스 CLI 도구입니다.

## 개요

```
┌─────────────────────────┐        ┌─────────────────┐        ┌──────────────────────┐
│  AgentBlueCLI           │        │  Firebase        │        │  AgentBlue (Android) │
│                         │◄──────►│  Firestore       │◄──────►│                      │
│  · Terminal REPL        │        │  (Relay Server)  │        │  · 접근성 서비스      │
│  · Telegram Bot         │        │                  │        │  · ReAct 루프         │
│  · Discord Bot          │        │                  │        │  · LLM API 호출       │
└─────────────────────────┘        └─────────────────┘        └──────────────────────┘
```

## 설치

### 사전 요구 사항

- Node.js 18 이상
- Android 기기에 [AgentBlue 앱](../AgentBlue) 설치

### 글로벌 설치 (권장)

```bash
npm install -g @agentblue/cli
```

### 개발용 (소스에서 빌드)

```bash
cd AgentBlueCLI
npm install
npm run build
npm link packages/cli
```

## 빠른 시작

### 1. 초기 설정

```bash
agentblue init
```

Firebase 설정 방식을 선택합니다:
- **기본 공유 서버**: 별도 설정 없이 즉시 사용 가능
- **내 Firebase**: 직접 Firebase 프로젝트를 생성해 완전히 독립적으로 운영

### 2. 세션 시작

```bash
agentblue start
```

```
AgentBlue v2.0.0
────────────────────────────────────────────
Session Code: ABCD1234
Android 앱을 열고 메인 화면에 이 코드를 입력하세요.
────────────────────────────────────────────

Waiting for device connection...
✓ 기기가 연결되었습니다!

> YouTube에서 BTS 최신 노래 검색해줘

⠸ Processing... Step 3/15
  👆 [CLICK] "YouTube" → SUCCESS
  ⌨️ [TYPE] "BTS" → SUCCESS
  👆 [CLICK] 검색 → RUNNING...

✓ 완료!

>
```

### 3. 단일 명령 전송 (스크립트/자동화용)

```bash
agentblue send "카카오톡에서 홍길동에게 '오늘 회의 취소' 메시지 보내줘"
```

## Telegram 봇 연동

### 설정

```bash
agentblue attach telegram
```

1. Telegram에서 [@BotFather](https://t.me/botfather)를 찾아 `/newbot`으로 봇 생성
2. 받은 토큰을 입력
3. 허용할 Chat ID 설정 (보안 강화)

### 사용

```
/run YouTube에서 BTS 최신 노래 검색해줘
/status
/session
/help
```

`agentblue start` 실행 시 Telegram 봇이 자동으로 시작됩니다.

### 독립 실행 (서버 운영용)

```bash
npm install -g @agentblue/telegram

AGENTBLUE_BOT_TOKEN=xxx \
AGENTBLUE_SESSION_ID=yyy \
agentblue-telegram
```

## Discord 봇 연동

### 설정

```bash
agentblue attach discord
```

1. [Discord 개발자 포털](https://discord.com/developers/applications)에서 애플리케이션 생성
2. Bot 탭에서 토큰 복사
3. OAuth2 > URL Generator에서 `bot` + `applications.commands` 권한으로 서버에 초대
4. Server ID, Channel ID 입력

### 사용

```
/run YouTube에서 BTS 검색해줘
/status
```

### 독립 실행 (서버 운영용)

```bash
npm install -g @agentblue/discord

AGENTBLUE_BOT_TOKEN=xxx \
AGENTBLUE_SESSION_ID=yyy \
AGENTBLUE_GUILD_ID=zzz \
AGENTBLUE_CHANNEL_ID=www \
AGENTBLUE_CLIENT_ID=vvv \
agentblue-discord
```

## 자체 Firebase 프로젝트 사용 (고급)

기본 공유 서버 대신 직접 Firebase 프로젝트를 운영하려면:

1. [Firebase 콘솔](https://console.firebase.google.com)에서 프로젝트 생성
2. Firestore Database 생성 (프로덕션 모드)
3. Authentication > 로그인 제공업체 > 익명 활성화
4. 보안 규칙 적용: `docs/firebase-rules.md` 참고
5. `agentblue init` 실행 시 "내 Firebase 프로젝트 사용" 선택

Android 앱도 동일한 Firebase 프로젝트의 `google-services.json`을 사용해야 합니다.

## 설정 파일

설정은 `~/.agentblue/config.json`에 저장됩니다:

```json
{
  "firebase": { ... },
  "sessionId": "...",
  "sessionCode": "ABCD1234",
  "telegram": {
    "botToken": "...",
    "allowedChatIds": [123456789]
  },
  "discord": {
    "botToken": "...",
    "guildId": "...",
    "channelId": "..."
  }
}
```

## 패키지 구조

```
AgentBlueCLI/
├── packages/
│   ├── cli/        — 메인 CLI (@agentblue/cli)
│   ├── telegram/   — Telegram 봇 데몬 (@agentblue/telegram)
│   └── discord/    — Discord 봇 데몬 (@agentblue/discord)
└── docs/           — 설정 가이드
```

## 라이선스

MIT
