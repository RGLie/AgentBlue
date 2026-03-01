# AgentBlue Project Detailed Analysis Report

> Last Updated: 2026-03-01  
> Version: Android v2.0.0 / AgentBlueCLI v2.0.0

---

## 1. Project Overview

AgentBlue is an **AI-powered Android automation agent system** consisting of three independent projects.

| Project | Tech Stack | Role |
|----------|-----------|------|
| **AgentBlueCLI** (Primary) | Node.js, TypeScript, Firebase JS SDK | Terminal REPL, remote control (/stop, /setting, /model) |
| **AgentBlue** (Android) | Kotlin, Jetpack Compose, Room, Firebase, Retrofit | Analyzes UI via Accessibility Service on Android and performs automated operations based on LLM reasoning |
| **AgentBlueDesktop** (Legacy) | Flutter (Dart), Firebase | Legacy desktop commander (replaced by AgentBlueCLI) |

The three projects share the same Firebase project (`agentblue-d83e5`) and use **Firebase Firestore** as a real-time relay.

---

## 2. System Architecture

### 2.1 Overall System Flow

```
┌─────────────────────────────────────────────────┐
│  AgentBlueCLI (Node.js)                         │
│                                                  │
│  · agentblue start  → REPL (readline)           │
│  · /stop  → control/current {action:"cancel"}   │
│  · /setting → settings/current (agent config)   │
│  · /model  → settings/current (model config)    │
│                                                  │
└────────────────────┬────────────────────────────┘
                     │ Firebase JS SDK
                     ▼
         ┌─────────────────────┐
         │  Firebase Firestore  │
         │                      │
         │  sessions/{id}/      │
         │    commands/         │
         │    agentState/       │
         │    control/    ← NEW │
         │    settings/   ← NEW │
         └──────────┬──────────┘
                    │ Firestore Android SDK
                    ▼
┌───────────────────────────────────────────────────┐
│  AgentBlue (Android, Kotlin)                      │
│                                                    │
│  · FirebaseCommandListener  → Executes ReAct loop │
│  · FirebaseCommandListener  → Detects cancellation│
│  · FirebaseSettingsListener → Syncs settings      │
│  · AgentAccessibilityService → LLM + Actions      │
└───────────────────────────────────────────────────┘
```

### 2.2 Session Pairing Protocol

```
AgentBlueCLI                   Firebase                         Android
     │                            │                               │
     │  1. createSession()        │                               │
     │  ─────────────────────►   │                               │
     │  sessions/{id}             │                               │
     │  code: "ABCD1234"         │                               │
     │  status: "waiting"        │                               │
     │  desktopUid: uid          │                               │
     │                            │                               │
     │                            │  2. Enter code → Query session│
     │                            │  ◄──────────────────────────  │
     │                            │                               │
     │                            │  3. Set androidUid            │
     │                            │     status → "paired"         │
     │                            │  ◄──────────────────────────  │
     │                            │                               │
     │  4. listenSessionStatus()  │                               │
     │     Detect "paired"        │                               │
     │  ◄──────────────────────   │                               │
```

### 2.3 Command Execution Flow

```
AgentBlueCLI REPL               Firebase                         Android
     │                            │                               │
     │  sendCommand()             │                               │
     │  status: "pending"         │                               │
     │  ─────────────────────►   │                               │
     │                            │  FirebaseCommandListener      │
     │                            │  Detects "pending"            │
     │                            │  ──────────────────────────►  │
     │                            │                               │
     │                            │  status → "processing"        │
     │                            │  ◄──────────────────────────  │
     │                            │                               │
     │  listenAgentState()        │  Execute ReAct Loop           │
     │  Receive real-time progress│  Sync state at each step      │
     │  ◄──────────────────────   │  ◄──────────────────────────  │
     │                            │                               │
     │                            │  status → "completed"/"failed"│
     │  Display result            │  Update result                │
     │  ◄──────────────────────   │  ◄──────────────────────────  │
```

### 2.4 Remote Cancellation Flow (NEW — `/stop`)

