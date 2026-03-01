# AgentBlue 프로젝트 상세 분석 보고서

## 1. 프로젝트 개요

AgentBlue는 **AI 기반 Android 자동화 에이전트 시스템**으로, 두 개의 독립된 프로젝트로 구성된다.

| 프로젝트 | 기술 스택 | 역할 |
|----------|-----------|------|
| **AgentBlue** (Android) | Kotlin, Jetpack Compose, Room, Firebase, Retrofit | Android 기기에서 접근성 서비스를 통해 UI를 분석하고 LLM 추론 기반으로 자동 조작 |
| **AgentBlueDesktop** (Desktop/Web) | Flutter (Dart), Firebase | PC/웹에서 원격으로 명령을 전송하고 실행 상태를 실시간 모니터링 |

두 프로젝트는 **Firebase Firestore**를 통해 실시간으로 통신하며, 동일한 Firebase 프로젝트(`agentblue-d83e5`)를 공유한다.

---

## 2. 시스템 아키텍처

### 2.1 전체 시스템 흐름

```
┌──────────────────────┐        ┌─────────────────┐        ┌───────────────────────┐
│  AgentBlueDesktop    │        │  Firebase        │        │  AgentBlue (Android)  │
│  (Flutter)           │◄──────►│  Firestore       │◄──────►│  (Kotlin)             │
│                      │        │                  │        │                       │
│  · 세션 생성/관리     │        │  sessions/       │        │  · 접근성 서비스       │
│  · 명령 전송          │        │    commands/     │        │  · ReAct 루프          │
│  · 실시간 상태 모니터  │        │    agentState/   │        │  · LLM API 호출        │
│  · 데스크톱 알림      │        │                  │        │  · UI 트리 분석/조작    │
└──────────────────────┘        └─────────────────┘        └───────────────────────┘
                                                                    │
                                                                    ▼
                                                           ┌───────────────────┐
                                                           │  LLM API          │
                                                           │  (OpenAI/Gemini/  │
                                                           │   Claude/DeepSeek)│
                                                           └───────────────────┘
```

### 2.2 세션 페어링 프로토콜

```
Desktop                         Firebase                         Android
  │                               │                               │
  │  1. createSession()           │                               │
  │  ──────────────────────────►  │                               │
  │  sessions/{id}                │                               │
  │  code: "ABCD1234"            │                               │
  │  status: "waiting"           │                               │
  │  desktopUid: uid             │                               │
  │                               │                               │
  │                               │  2. code 입력 → 세션 조회      │
  │                               │  ◄──────────────────────────  │
  │                               │                               │
  │                               │  3. androidUid 설정            │
  │                               │     status → "paired"         │
  │                               │  ◄──────────────────────────  │
  │                               │                               │
  │  4. listenSessionStatus()     │                               │
  │     "paired" 감지             │                               │
  │  ◄──────────────────────────  │                               │
```

### 2.3 명령 실행 흐름

```
Desktop                         Firebase                         Android
  │                               │                               │
  │  sendCommand()                │                               │
  │  status: "pending"            │                               │
  │  ──────────────────────────►  │                               │
  │                               │  FirebaseCommandListener      │
  │                               │  "pending" 감지               │
  │                               │  ──────────────────────────►  │
  │                               │                               │
  │                               │  status → "processing"        │
  │                               │  ◄──────────────────────────  │
  │                               │                               │
  │  agentStateStream()           │  ReAct 루프 실행               │
  │  실시간 진행 수신             │  매 스텝마다 상태 동기화         │
  │  ◄──────────────────────────  │  ◄──────────────────────────  │
  │                               │                               │
  │                               │  status → "completed"/"failed"│
  │  결과 수신 + 알림             │  result: "결과 메시지"          │
  │  ◄──────────────────────────  │  ◄──────────────────────────  │
```

---

## 3. AgentBlue (Android) 상세 분석

### 3.1 프로젝트 구조

