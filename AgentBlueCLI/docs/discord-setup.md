# Discord 봇 연동 가이드

## 봇 생성

1. [Discord 개발자 포털](https://discord.com/developers/applications) 접속
2. **New Application** 클릭 → 이름 입력 (예: `AgentBlue`)
3. 왼쪽 메뉴 **Bot** 클릭 → **Add Bot**
4. **Token** 섹션에서 **Reset Token** → 토큰 복사

## 봇 서버 초대

1. 왼쪽 메뉴 **OAuth2 > URL Generator**
2. Scopes: `bot`, `applications.commands` 체크
3. Bot Permissions: `Send Messages`, `Use Slash Commands`, `Embed Links`, `Read Message History` 체크
4. 생성된 URL을 브라우저에서 열어 서버에 초대

## ID 확인 방법

Discord 개발자 모드 활성화 (설정 > 고급 > 개발자 모드):
- **Server ID**: 서버 이름 우클릭 → ID 복사
- **Channel ID**: 채널 이름 우클릭 → ID 복사
- **Client ID**: 개발자 포털 > OAuth2 에서 확인

## AgentBlueCLI에 연결

```bash
agentblue attach discord
```

Bot Token, Server ID, Channel ID를 입력합니다.

## 사용법

`agentblue start` 실행 후 Discord 지정 채널에서:

| 슬래시 커맨드 | 설명 |
|------|------|
| `/run command:명령어` | Android 기기에 명령 전송 |
| `/status` | 현재 실행 상태 확인 (본인에게만 표시) |

### 예시

```
/run command:YouTube에서 BTS 최신 뮤직비디오 재생해줘
/run command:오늘 날씨 검색해줘
/status
```

실행 결과는 임베드 메시지로 실시간 업데이트됩니다.

## 서버 독립 실행

```bash
npm install -g @agentblue/discord
```

`.env` 파일 생성:
```env
AGENTBLUE_BOT_TOKEN=봇_토큰
AGENTBLUE_SESSION_ID=세션_ID
AGENTBLUE_GUILD_ID=서버_ID
AGENTBLUE_CHANNEL_ID=채널_ID
AGENTBLUE_CLIENT_ID=클라이언트_ID
```

실행:
```bash
AGENTBLUE_BOT_TOKEN=xxx AGENTBLUE_SESSION_ID=yyy \
AGENTBLUE_GUILD_ID=zzz AGENTBLUE_CHANNEL_ID=www \
AGENTBLUE_CLIENT_ID=vvv agentblue-discord
```