```
AgentBlueCLI REPL               Firebase                         Android
     │                            │                               │
     │  Enter /stop               │                               │
     │  requestCancel(sessionId)  │                               │
     │  ─────────────────────►   │  control/current              │
     │                            │  {action:"cancel"}            │
     │                            │  ──────────────────────────►  │
     │                            │                               │
     │                            │  listenForCancelRequests()    │
     │                            │  Detect "cancel"              │
     │                            │                               agentStateManager.requestCancel()
     │                            │  action → "idle" (Reset)      │
     │                            │  ◄──────────────────────────  │
     │                            │                               │
     │  agentState → CANCELLED    │  Update agentState            │
     │  ◄──────────────────────   │  ◄──────────────────────────  │
```

### 2.5 Remote Settings Synchronization Flow (NEW — `/setting`, `/model`)

```
AgentBlueCLI                    Firebase                         Android
     │                            │                               │
     │  /setting or               │                               │
     │  agentblue setting         │                               │
     │  writeSettings(sid, data)  │                               │
     │  ─────────────────────►   │  settings/current             │
     │                            │  {maxSteps, stepDelayMs,      │
     │                            │   browser, language,          │
     │                            │   provider, model, apiKey}    │
     │                            │  ──────────────────────────►  │
     │                            │                               │
     │                            │  FirebaseSettingsListener     │
     │                            │  Detect onSnapshot            │
     │                            │                               applyAgentSettings()
     │                            │                               → Update AgentPreferences
     │                            │                               applyModelSettings()
     │                            │                               → Update ModelPreferences
```

---

## 3. AgentBlueCLI Detailed Analysis

### 3.1 Monorepo Structure

```
AgentBlueCLI/
├── package.json                   # npm workspaces root
├── tsconfig.base.json             # Common TypeScript config
│                                   (target: ES2022, module: NodeNext)
├── packages/
│   ├── cli/                       # @agentblue/cli
│   │   ├── package.json
│   │   ├── tsconfig.json          # composite: true, include: src/**/*.ts
│   │   └── src/
│   │       ├── index.ts           # Commander entry point (5 commands)
│   │       ├── config.ts          # Config management (~/.agentblue/config.json)
│   │       ├── i18n.ts            # English/Korean Internationalization
│   │       ├── commands/
│   │       │   ├── init.ts        # Firebase setup + Language selection
│   │       │   ├── start.ts       # Session start + REPL entry
│   │       │   ├── send.ts        # Single command send (non-interactive)
│   │       │   ├── setting.ts     # Remote agent settings (NEW)
│   │       │   └── model.ts       # Remote AI model settings (NEW)
│   │       ├── firebase/
│   │       │   ├── client.ts      # Firebase initialization + Anonymous Auth
│   │       │   ├── session.ts     # Session creation/listening/release
│   │       │   └── command.ts     # Command send/listen + requestCancel/writeSettings (UPDATED)
│   │       ├── ui/
│   │       │   ├── repl.ts        # Terminal REPL + Slash commands (UPDATED)
│   │       │   └── status.ts      # Agent state rendering
└── docs/
    └── getting-started.md
```

### 3.2 Major Dependencies

| Package | Version | Purpose |
|---------|---------|---------|
| commander | ^12.x | CLI command definition |
| @inquirer/prompts | ^7.x | Interactive prompts (select, input, number, password) |
| firebase | ^11.x | Firebase JS SDK |
| chalk | ^5.x | Terminal color output |
| ora | ^8.x | Spinner animation |

### 3.3 CLI Command List

| Command | Description |
|---------|-------------|
| `agentblue init` | Initial setup (Firebase backend + language selection) |
| `agentblue start` | Start session + Wait for pairing + REPL |
| `agentblue send <command>` | Send a single command (non-interactive) |
| `agentblue setting` | Remotely change Android agent settings |
| `agentblue model` | Remotely change Android AI model settings |

### 3.4 REPL Slash Commands

Interactive commands available after `agentblue start`:

| Input | Action |
|-------|--------|
| `<natural language>` | Send command to Android → Show real-time state stream |
| `/stop` | Request cancellation of running task → `control/current {action:"cancel"}` |
| `/setting` | Change agent settings (maxSteps, delay, browser, language) |
| `/model` | Change AI model settings (provider, model, apiKey) |
| `/help` | Show list of slash commands |
| `exit` / Ctrl+C | Terminate session |