```
app/src/main/java/com/example/agentdroid/
├── MainActivity.kt                  # Jetpack Compose UI, 동의/설정/기록 화면
├── AgentStateManager.kt             # 실행 상태 관리 (StateFlow + Firestore + Room)
├── data/
│   ├── AppDatabase.kt               # Room 데이터베이스
│   ├── ExecutionEntity.kt           # 실행 기록 Entity
│   ├── ExecutionDao.kt              # 실행 기록 DAO
│   ├── AgentPreferences.kt          # 에이전트 설정 (SharedPreferences)
│   ├── ModelPreferences.kt          # AI 모델 설정
│   ├── SessionPreferences.kt        # 세션 설정
│   └── ConsentPreferences.kt        # 사용자 동의 설정
├── model/
│   ├── AgentDto.kt                  # OpenAI 요청/응답 DTO, LlmAction
│   ├── AgentState.kt                # AgentStatus enum, StepLog, ExecutionRecord
│   ├── UiNode.kt                    # UI 트리 노드 모델
│   ├── AiProvider.kt                # AI 프로바이더 정의 (4개 지원)
│   └── AnthropicDto.kt              # Anthropic 전용 DTO
├── network/
│   ├── LlmClient.kt                # LLM API 호출 클라이언트
│   ├── LlmApiService.kt            # Retrofit API 인터페이스
│   ├── RetrofitClient.kt           # Retrofit 인스턴스 팩토리
│   └── AgentApiService.kt          # Agent API 서비스
├── service/
│   ├── AgentAccessibilityService.kt # 핵심 접근성 서비스 (ReAct 루프)
│   ├── ScreenAnalyzer.kt           # 화면 분석 + LLM 프롬프트 생성
│   ├── UiTreeParser.kt             # UI 트리 파싱/JSON 직렬화
│   ├── ActionExecutor.kt           # 액션 실행 (5단계 매칭)
│   ├── FirebaseCommandListener.kt  # Firebase 원격 명령 리스너
│   ├── FloatingWindowManager.kt    # 플로팅 버튼 관리
│   └── FloatingPanelManager.kt     # 실행 상태 오버레이 패널
├── legal/
│   ├── LegalTexts.kt               # 법적 문서 텍스트
│   └── LegalScreens.kt            # 법적 문서 화면 (Compose)
└── ui/theme/
    ├── Color.kt                     # 색상 정의
    ├── Theme.kt                     # Material 3 테마
    └── Type.kt                      # 타이포그래피
```

### 3.2 빌드 설정

| 항목 | 값 |
|------|-----|
| namespace | `com.example.agentdroid` |
| compileSdk / targetSdk | 36 |
| minSdk | 26 (Android 8.0) |
| versionCode | 2 |
| versionName | 1.1.0 |
| Java 호환성 | 11 |
| Kotlin | 2.0.21 |
| AGP | 8.11.2 |

### 3.3 주요 의존성

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Jetpack Compose BOM | 2024.09.00 | UI 프레임워크 |
| Room | 2.7.1 | 로컬 DB |
| Firebase BOM | 34.9.0 | Firebase 서비스 |
| Firebase Auth | 22.1.0 | 익명 인증 |
| Retrofit | 3.0.0 | HTTP 클라이언트 |
| OkHttp Logging | 4.12.0 | 네트워크 로깅 |
| Gson | 2.13.2 | JSON 직렬화 |
| Coroutines | 1.10.2 | 비동기 처리 |

### 3.4 권한 요구사항

| 권한 | 용도 |
|------|------|
| `INTERNET` | AI API, Firebase 통신 |
| `ACCESS_NETWORK_STATE` | 네트워크 상태 확인 |
| `SYSTEM_ALERT_WINDOW` | 플로팅 UI 오버레이 |
| `BIND_ACCESSIBILITY_SERVICE` | 접근성 서비스 바인딩 |

---

### 3.5 핵심 컴포넌트 상세

#### 3.5.1 AgentAccessibilityService — 에이전트 엔진

접근성 서비스를 상속하여 전체 에이전트 흐름을 조율하는 **핵심 엔진**.

**생명주기:**
- `onServiceConnected()`: 접근성 서비스 활성화 시 호출. `AgentStateManager`, `AgentPreferences`, `ModelPreferences`, `SessionPreferences` 초기화 후, `FloatingPanelManager`, `FloatingWindowManager`, `FirebaseCommandListener`를 생성하고 시작.
- `onAccessibilityEvent()`: 이벤트 기반 동작 없음 (명령 기반 설계).
- `onDestroy()`: 모든 리소스 정리 (`currentJob` 취소, 리스너 중지, 플로팅 UI 제거, 상태 리셋).

**명령 수신 경로:**
1. **로컬**: 플로팅 버튼 → `handleCommand()` → `runReActLoop()`
2. **원격**: `FirebaseCommandListener` → `executeRemoteCommand()` → `runReActLoop()`

**ReAct 루프 (`runReActLoop`):**

