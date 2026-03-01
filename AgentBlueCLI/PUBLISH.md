# npm 배포 가이드

## 사전 준비

1. **npm 계정 생성**: https://www.npmjs.com/signup
2. **스코프 `@agentblue` 소유권**:  
   - `@agentblue/cli`는 스코프 패키지입니다.  
   - 처음 배포 시 npm에서 `agentblue` 조직을 만들거나, npm 사용자명이 `agentblue`여야 합니다.  
   - 다른 이름 사용 시: `packages/cli/package.json`에서 `"name": "@your-username/cli"` 등으로 변경하세요.

## 배포 절차

### 1. 로그인

```bash
npm login
```

Username, Password, Email, OTP를 입력합니다.

### 2. 빌드

```bash
cd AgentBlueCLI
npm run build
```

### 3. 배포

**패키지 디렉터리에서 직접 배포:**

```bash
cd packages/cli
npm publish --access public
```

**또는 워크스페이스 루트에서:**

```bash
cd AgentBlueCLI
npm publish -w @agentblue/cli --access public
```

> `--access public`은 스코프 패키지(`@agentblue/cli`)를 공개 배포할 때 필요합니다.

### 4. 버전 올리기 (재배포 시)

```bash
cd packages/cli
npm version patch   # 2.0.0 → 2.0.1
npm version minor   # 2.0.0 → 2.1.0
npm version major   # 2.0.0 → 3.0.0
npm publish --access public
```

## 설치 확인

```bash
npm install -g @agentblue/cli
agentblue --help
```

## 트러블슈팅

| 문제 | 해결 |
|------|------|
| `402 Payment Required` | 무료 계정은 월 제한이 있습니다. 일정 기간 후 재시도하세요. |
| `403 Forbidden` | 로그인 상태 확인: `npm whoami` |
| 패키지명 중복 | `package.json`의 `name`을 다른 이름으로 변경 |
| `@agentblue` 사용 권한 없음 | `name`을 `@내npm아이디/cli` 형식으로 변경 |
