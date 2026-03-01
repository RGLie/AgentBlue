# 시작 가이드

## 준비물

1. **Android 기기** — AgentBlue 앱 설치 + 접근성 서비스 활성화
2. **PC** — Node.js 18 이상

## 설치

```bash
npm install -g @agentblue/cli
```

## 단계별 설정

### 1단계: 초기화

```bash
agentblue init
```

Firebase 서버를 선택합니다:
- **기본 공유 서버** — 즉시 사용 가능, 추천
- **내 Firebase** — 완전 독립 운영 (고급 사용자)

### 2단계: Android 앱 설정

AgentBlue Android 앱을 열고:
1. 접근성 서비스 활성화
2. 오버레이 권한 허용
3. AI API 키 설정 (OpenAI / Gemini / Claude / DeepSeek 중 택1)

### 3단계: 세션 연결

PC 터미널에서:
```bash
agentblue start
```

표시된 8자리 세션 코드를 Android 앱의 세션 코드 입력창에 입력합니다.

### 4단계: 명령 실행

연결되면 자연어로 명령을 입력합니다:
```
> YouTube에서 BTS 최신 노래 검색해줘
> 카카오톡에서 엄마에게 '집에 가고 있어' 보내줘
> 설정에서 화면 밝기 최대로 설정해줘
```

## 다음 단계

- 자체 Firebase 사용: `agentblue init --custom-firebase`