```
입력: userCommand (자연어 명령)
설정: maxSteps (5~30, 기본 15), delayBetweenStepsMs (500~3000, 기본 1500)

for step in 1..maxSteps:
    1. 취소 요청 확인 → 취소 시 CANCELLED 상태로 종료
    2. consecutiveFailures >= 5 → 강제 BACK 수행 (LLM 우회)
    3. rootNode = rootInActiveWindow (null이면 대기 후 continue)
    4. consecutiveFailures >= 3 → LLM에 [SYSTEM HINT] 삽입
    5. uiTreeParser.parse(rootNode) → UiNode 트리 생성
    6. screenAnalyzer.analyze(uiTree, userCommand, actionHistory) → LlmAction
       - 실패 시 ERROR 기록, consecutiveFailures++, continue
    7. action.isDone() → COMPLETED 상태로 종료 (성공)
    8. actionExecutor.execute(rootNode, action, service) → Boolean
    9. actionHistory에 기록, consecutiveFailures 업데이트
   10. AgentStateManager.onStepCompleted() → Room + Firestore 동기화
   11. floatingPanelManager.updateStep() → 오버레이 업데이트
   12. delay(delayBetweenStepsMs)

maxSteps 도달 시 → FAILED 상태로 종료
```

#### 3.5.2 ScreenAnalyzer — 화면 분석 및 LLM 프롬프트

LLM에 전달할 시스템 프롬프트를 구성하고, UI 트리 + 사용자 목표 + 액션 히스토리를 조합하여 API를 호출한다.

**시스템 프롬프트 구조:**
- 역할 정의: "Android Automation Agent operating in a step-by-step ReAct loop"
- 각 스텝에서 받는 정보: 사용자 목표, 액션 히스토리, 현재 화면 UI 트리(JSON)
- 사용 가능한 액션: CLICK, TYPE, SCROLL, BACK, HOME, DONE
- 타겟팅 규칙: `target_text` (텍스트/힌트/설명), `target_id` (리소스 ID)
- 클릭 동작 규칙: Android의 ViewGroup 패턴(클릭 가능한 컨테이너가 비-클릭 가능 자식을 감싸는 구조) 처리
- 네비게이션 복구 규칙: 현재 화면이 목표와 무관하면 BACK/HOME 사용
- Stuck 방지 규칙: 이전 실패 액션 반복 금지, 연속 2회 실패 시 전략 변경 필수
- **결제/구매 안전 규칙**: 결제 화면 도달 시 즉시 DONE 반환. 절대로 최종 결제 버튼을 클릭하지 않음.
- **파괴적 액션 안전 규칙**: 삭제/탈퇴/구독 취소 등 확인 다이얼로그에서 즉시 DONE 반환.
- 사용자 설정: 브라우저 선호, 언어 (한국어/English)
- 출력 형식: JSON 단일 액션 (`action_type`, `target_text`, `target_id`, `input_text`, `reasoning`)

**`analyze()` 메서드 흐름:**
1. `uiTreeParser.toJson(uiTree)` → UI 트리 JSON 변환
2. `uiTreeParser.logTree(uiTree)` → 디버그 로깅
3. 사용자 메시지 구성: `USER GOAL` + `ACTION HISTORY` + `CURRENT SCREEN UI`
4. 시스템 프롬프트 빌드 (브라우저/언어 설정 반영)
5. `LlmClient.chat(systemPrompt, userMessage)` → API 호출
6. JSON 응답 파싱 → `LlmAction` 객체 반환

#### 3.5.3 ActionExecutor — 5단계 우선순위 매칭

Android UI의 복잡한 뷰 계층 구조를 처리하기 위한 **5단계 클릭 매칭 시스템**.

**문제 상황:**
```
ViewGroup (clickable, text=null)       ← 실제 클릭 대상
  ├── TextView (not clickable, text="아이유")
  └── ImageView (clickable, desc="아이유 제안 수정")  ← 잘못 클릭 위험
```

**5단계 매칭 순서:**

| 순위 | 전략 | 설명 |
|------|------|------|
| 1순위 | Resource ID | `target_id`로 `viewIdResourceName` 정확 매칭 (클릭 가능 + 서브트리 텍스트 확인) |
| 2순위 | node.text 직접 매칭 | `node.text`만 검사 (contentDescription 제외, editable 제외) |
| 3순위 | Bubble-up | 자식의 `node.text`가 매칭되면 클릭 가능한 부모를 클릭 |
| 4순위 | contentDescription | `contentDescription` 매칭 (아이콘 버튼 등) |
| 5순위 | Fallback | editable 포함 전체 탐색 |

