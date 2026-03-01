# Telegram 봇 연동 가이드

## 봇 생성

1. Telegram에서 [@BotFather](https://t.me/botfather)를 검색합니다.
2. `/newbot` 명령을 보냅니다.
3. 봇 이름을 입력합니다 (예: `My AgentBlue`).
4. 봇 사용자명을 입력합니다 (예: `my_agentblue_bot`).
5. BotFather가 **토큰**을 제공합니다 (예: `7123456789:AAFxx...`).

## AgentBlueCLI에 연결

```bash
agentblue attach telegram
```

- Bot Token을 입력합니다.
- 허용할 Chat ID를 설정합니다 (보안 권장).

Chat ID 확인 방법:
1. 봇에게 아무 메시지를 보냅니다.
2. 브라우저에서 열기: `https://api.telegram.org/bot{TOKEN}/getUpdates`
3. JSON 응답에서 `result[0].message.chat.id` 값이 Chat ID입니다.

## 사용법

`agentblue start` 실행 후 Telegram에서:

| 명령 | 설명 |
|------|------|
| `/run <명령>` | Android 기기에 명령 전송 |
| `/r <명령>` | `/run`의 단축키 |
| `/status` | 현재 실행 상태 확인 |
| `/session` | 세션 ID 확인 |
| `/help` | 도움말 |

### 예시

```
/run YouTube에서 BTS 최신 뮤직비디오 재생해줘
/run 오늘 날씨 검색해줘
/status
```

## 서버 독립 실행

CLI 없이 서버에서 Telegram 봇만 단독으로 운영할 수 있습니다.

```bash
npm install -g @agentblue/telegram
```

`.env` 파일 생성:
```env
AGENTBLUE_BOT_TOKEN=7123456789:AAFxx...
AGENTBLUE_SESSION_ID=세션_ID_from_agentblue_start
AGENTBLUE_ALLOWED_CHAT_IDS=123456789,987654321
```

실행:
```bash
AGENTBLUE_BOT_TOKEN=xxx AGENTBLUE_SESSION_ID=yyy agentblue-telegram
```

systemd 서비스로 등록해 항상 실행할 수도 있습니다.