### 3.5 firebase/command.ts Functions

```typescript
// Send command
sendCommand(sessionId, command): Promise<string>

// Subscribe to real-time agent state
listenAgentState(sessionId, onUpdate): Unsubscribe

// Subscribe to real-time command result
listenCommandResult(sessionId, commandId, onResult): Unsubscribe

// Request cancellation (NEW)
requestCancel(sessionId): Promise<void>
  → setDoc sessions/{id}/control/current {action:"cancel", requestedAt}

// Write settings (NEW)
writeSettings(sessionId, settings): Promise<void>
  → setDoc sessions/{id}/settings/current (merge: true)

// Subscribe to settings (NEW)
listenSettings(sessionId, onUpdate): Unsubscribe
```

---

## 4. AgentBlue (Android) Detailed Analysis

### 4.1 Project Structure

```
app/src/main/java/com/example/agentdroid/
├── MainActivity.kt                  # Jetpack Compose UI (Full English)
├── AgentStateManager.kt             # Execution state management (StateFlow + Firestore + Room)
├── data/
│   ├── AppDatabase.kt               # Room database
│   ├── ExecutionEntity.kt           # Execution record Entity
│   ├── ExecutionDao.kt              # Execution record DAO
│   ├── AgentPreferences.kt          # Agent settings (Default: English)
│   ├── ModelPreferences.kt          # AI model settings
│   ├── SessionPreferences.kt        # Session settings
│   └── ConsentPreferences.kt        # User consent settings
├── model/
│   ├── AgentDto.kt                  # OpenAI Request/Response DTOs, LlmAction
│   ├── AgentState.kt                # AgentStatus enum, StepLog, ExecutionRecord
│   ├── UiNode.kt                    # UI tree node model
│   ├── AiProvider.kt                # AI Provider definitions (4 supported)
│   └── AnthropicDto.kt              # Anthropic-specific DTOs
├── network/
│   ├── LlmClient.kt                # LLM API call client
│   ├── LlmApiService.kt            # Retrofit API interface
│   ├── RetrofitClient.kt           # Retrofit instance factory
│   └── AgentApiService.kt          # Agent API service
├── service/
│   ├── AgentAccessibilityService.kt # Core Accessibility Service (UPDATED)
│   ├── ScreenAnalyzer.kt           # Screen analysis + LLM prompt generation
│   ├── UiTreeParser.kt             # UI tree parsing/JSON serialization
│   ├── ActionExecutor.kt           # Action execution (5-tier matching)
│   ├── FirebaseCommandListener.kt  # Remote command + Cancellation listener (UPDATED)
│   ├── FirebaseSettingsListener.kt # Remote settings sync listener (NEW)
│   ├── FloatingWindowManager.kt    # Floating button (English)
│   └── FloatingPanelManager.kt     # Execution state overlay (English)
├── legal/
│   ├── LegalTexts.kt               # Legal document text constants
│   └── LegalScreens.kt            # Legal document screens (Compose)
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

### 4.2 Build Configuration

| Item | Value |
|------|-------|
| namespace | `com.example.agentdroid` |
| compileSdk / targetSdk | 36 |
| minSdk | 26 (Android 8.0) |
| versionCode | 3 |
| versionName | 2.0.0 |
| Java Compatibility | 11 |
| Kotlin | 2.0.21 |
| AGP | 8.11.2 |

### 4.3 Major Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2024.09.00 | UI Framework |
| Room | 2.7.1 | Local Database |
| Firebase BOM | 34.9.0 | Firebase Services |
| Firebase Auth | 22.1.0 | Anonymous Authentication |
| Retrofit | 3.0.0 | HTTP Client |
| OkHttp Logging | 4.12.0 | Network Logging |
| Gson | 2.13.2 | JSON Serialization |
| Coroutines | 1.10.2 | Asynchronous Processing |

### 4.4 Permission Requirements

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Communication with AI APIs and Firebase |
| `ACCESS_NETWORK_STATE` | Checking network status |
| `SYSTEM_ALERT_WINDOW` | Floating UI overlay |
| `BIND_ACCESSIBILITY_SERVICE` | Binding the Accessibility Service |

---

### 4.5 Core Component Details

#### 4.5.1 AgentAccessibilityService — Agent Engine (UPDATED)

**Lifecycle:**

- `onServiceConnected()`:
  1. Initializes `AgentStateManager`, `AgentPreferences`, `ModelPreferences`, `SessionPreferences`.
  2. Creates and starts `FloatingPanelManager`, `FloatingWindowManager`.
  3. Creates `FirebaseCommandListener(this, AgentStateManager)` including cancel listener.
  4. Creates and starts `FirebaseSettingsListener()`.
- `onDestroy()`: Cleans up listeners, current jobs, and floating UI.

**Command Reception Paths:**
1. **Local**: Floating button → `handleCommand()` → `runReActLoop()`.
2. **Remote**: `FirebaseCommandListener` → `executeRemoteCommand()` → `runReActLoop()`.

**ReAct Loop (`runReActLoop`):**

```
Input: userCommand (Natural language)
Config: maxSteps (5~30, Default 15), delayBetweenStepsMs (500~3000, Default 1500)