**TYPE 액션:**
- `node.isEditable` 확인
- 텍스트/힌트 매칭 또는 `search_edit_text` ID 매칭
- `ACTION_FOCUS` → `ACTION_SET_TEXT` 순서로 실행

**SCROLL 액션:**
- 스크롤 가능한 노드 탐색 (재귀)
- "UP" → `ACTION_SCROLL_BACKWARD`, "DOWN" → `ACTION_SCROLL_FORWARD`

**글로벌 액션:**
- BACK → `GLOBAL_ACTION_BACK`
- HOME → `GLOBAL_ACTION_HOME`

#### 3.5.4 UiTreeParser — UI 트리 파싱

`AccessibilityNodeInfo` → `UiNode` 변환을 담당한다.

**파싱 정보:**
- `text`, `hintText` (API 26+), `contentDescription`
- `viewIdResourceName`, `packageName`, `className`
- `bounds` (Rect — 화면 내 좌표)
- `isClickable`, `isEditable`
- `children` (재귀적 자식 노드)

변환된 `UiNode` 트리는 Gson으로 JSON 직렬화되어 LLM에 전달된다.

#### 3.5.5 LlmClient — 멀티 프로바이더 LLM 클라이언트

**지원 프로바이더:**

| 프로바이더 | 엔드포인트 | 호환성 | 모델 |
|-----------|-----------|--------|------|
| OpenAI | `api.openai.com/v1/chat/completions` | OpenAI 호환 | gpt-5-mini, gpt-5-nano, gpt-4.1-mini, gpt-4o-mini, gpt-4o, o3-mini |
| Google Gemini | `generativelanguage.googleapis.com/v1beta/openai/chat/completions` | OpenAI 호환 | gemini-2.5-flash, gemini-2.0-flash, gemini-2.0-flash-lite, gemini-1.5-flash, gemini-1.5-pro |
| Anthropic Claude | `api.anthropic.com/v1/messages` | Anthropic 전용 | claude-sonnet-4, claude-3-5-sonnet, claude-3-5-haiku |
| DeepSeek | `api.deepseek.com/v1/chat/completions` | OpenAI 호환 | deepseek-chat (V3), deepseek-reasoner (R1) |

**분기 로직:**
- `provider.isOpenAiCompatible == true` (OpenAI, Gemini, DeepSeek): OpenAI 호환 API 호출
  - Header: `Authorization: Bearer {apiKey}`
  - Body: `OpenAiRequest` (model, messages: [system, user], responseFormat)
- `provider.isOpenAiCompatible == false` (Claude): Anthropic 전용 API 호출
  - Header: `x-api-key: {apiKey}`, `anthropic-version: 2023-06-01`
  - Body: `AnthropicRequest` (model, maxTokens: 1024, system, messages: [user])

**에러 처리:**
- 401: API 키 유효하지 않음
- 402: 잔액 부족
- 403: 접근 권한 없음
- 429: 요청 한도 초과
- 5xx: 서버 오류

#### 3.5.6 AgentStateManager — 상태 관리 허브

**관리 상태 (StateFlow):**
- `status`: `IDLE | RUNNING | COMPLETED | FAILED | CANCELLED`
- `currentCommand`: 현재 실행 중인 명령
- `currentStep` / `maxSteps`: 진행률
- `currentReasoning`: LLM의 최신 추론
- `liveSteps`: 모든 스텝 로그 (`List<StepLog>`)
- `cancelRequested`: 취소 요청 플래그

**데이터 저장소:**
- **Room DB**: `ExecutionEntity`로 실행 기록 영구 저장 (command, status, stepsJson, startTime, endTime)
- **Firestore**: `sessions/{sessionId}/agentState/current` 문서에 실시간 동기화

**동기화 타이밍:** 실행 시작(`onExecutionStarted`), 매 스텝 완료(`onStepCompleted`), 실행 종료(`onExecutionFinished`), 리셋 시 자동 호출.

#### 3.5.7 FirebaseCommandListener — 원격 명령 리스너

**리스닝 경로:**
- 세션 페어링됨: `sessions/{sessionId}/commands`
- 세션 없음: `commands` (폴백)

**동작:**
1. `status == "pending"` 문서 실시간 감지 (`addSnapshotListener`)
2. 새 문서 감지 시 `status` → `"processing"` 업데이트
3. `accessibilityService.executeRemoteCommand()` 호출
4. 결과에 따라 `status` → `"completed"` 또는 `"failed"` + `result` 업데이트

#### 3.5.8 FloatingWindowManager — 플로팅 버튼

