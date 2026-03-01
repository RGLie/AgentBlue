# AgentBlue — AI-Powered Android Automation Agent

An AI agent app that leverages Android's Accessibility Service to analyze on-screen UI and autonomously perform actions (tap, type, scroll, navigate) based on LLM reasoning — all from a single natural-language command.

## Key Features

- **ReAct Loop Automation** — Reasoning + Acting loop that breaks down natural-language commands into step-by-step screen actions
- **Multi-Provider LLM Support** — OpenAI, Google Gemini, Anthropic Claude, and DeepSeek
- **CLI Remote Control** — Pair with [AgentBlueCLI](../AgentBlueCLI) (recommended) to send commands from your terminal, Telegram, or Discord
- **Remote Settings** — Configure agent and AI model via `agentblue setting` and `agentblue model` from the CLI
- **Remote Cancel** — Stop running tasks with `/stop` from the CLI REPL
- **Desktop/Web** — Also compatible with [AgentBlueDesktop](https://agentblue-d83e5.web.app) (legacy)
- **Real-Time State Sync** — Live execution status shared via Firebase Firestore
- **Floating UI** — Overlay button lets you issue commands on top of any app
- **Stuck Detection & Recovery** — Automatically recovers when the agent gets stuck

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  MainActivity (Jetpack Compose)                             │
│  ┌────────────┐ ┌───────────────┐ ┌───────────────────────┐ │
│  │  Session    │ │  AI Model     │ │  Agent Settings /     │ │
│  │  Pairing    │ │  Config       │ │  Execution History    │ │
│  └────────────┘ └───────────────┘ └───────────────────────┘ │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌─────────────┐ ┌──────────────┐
│ Accessibility │ │  Firebase    │ │  LLM Client  │
│   Service     │ │  Command     │ │  (Retrofit)  │
│               │ │  Listener    │ │              │
└───────┬───────┘ └──────┬──────┘ └──────┬───────┘
        │                │               │
        ▼                ▼               ▼
┌──────────────┐ ┌─────────────┐ ┌──────────────┐
│ Screen       │ │ Agent State  │ │  OpenAI /    │
│ Analyzer     │ │ Manager      │ │  Gemini /    │
│ + UiTree     │ │ (Room +      │ │  Claude /    │
│   Parser     │ │  Firestore)  │ │  DeepSeek    │
└───────┬───────┘ └─────────────┘ └──────────────┘
        │
        ▼
┌──────────────┐
│ Action       │
│ Executor     │
│ (5-level     │
│  matching)   │
└──────────────┘
```

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| AI Communication | Retrofit 3.0 + OkHttp |
| Local DB | Room |
| Remote Sync | Firebase Firestore + Auth |
| Async | Kotlin Coroutines |
| Build | Gradle (Kotlin DSL) |
| Min SDK | 26 (Android 8.0) |

## Supported AI Models

| Provider | Models |
|----------|--------|
| OpenAI | gpt-4o-mini, gpt-4o, o3-mini |
| Google Gemini | gemini-2.0-flash, gemini-2.0-flash-lite, gemini-1.5-flash, gemini-1.5-pro |
| Anthropic | claude-sonnet-4-20250514, claude-3-5-sonnet-20241022, claude-3-5-haiku-20241022 |
| DeepSeek | deepseek-chat, deepseek-reasoner |

## Project Structure

```
app/src/main/java/com/example/agentdroid/
├── MainActivity.kt              # Main UI (Compose)
├── AgentStateManager.kt         # Execution state management + Firestore sync
├── data/
│   ├── AppDatabase.kt           # Room database
│   ├── ExecutionEntity.kt       # Execution history entity
│   ├── ExecutionDao.kt          # Execution history DAO
│   ├── AgentPreferences.kt      # Agent settings (SharedPreferences)
│   ├── ModelPreferences.kt      # AI model settings
│   ├── SessionPreferences.kt    # Session settings
│   └── ConsentPreferences.kt    # User consent settings
├── model/
│   ├── AgentDto.kt              # LLM response DTO (LlmAction, etc.)
│   ├── AgentState.kt            # Agent state (AgentStatus enum)
│   ├── UiNode.kt                # UI tree node
│   ├── AiProvider.kt            # AI provider definitions
│   └── AnthropicDto.kt          # Anthropic-specific DTO
├── network/
│   ├── LlmClient.kt            # AI API call client
│   ├── LlmApiService.kt        # Retrofit API interface
│   ├── RetrofitClient.kt       # Retrofit instance factory
│   └── AgentApiService.kt      # Agent API service
├── service/
│   ├── AgentAccessibilityService.kt  # Core accessibility service (ReAct loop)
│   ├── ScreenAnalyzer.kt        # Screen analysis + LLM prompt generation
│   ├── UiTreeParser.kt          # UI tree parsing (JSON serialization)
│   ├── ActionExecutor.kt        # Action execution (click, type, scroll, etc.)
│   ├── FirebaseCommandListener.kt   # Remote commands + cancel listener
│   ├── FirebaseSettingsListener.kt # Remote settings sync (agent + model)
│   ├── FloatingWindowManager.kt     # Floating button manager
│   └── FloatingPanelManager.kt      # Execution status overlay panel
└── legal/
    ├── LegalTexts.kt            # Legal document text
    └── LegalScreens.kt          # Legal document screens
```

## How It Works

### ReAct Loop

```
1. Receive command (floating button or Firebase remote command)
2. For each step (up to N iterations):
   ├── Parse UI tree (UiTreeParser)
   ├── Send screen context to LLM (ScreenAnalyzer → LlmClient)
   ├── LLM responds: { actionType, targetText, inputText, reasoning }
   ├── Execute action (ActionExecutor)
   │   └── 5-level priority matching:
   │       1) Resource ID → 2) Text → 3) Bubble-up → 4) ContentDescription → 5) Fallback
   ├── Update state (AgentStateManager → Room + Firestore)
   └── Stuck detection (3 failures: inject hint / 5 failures: force BACK)
