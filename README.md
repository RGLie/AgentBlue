# AgentBlue

An AI-powered Android automation system. Give it a natural-language command from your PC, and it autonomously navigates your Android device — tapping, typing, and scrolling — using a step-by-step ReAct loop backed by your choice of LLM.

## Projects

| Project | Stack | Description |
|---------|-------|-------------|
| [**AgentBlue**](./AgentBlue) | Kotlin · Jetpack Compose · Room · Firebase | Android agent app — reads the screen via Accessibility Service and executes actions |
| [**AgentBlueDesktop**](./AgentBlueDesktop) | Flutter · Firebase | Desktop & web commander — sends commands and monitors execution in real time |

**Live web app**: https://agentblue-d83e5.web.app

---

## How It Works

```
AgentBlueDesktop                 Firebase Firestore               AgentBlue (Android)
       │                               │                               │
       │  Type: "Search YouTube        │                               │
       │   for lo-fi music"            │                               │
       │  ──────────────────────────►  │                               │
       │  commands/{id}                │  FirebaseCommandListener      │
       │  status: "pending"            │  ──────────────────────────►  │
       │                               │                               │
       │                               │  ┌─────────────────────────┐  │
       │                               │  │      ReAct Loop          │  │
       │                               │  │  Parse UI tree           │  │
       │                               │  │  → Ask LLM               │  │
       │                               │  │  → Execute action        │  │
       │                               │  │  → Repeat until DONE     │  │
       │                               │  └─────────────────────────┘  │
       │  Live step updates            │  agentState/current           │
       │  ◄──────────────────────────  │  ◄──────────────────────────  │
       │                               │                               │
       │  Result + notification        │  status: "completed"          │
       │  ◄──────────────────────────  │  ◄──────────────────────────  │
```

### Session Pairing

1. Open AgentBlueDesktop → click **Create Session** → get an 8-digit code
2. On your Android device, open AgentBlue → enter the code under **Session**
3. Once paired, commands typed on desktop execute on the Android device

### Running Commands Directly on Device

Tap the floating robot button that appears on top of any app and type a command — no desktop required.

---

## Supported AI Providers

| Provider | Models |
|----------|--------|
| OpenAI | GPT-5 Mini, GPT-5 Nano, GPT-4.1 Mini, GPT-4o Mini, GPT-4o, o3-mini |
| Google Gemini | Gemini 2.5 Flash, 2.0 Flash, 2.0 Flash Lite, 1.5 Flash, 1.5 Pro |
| Anthropic Claude | Claude Sonnet 4, Claude 3.5 Sonnet, Claude 3.5 Haiku |
| DeepSeek | DeepSeek V3, DeepSeek R1 |

---

## Firestore Data Structure

```
sessions/{sessionId}
├── code                    # 8-digit pairing code
├── desktopUid / androidUid # Firebase Auth UIDs
├── status                  # waiting → paired → disconnected
│
├── commands/{commandId}
│   ├── command             # natural-language instruction
│   ├── status              # pending → processing → completed / failed
│   └── result
│
└── agentState/current
    ├── status              # IDLE / RUNNING / COMPLETED / FAILED / CANCELLED
    ├── currentStep / maxSteps
    ├── currentReasoning
    └── liveSteps[]         # per-step action, target, reasoning, success
```

---

## Quick Start

### AgentBlue (Android)

**Prerequisites:** Android Studio · JDK 17+ · API key for at least one LLM provider

```bash
cd AgentBlue
./gradlew assembleDebug
```

1. Install the APK and accept the consent screen
2. Open **AI Agent Model Settings** — select a provider and enter your API key
3. Enable **Accessibility Service** (Settings → Accessibility → AgentBlue)
4. Grant **Draw over other apps** permission
5. *(Optional)* Enter a session code from AgentBlueDesktop to enable remote control

### AgentBlueDesktop

**Prerequisites:** Flutter SDK 3.3.0+

```bash
cd AgentBlueDesktop

# Run on macOS
flutter run -d macos

# Run on Windows
flutter run -d windows

# Run in browser
flutter run -d chrome
```

**Deploy to Firebase Hosting:**

```bash
flutter build web
firebase deploy --only hosting
```

---

## Safety Guardrails

The agent will **stop and return DONE** before:
- Confirming any payment or purchase
- Confirming any destructive action (delete, unsubscribe, factory reset, etc.)

The user always makes the final call on irreversible actions.

---

## Tech Stack

| | AgentBlue | AgentBlueDesktop |
|-|-----------|-----------------|
| Language | Kotlin | Dart |
| UI | Jetpack Compose + Material 3 | Flutter |
| Backend | Firebase Firestore + Auth | Firebase Firestore + Auth |
| Networking | Retrofit 3 + OkHttp | — |
| Local storage | Room | — |
| Async | Kotlin Coroutines | Dart async/streams |
| Min platform | Android 8.0 (API 26) | macOS / Windows / Web |

---

## License

Private project.