- `TYPE_APPLICATION_OVERLAY`로 다른 앱 위에 표시
- 드래그 가능 (DraggableTouchListener: ACTION_DOWN/MOVE/UP 처리)
- 탭 시 명령 입력 다이얼로그 표시 (AlertDialog)
- 다이얼로그에서 "실행" → `onCommandEntered` 콜백 → ReAct 루프 시작
- "설정" 버튼으로 MainActivity 실행

#### 3.5.9 FloatingPanelManager — 실행 상태 오버레이

- 에이전트 실행 시 화면 상단에 상태 패널 표시
- 진행률 바 (ObjectAnimator로 애니메이션)
- 현재 명령, 스텝 진행률, 추론 내용 표시
- 상태 도트 깜빡임 애니메이션 (alpha 1→0.3 반복)
- "중지" 버튼 → `AgentStateManager.requestCancel()`
- 닫기 버튼, 드래그 이동 지원
- 결과 표시 후 5초 뒤 자동 dismiss

### 3.6 데이터 모델

#### LlmAction (AgentDto.kt)
```kotlin
data class LlmAction(
    @SerializedName("action_type") val actionType: String,   // CLICK|TYPE|SCROLL|BACK|HOME|DONE
    @SerializedName("target_text") val targetText: String?,  // 대상 요소 텍스트
    @SerializedName("target_id") val targetId: String?,      // 리소스 ID (선택)
    @SerializedName("input_text") val inputText: String?,    // TYPE용 입력 텍스트
    val reasoning: String?                                    // LLM 추론 근거
)
```

#### UiNode
```kotlin
data class UiNode(
    val text: String?, val hintText: String?, val contentDescription: String?,
    val viewIdResourceName: String?, val packageName: String?, val className: String?,
    val bounds: Rect, val isClickable: Boolean, val isEditable: Boolean,
    val children: List<UiNode>
)
```

#### StepLog (AgentState.kt)
```kotlin
data class StepLog(
    val step: Int, val actionType: String, val targetText: String?,
    val reasoning: String?, val success: Boolean, val timestamp: Long
)
```

#### ExecutionEntity (Room)
```kotlin
@Entity(tableName = "executions")
data class ExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String, val status: String, val resultMessage: String?,
    val stepsJson: String, val startTime: Long, val endTime: Long?
)
```

### 3.7 Stuck 감지 및 복구 시스템

| 연속 실패 횟수 | 대응 |
|---------------|------|
| 3회 이상 | LLM에 `[SYSTEM HINT]` 삽입: "BACK/HOME 시도 권장, 랜덤 클릭 금지" |
| 5회 이상 | LLM 판단 우회, `GLOBAL_ACTION_BACK` 강제 수행 후 `consecutiveFailures` 리셋 |

### 3.8 UI 화면 구성 (MainActivity)

**네비게이션 구조 (AppNavigator):**
- `CONSENT` → `MAIN` → (법적 문서 화면들)
- 최초 실행 시 동의 화면 → 전체 동의 후 메인 화면

**메인 화면 (LazyColumn):**
1. **SessionCard**: 8자리 코드 입력으로 세션 연결/해제
2. **ModelSettingsCard**: AI 프로바이더/모델/API 키 설정 (접기/펼치기)
3. **AgentSettingsCard**: 최대 스텝(5~30), 딜레이(0.5~3초), 브라우저, 언어 설정
4. **SettingsCard**: 접근성 서비스/오버레이 권한 설정 버튼
5. **HistoryCard**: 실행 기록 목록 (상태 배지, 스텝 수, 소요 시간, 상세 보기)
6. **InfoDialog**: 앱 정보, 초기 설정 가이드, 웹 대시보드 링크, 변경 로그, 법적 고지

---

## 4. AgentBlueDesktop (Flutter) 상세 분석

### 4.1 프로젝트 구조

```
lib/
├── main.dart                      # 앱 진입점, MainPage, 창 관리
├── firebase_options.dart          # Firebase 플랫폼별 설정 (자동 생성)
├── services/
│   ├── firebase_service.dart      # Firebase 통신 (세션, 명령, 상태)
│   ├── hotkey_service.dart        # 글로벌 단축키 등록/관리
│   └── notification_service.dart  # 데스크톱 알림
├── widgets/
│   ├── session_panel.dart         # 세션 생성/연결/해제 UI
│   ├── command_input.dart         # 명령 입력 필드
│   ├── command_history.dart       # 명령 히스토리 목록
│   └── agent_status_panel.dart    # 에이전트 상태 패널
└── theme/
    └── app_colors.dart            # 색상 테마
```