3. Terminate: DONE → success / max steps reached → failure
```

### Supported Actions

| Action | Description |
|--------|-------------|
| `CLICK` | Tap a target element |
| `TYPE` | Enter text into a target field |
| `SCROLL` | Scroll the screen (UP / DOWN / LEFT / RIGHT) |
| `BACK` | Press the back button |
| `HOME` | Go to the home screen |
| `DONE` | Task completed |

## Connection with AgentBlueCLI (or AgentBlueDesktop)

The app communicates in real time through **Firebase Firestore**, sharing the same Firebase project (`agentblue-d83e5`). Use [AgentBlueCLI](../AgentBlueCLI) for terminal, Telegram, and Discord control — or [AgentBlueDesktop](https://agentblue-d83e5.web.app) for legacy desktop/web.

### Session Pairing

```
AgentBlueCLI / AgentBlueDesktop      Firebase                      AgentBlue (Android)
      │                               │                               │
      │  1. createSession()           │                               │
      │  ─────────────────────────►   │                               │
      │   sessions/{id}               │                               │
      │   code: "ABCD1234"            │                               │
      │   status: "waiting"           │                               │
      │                               │                               │
      │                               │   2. Enter code → find session │
      │                               │  ◄─────────────────────────── │
      │                               │                               │
      │                               │   3. status: "paired"         │
      │                               │      set androidUid           │
      │                               │  ◄─────────────────────────── │
      │                               │                               │
      │  4. Detect status change       │                               │
      │     (paired)                   │                               │
      │  ◄─────────────────────────   │                               │
```

### Command Execution Flow

```
AgentBlueCLI / AgentBlueDesktop      Firebase                      AgentBlue (Android)
      │                               │                               │
      │  sendCommand()                │                               │
      │  status: "pending"            │                               │
      │  ─────────────────────────►   │                               │
      │                               │   FirebaseCommandListener     │
      │                               │   status: "processing"        │
      │                               │  ─────────────────────────►   │
      │                               │                               │
      │                               │   Execute ReAct loop          │
      │                               │   (real-time state updates)   │
      │                               │  ◄─────────────────────────   │
      │  agentStateStream()           │                               │
      │  Receive live progress        │                               │
      │  ◄─────────────────────────   │                               │
      │                               │                               │
      │                               │   status: "completed"         │
      │                               │   result: "Task done"         │
      │                               │  ◄─────────────────────────   │
      │  Receive result               │                               │
      │  ◄─────────────────────────   │                               │
```

### Firestore Data Structure

```
sessions/{sessionId}
├── code: String                    # 8-digit pairing code
├── desktopUid: String              # CLI/Desktop user UID
├── androidUid: String              # Android user UID
├── status: String                  # "waiting" | "paired" | "disconnected"
├── createdAt: Timestamp
│
├── commands/{commandId}            # Commands subcollection
│   ├── command: String
│   ├── status: String              # "pending" | "processing" | "completed" | "failed"
│   ├── result: String
│   ├── createdAt: Timestamp
│   └── updatedAt: Timestamp
│
├── agentState/current              # Agent state document
│   ├── status: String              # "IDLE" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED"
│   ├── currentCommand: String
│   ├── currentStep: int
│   ├── maxSteps: int
│   ├── currentReasoning: String
│   └── liveSteps: Array<Map>
│
├── control/current                 # Remote cancel (CLI /stop)
│   ├── action: String              # "cancel" | "idle"
│   └── requestedAt: Timestamp
│
└── settings/current                # Remote config (CLI /setting, /model)
    ├── maxSteps, stepDelayMs, defaultBrowser, language
    ├── provider, model, apiKey
    └── updatedAt
```

## Required Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | AI API and Firebase communication |
| `ACCESS_NETWORK_STATE` | Network state check |
| `SYSTEM_ALERT_WINDOW` | Floating UI overlay |
| Accessibility Service | Screen reading and automated interaction |

## Build & Run

### Prerequisites

- Android Studio (latest recommended)
- JDK 17+
- An API key for at least one AI provider (OpenAI / Gemini / Claude / DeepSeek)

### Build

```bash
./gradlew assembleDebug
```

### Initial Setup

1. Install the app and accept the Privacy Policy & Terms of Service
2. Configure AI model settings — select provider, model, and enter API key (or use `agentblue model` after pairing)
3. Enable the Accessibility Service (Settings → Accessibility → AgentBlue)
4. Grant overlay permission
5. (Optional) Run `agentblue start` in AgentBlueCLI and enter the session code in the app for remote control

## License

This is a private project.