for step in 1..maxSteps:
    1. Check for cancel request → Terminate with CANCELLED state
    2. if consecutiveFailures >= 5 → Force BACK (bypass LLM)
    3. rootNode = rootInActiveWindow (if null, wait and continue)
    4. if consecutiveFailures >= 3 → Inject [SYSTEM HINT] into LLM prompt
    5. uiTreeParser.parse(rootNode) → UiNode tree
    6. screenAnalyzer.analyze(uiTree, userCommand, actionHistory) → LlmAction
    7. if action.isDone() → Terminate with COMPLETED
    8. actionExecutor.execute(rootNode, action, service) → Boolean
    9. AgentStateManager.onStepCompleted() → Sync with Room + Firestore
   10. Update floatingPanelManager.updateStep()
   11. delay(delayBetweenStepsMs)

If maxSteps reached → Terminate with FAILED
```

#### 4.5.2 FirebaseCommandListener — Remote Command + Cancellation Listener (UPDATED)

```kotlin
class FirebaseCommandListener(
    private val accessibilityService: AgentAccessibilityService,
    private val agentStateManager: AgentStateManager
)
```

**Two Listeners:**

1. **Command Listener** (`listenerRegistration`):
   - Path: `sessions/{id}/commands` (or `commands` if no session)
   - Detects `status == "pending"` → Update to `"processing"` → `executeRemoteCommand()` → Update to `"completed"/"failed"` on finish.

2. **Cancellation Listener** (`cancelListenerRegistration`, NEW):
   - Path: `sessions/{sessionId}/control/current`
   - Detects `action == "cancel"` → Calls `agentStateManager.requestCancel()`.
   - Resets `action → "idle"` after processing to prevent re-triggering.

#### 4.5.3 FirebaseSettingsListener — Remote Settings Synchronization (NEW)

**Listening Path:** `sessions/{sessionId}/settings/current`

**`applyAgentSettings(snap)`:**
- `maxSteps` → `AgentPreferences.setMaxSteps()`
- `stepDelayMs` → `AgentPreferences.setStepDelayMs()`
- `defaultBrowser` → `AgentPreferences.setDefaultBrowser()`
- `language` → `AgentPreferences.setLanguage()`

**`applyModelSettings(snap)`:**
- `provider` → Parse `AiProvider.valueOf()`
- `model` → Provider-specific model ID
- `apiKey` → Apply to `ModelPreferences` only if not empty (preserves existing key if empty).

#### 4.5.4 AgentPreferences — Agent Settings (UPDATED)

```kotlin
object AgentPreferences {
    const val DEFAULT_BROWSER = "Default Browser"
    const val DEFAULT_LANGUAGE = "English"