### 4.2 의존성

| 패키지 | 버전 | 용도 |
|--------|------|------|
| firebase_core | ^3.9.0 | Firebase 초기화 |
| cloud_firestore | ^5.6.0 | Firestore 실시간 DB |
| firebase_auth | ^5.3.4 | 익명 인증 |
| window_manager | ^0.4.3 | 데스크톱 창 관리 |
| hotkey_manager | ^0.2.3 | 글로벌 단축키 |
| local_notifier | ^0.1.6 | 데스크톱 알림 |
| uuid | ^4.5.1 | UUID 생성 |

### 4.3 핵심 컴포넌트 상세

#### 4.3.1 main.dart — 앱 엔트리 및 창 관리

**초기화 순서:**
1. `WidgetsFlutterBinding.ensureInitialized()`
2. `windowManager.ensureInitialized()` (데스크톱만)
3. `HotkeyService.instance.initialize()`
4. `NotificationService.instance.initialize()`
5. `FirebaseService.instance.initialize()` (Firebase + 익명 로그인 + 세션 복원)

**두 가지 UI 모드:**

| 모드 | 창 크기 | 특징 |
|------|---------|------|
| **Main Mode** | 480×720 | 전체 UI (세션, 입력, 상태, 히스토리) |
| **Compact Mode** | 520×110 | 명령 입력만, 항상 위에 표시, 태스크바 숨김 |

**모드 전환:** `Cmd+Shift+Space` (macOS) / `Ctrl+Shift+Space` 글로벌 핫키

**창 설정:**
- 타이틀바 숨김 (`TitleBarStyle.hidden`)
- 닫기 방지 (`setPreventClose(true)`) — 컴팩트 모드에서 닫기 시 컴팩트 모드 유지
- 커스텀 타이틀바 (드래그 가능한 GestureDetector)

#### 4.3.2 FirebaseService — Firebase 통신 허브

**싱글톤 패턴** (`FirebaseService._()` + `static final instance`)

**세션 관리:**
- `createSession()`: 8자리 코드 생성 (혼동 방지 문자 제외: 0/O/1/I/L), 중복 확인 후 Firestore에 문서 생성
- `listenSessionStatus()`: 세션 상태 실시간 구독 (paired/disconnected 콜백)
- `disconnectSession()`: `status` → `"disconnected"`, 모든 구독 취소
- `_restoreSession()`: 앱 재시작 시 기존 세션 복원 (desktopUid로 조회)

**명령 전송:**
- `sendCommand()`: `commands/{id}` 문서 생성 (command, status: "pending", deviceId)
- `listenCommandResult()`: 명령 결과 실시간 구독 (processing/completed/failed 콜백)

**스트림:**
- `agentStateStream()`: `agentState/current` 문서 → `AgentState` 스트림
- `commandHistoryStream()`: 최근 50개 명령 → `List<CommandRecord>` 스트림

#### 4.3.3 SessionPanel — 세션 관리 위젯

**상태 머신:**
- `none` → 세션 없음 (생성 버튼 표시)
- `waiting` → 대기 중 (코드 표시, 리스닝 시작)
- `paired` → 연결됨 (접기 가능, 해제 버튼)
- `disconnected` → 해제됨 (새 세션 생성 유도)

**접기/펼치기:** `AnimationController` + `SizeTransition` (250ms, easeInOut)

#### 4.3.4 AgentStatusPanel — 에이전트 상태 패널

**StreamBuilder 기반** 실시간 상태 표시:
- 현재 명령 표시
- 진행률 바 (`LinearProgressIndicator`)
- LLM 추론 내용 (reasoning)
- 최근 5개 스텝 목록 (`_StepRow`)
- 대기 상태 아이콘

#### 4.3.5 CommandInput — 명령 입력

- 메인 모드: 텍스트 필드 + 전송 버튼 + 미니 상태바
- 컴팩트 모드: 텍스트 필드 + 전송 버튼 + 닫기 버튼 + 상태 표시

#### 4.3.6 CommandHistory — 명령 히스토리

**StreamBuilder**로 `commandHistoryStream()` 구독하여 최근 50개 명령 표시.
각 명령별 상태 배지 (pending/processing/completed/failed), 결과 텍스트, 생성 시간.

#### 4.3.7 HotkeyService — 글로벌 단축키

- `Cmd+Shift+Space` (macOS) / `Ctrl+Shift+Space`
- `HotKeyScope.system`: 앱이 포커스되지 않아도 동작
- `hotkey_manager` 패키지 사용

#### 4.3.8 NotificationService — 데스크톱 알림

- `local_notifier` 패키지 사용
- 명령 완료/실패 시 데스크톱 알림 표시
- Web 환경에서는 비활성화

### 4.4 상태 관리 방식

- **상태 관리 라이브러리 미사용** (Provider, Riverpod, Bloc 등 없음)
- `StatefulWidget` + 로컬 `setState()`
- `StreamBuilder`로 Firestore 스트림 구독
- 콜백 패턴으로 부모-자식 통신 (`onSubmit`, `onSessionCreated`, `onDisconnected`)
- 세션 변경 시 `_refreshStreams()`로 스트림 재생성

### 4.5 지원 플랫폼

| 플랫폼 | 상태 | 비고 |
|--------|------|------|
| macOS | 지원 | GoogleService-Info.plist 포함 |
| Windows | 지원 | Firebase 설정 완료 |
| Web | 지원 | Firebase Hosting 배포 |
| Linux | 부분 지원 | Firebase 설정 미완성 |
| iOS | 설정 존재 | 미확인 |
| Android | 설정 존재 | 미확인 |

---

## 5. Firebase / Firestore 구조

### 5.1 데이터 스키마

```
sessions/{sessionId}
├── code: String                    # 8자리 페어링 코드 (혼동 문자 제외)
├── desktopUid: String              # Desktop 사용자 UID (Firebase Auth)
├── androidUid: String?             # Android 사용자 UID (페어링 시 설정)
├── status: String                  # "waiting" | "paired" | "disconnected"
├── createdAt: Timestamp
│
├── commands/{commandId}            # 명령 서브컬렉션
│   ├── command: String             # 자연어 명령 텍스트
│   ├── status: String              # "pending" → "processing" → "completed"/"failed"
│   ├── result: String              # 실행 결과 메시지
│   ├── deviceId: String            # 전송 기기 ID
│   ├── createdAt: Timestamp
│   └── updatedAt: Timestamp
│
└── agentState/current              # 실시간 에이전트 상태
    ├── status: String              # "IDLE" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED"
    ├── currentCommand: String
    ├── currentStep: int
    ├── maxSteps: int
    ├── currentReasoning: String
    ├── liveSteps: Array<Map>       # 각 스텝의 상세 정보
    └── updatedAt: Timestamp
```

### 5.2 보안 규칙 (firestore.rules)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /sessions/{sessionId} {
      // 읽기: 인증된 사용자만
      allow read: if request.auth != null;
      // 생성: 인증된 사용자만
      allow create: if request.auth != null;
      // 수정: 세션 소유자 또는 androidUid가 null인 경우만
      allow update: if request.auth != null
        && (resource.data.desktopUid == request.auth.uid
            || resource.data.androidUid == request.auth.uid
            || resource.data.androidUid == null);

      // commands 서브컬렉션: 세션 참여자만 접근
      match /commands/{commandId} {
        allow read, write: if request.auth != null
          && (get(.../sessions/$(sessionId)).data.desktopUid == request.auth.uid
              || get(.../sessions/$(sessionId)).data.androidUid == request.auth.uid);
      }

      // agentState 서브컬렉션: 세션 참여자만 접근
      match /agentState/{doc} {
        allow read, write: if request.auth != null
          && (get(.../sessions/$(sessionId)).data.desktopUid == request.auth.uid
              || get(.../sessions/$(sessionId)).data.androidUid == request.auth.uid);
      }
    }
  }
}
```

**보안 특징:**
- 모든 작업에 Firebase Auth 인증 필수 (익명 로그인 포함)
- 세션 업데이트는 소유자(desktopUid/androidUid) 또는 미페어링 상태에서만 허용
- 서브컬렉션(commands, agentState)은 세션 참여자만 접근 가능
- 8자리 코드는 혼동 가능 문자(0, O, 1, I, L) 제외

---

## 6. 두 프로젝트 간 상호작용 상세

### 6.1 데이터 흐름 매핑

| Desktop 동작 | Firestore 변경 | Android 반응 |
|-------------|---------------|-------------|
| `createSession()` | `sessions/{id}` 생성 (status: waiting) | - |
| - | - | 코드 입력 → `sessions/{id}` 업데이트 (status: paired, androidUid 설정) |
| `listenSessionStatus()` → paired 감지 | - | `FirebaseCommandListener.restartListening()` |
| `sendCommand("YouTube에서 음악 검색")` | `commands/{id}` 생성 (status: pending) | - |
| - | - | `FirebaseCommandListener` pending 감지 → status: processing |
| - | - | ReAct 루프 실행, 매 스텝마다 `agentState/current` 업데이트 |
| `agentStateStream()` 구독 → UI 업데이트 | - | - |
| - | - | 완료 → `commands/{id}` status: completed/failed + result |
| `listenCommandResult()` → 알림 표시 | - | - |
| `disconnectSession()` | `sessions/{id}` status: disconnected | `FirebaseCommandListener.restartListening()` |

### 6.2 인증 방식

- **Firebase Anonymous Auth**: 양쪽 모두 계정 생성 없이 익명 로그인
- 각 UID는 세션 문서의 `desktopUid`/`androidUid`에 저장
- 보안 규칙에서 UID 기반 접근 제어

---

## 7. LLM 프롬프트 엔지니어링 상세

### 7.1 시스템 프롬프트 핵심 규칙

1. **단일 액션 원칙**: 한 번에 정확히 1개의 액션만 반환
2. **히스토리 분석**: 이전 실패 액션 반복 금지
3. **결제 안전**: 결제/구매 화면 도달 시 즉시 DONE
4. **파괴적 액션 안전**: 삭제/탈퇴 확인 다이얼로그에서 즉시 DONE
5. **네비게이션 복구**: 무관한 화면에서 BACK → HOME → 앱 아이콘 탭
6. **Stuck 방지**: 연속 실패 시 SCROLL/BACK/HOME 전략 변경
7. **JSON 출력**: 마크다운 없이 순수 JSON만 출력

### 7.2 사용자 메시지 구조

```
=== USER GOAL ===
{사용자 명령}