    val BROWSER_OPTIONS = listOf("Chrome", "Samsung Internet", "Firefox", "Default Browser")
    val LANGUAGE_OPTIONS = listOf("English", "한국어") // English prioritized
}
```

#### 4.5.5 ScreenAnalyzer — Screen Analysis & LLM Prompting

Constructs the system prompt for the LLM, combining UI tree, user goal, and action history.

**System Prompt Structure:**
- Role: "Android Automation Agent operating in a step-by-step ReAct loop"
- Available Actions: CLICK, TYPE, SCROLL, BACK, HOME, DONE
- Targeting Rules: `target_text` (text/hint/desc), `target_id` (resource ID)
- Safety Rules: Immediate DONE on payment screens or destructive confirmation dialogs (delete/cancel account).
- Response Format: Pure JSON without Markdown.

#### 4.5.6 ActionExecutor — 5-Tier Priority Matching

| Rank | Strategy | Description |
|------|----------|-------------|
| 1 | Resource ID | Exact match with `target_id` (viewIdResourceName) |
| 2 | Direct Text | Match `node.text` (excluding contentDescription) |
| 3 | Bubble-up | Match child text → Click clickable parent |
| 4 | Content Desc | Match `contentDescription` (icon buttons, etc.) |
| 5 | Fallback | Exhaustive search including editable nodes |

#### 4.5.7 LlmClient — Multi-Provider LLM Client

| Provider | Endpoint | Supported Models |
|-----------|-----------|------------------|
| OpenAI | `api.openai.com/v1/chat/completions` | gpt-5-mini, gpt-5-nano, gpt-4.1-mini, gpt-4o-mini, gpt-4o, o3-mini |
| Google Gemini | `generativelanguage.googleapis.com/v1beta/openai/chat/completions` | gemini-2.5-flash, gemini-2.0-flash, gemini-2.0-flash-lite, gemini-1.5-flash, gemini-1.5-pro |
| Anthropic Claude | `api.anthropic.com/v1/messages` | claude-sonnet-4, claude-3-5-sonnet, claude-3-5-haiku |
| DeepSeek | `api.deepseek.com/v1/chat/completions` | deepseek-chat (V3), deepseek-reasoner (R1) |

---

## 5. Firebase / Firestore Structure

### 5.1 Data Schema (UPDATED)

```
sessions/{sessionId}
├── code: String                    # 8-digit pairing code
├── desktopUid: String              # CLI User UID
├── androidUid: String?             # Android User UID (set on pairing)
├── status: String                  # "waiting" | "paired" | "disconnected"
├── createdAt: Timestamp
│
├── commands/{commandId}            # Command subcollection
│   ├── command: String             # Natural language text
│   ├── status: String              # "pending" → "processing" → "completed"/"failed"
│   ├── result: String              # Result message
│   └── ...
│
├── agentState/current              # Real-time agent state
│   ├── status: String              # "IDLE" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED"
│   ├── currentCommand: String
│   ├── currentStep: int
│   ├── currentReasoning: String
│   └── liveSteps: Array<Map>
│
├── control/current                 # Remote cancellation signal (NEW)
│   ├── action: String              # "cancel" | "idle"
│   └── requestedAt: Timestamp
│
└── settings/current                # Remote settings sync (NEW)
    ├── maxSteps: number
    ├── stepDelayMs: number
    ├── defaultBrowser: String
    ├── language: String
    ├── provider: String
    ├── model: String
    ├── apiKey: String
    └── updatedAt: Timestamp
```

---

## 6. Stuck Detection & Recovery System

| Consecutive Failures | Action |
|----------------------|--------|
| 3+ times | Inject `[SYSTEM HINT]` into LLM: "Try BACK/HOME, avoid random clicks." |
| 5+ times | Bypass LLM, force `GLOBAL_ACTION_BACK`, and reset counter. |

---

## 7. Version History

### v2.0.0 (Current)

**AgentBlueCLI:**
- Added `/stop` REPL command for remote cancellation.
- Added `/setting` & `/model` REPL commands + top-level commands for remote config.
- Added `/help` REPL command.
- Full English localization for CLI `--help`.
- Monorepo `tsconfig` improvements with project references.

**Android:**
- Added `control/current` cancellation listener.
- Added `settings/current` sync listener for remote configuration.
- Full English localization of the entire UI (MainActivity, Floating Windows).
- Initialized default preferences to English and "Default Browser".

---

## 8. Deployment Information

- **Firebase Project ID**: `agentblue-d83e5`
- **Web Dashboard**: https://agentblue-d83e5.web.app
- **Android Package**: `com.example.agentdroid`
- **CLI Package**: `@agentblue/cli`