=== ACTION HISTORY ===
Step 1 [CLICK] target="YouTube" → SUCCESS
Step 2 [TYPE] target="Search" input="음악" → SUCCESS
...
[SYSTEM HINT] (연속 3회 실패 시)

=== CURRENT SCREEN UI ===
{UI 트리 JSON}
```

---

## 8. 기술적 특이사항

### 8.1 Android 측

- **Retrofit 3.0 사용**: 최신 버전 (일반적으로 2.x가 주류)
- **`@Url` 동적 엔드포인트**: 프로바이더별 다른 URL 지원
- **`@HeaderMap` 동적 헤더**: 프로바이더별 다른 인증 헤더 지원
- **Google Gemini의 OpenAI 호환 API**: `/v1beta/openai/chat/completions` 엔드포인트 사용으로 별도 SDK 없이 통합
- **접근성 서비스의 `onAccessibilityEvent()` 미사용**: 순수 명령 기반 설계

### 8.2 Desktop 측

- **상태 관리 라이브러리 미사용**: 프로젝트 규모에 적합한 경량 접근
- **플랫폼별 분기**: `isDesktop` 플래그로 Web/Desktop 기능 분리
- **`window_manager`의 `setPreventClose`**: 컴팩트 모드에서 창 닫기 방지

### 8.3 공통

- **익명 인증**: 사용자 가입 없이 즉시 사용 가능
- **8자리 코드**: 혼동 문자 제외한 31자 세트 (`ABCDEFGHJKLMNPQRSTUVWXYZ23456789`)
- **실시간 동기화**: Firestore 스냅샷 리스너로 밀리초 단위 상태 전파

---

## 9. 버전 히스토리

### v1.1.0 (현재)
- 에이전트 동작 설정 추가 (최대 스텝, 딜레이, 브라우저, 언어)
- HOME 액션 지원
- Stuck 감지 및 자동 복구 시스템
- 앱 정보 다이얼로그

### v1.0.0
- 최초 릴리즈

---

## 10. 배포 정보

| 항목 | 값 |
|------|-----|
| Firebase 프로젝트 ID | `agentblue-d83e5` |
| 웹 대시보드 URL | https://agentblue-d83e5.web.app |
| Android 패키지명 | `com.example.agentdroid` |
| Flutter 앱 이름 | `agent_blue_desktop` |
| 호스팅 빌드 경로 | `build/web` |

---

## 11. 법적 문서

- `docs/privacy-policy.html`: 개인정보 처리방침 (HTML)
- `docs/terms-of-service.html`: 이용약관 (HTML)
- `legal/LegalTexts.kt`: 앱 내 법적 문서 텍스트 (상수)
- `legal/LegalScreens.kt`: 동의 화면 + 문서 표시 화면 (Compose)

동의 항목:
1. 개인정보 처리방침 동의
2. 이용약관 동의
3. 접근성 API 사용 고지 동의
