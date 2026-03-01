# AgentBlue

An open-source AI-powered Android automation system. Give it a natural-language command from your terminal, Telegram, or Discord — and it autonomously navigates your Android device, tapping, typing, and scrolling using a step-by-step ReAct loop backed by your choice of LLM.

## Projects

| Project | Stack | Description |
|---------|-------|-------------|
| [**AgentBlueCLI**](./AgentBlueCLI) ✨ | Node.js · TypeScript · Firebase | **Primary control interface** — terminal REPL, `/stop`, `/setting`, `/model`, Telegram, Discord |
| [**AgentBlue**](./AgentBlue) | Kotlin · Jetpack Compose · Room · Firebase | Android agent app — reads the screen via Accessibility Service and executes actions |
| [**AgentBlueDesktop**](./AgentBlueDesktop) | Flutter · Firebase | Legacy desktop commander (superseded by AgentBlueCLI) |

---

## How It Works

```
AgentBlueCLI / Telegram / Discord
          │
          │  "Search YouTube for lo-fi music"
          ▼
   Firebase Firestore  ──────────►  AgentBlue (Android)
   (Relay Server)                      │
          ◄────────────────────────    │  ReAct Loop
          real-time state updates      │  ┌──────────────────────┐
          result + steps               │  │ 1. Parse UI tree      │
                                       │  │ 2. Ask LLM            │
                                       │  │ 3. Execute action     │
                                       │  │ 4. Repeat until DONE  │
                                       │  └──────────────────────┘
```

### Session Pairing

1. Run `agentblue start` in your terminal → get an 8-character session code
2. Open the AgentBlue Android app → enter the code in the **Session** card
3. Once paired, every command you type executes on the Android device

### Running Commands Directly on Device

Tap the floating robot button that appears on top of any app and type a command — no terminal required.

---

## Quick Start

### AgentBlueCLI (Terminal / Telegram / Discord)

```bash
npm install -g @agentblue/cli
agentblue init      # first-time setup (Firebase config + language)
agentblue start     # pair with your Android device and start a REPL
```

**REPL slash commands (available after `agentblue start`):**

| Command | Action |
|---------|--------|
| `<natural language>` | Send command to Android |
| `/stop` | Cancel the currently running task |
| `/setting` | Change agent settings on the device (max steps, delay, browser, language) |
| `/model` | Change AI model settings on the device (provider, model, API key) |
| `/help` | Show available slash commands |
| `exit` / Ctrl+C | Disconnect session |

**Standalone commands:**

```bash
agentblue setting   # configure agent settings remotely
agentblue model     # configure AI model remotely
agentblue send "open YouTube"   # send a single command without REPL
agentblue attach telegram       # add Telegram integration
agentblue attach discord        # add Discord integration
```

See [AgentBlueCLI →](./AgentBlueCLI) for full documentation.

### AgentBlue (Android)

**Prerequisites:** Android Studio · JDK 17+ · API key for at least one LLM provider

```bash
cd AgentBlue
./gradlew assembleDebug
```

1. Install the APK and accept the consent screen
2. Open **AI Model Settings** — select a provider and enter your API key
3. Enable **Accessibility Service** (Settings → Accessibility → AgentBlue)
4. Grant **Draw over other apps** permission
5. Enter the session code shown by `agentblue start` to enable remote control

> You can also configure all settings remotely via `agentblue setting` and `agentblue model` from the CLI once paired.

---

## Supported AI Providers

| Provider | Models |
|----------|--------|
| OpenAI | GPT-5 Mini, GPT-5 Nano, GPT-4.1 Mini, GPT-4o Mini, GPT-4o, o3-mini |
| Google Gemini | Gemini 2.5 Flash, 2.0 Flash, 2.0 Flash Lite, 1.5 Flash, 1.5 Pro |
| Anthropic Claude | Claude Sonnet 4, Claude 3.5 Sonnet, Claude 3.5 Haiku |
| DeepSeek | DeepSeek V3 (chat), DeepSeek R1 (reasoner) |

---

## Safety Guardrails

The agent will **stop and return DONE** before:

- Confirming any payment or purchase
- Confirming any destructive action (delete account, unsubscribe, factory reset, etc.)

The user always makes the final call on irreversible actions.

---

## Firestore Data Structure

```
sessions/{sessionId}
├── code                    # 8-character pairing code
├── desktopUid / androidUid # Firebase Auth UIDs
├── status                  # waiting → paired → disconnected
│
├── commands/{commandId}
│   ├── command             # natural-language instruction
│   ├── status              # pending → processing → completed / failed
│   └── result
│
├── agentState/current
│   ├── status              # IDLE / RUNNING / COMPLETED / FAILED / CANCELLED
│   ├── currentStep / maxSteps
│   ├── currentReasoning
│   └── liveSteps[]         # per-step action, target, reasoning, success
│
├── control/current         # remote cancel signal (CLI /stop)
│   ├── action              # "cancel" | "idle"
│   └── requestedAt
│
└── settings/current        # remote configuration (CLI /setting, /model)
    ├── maxSteps / stepDelayMs / defaultBrowser / language
    ├── provider / model / apiKey
    └── updatedAt
```

---

## Tech Stack

| | AgentBlueCLI | AgentBlue (Android) |
|-|--------------|---------------------|
| Language | TypeScript | Kotlin |
| Runtime | Node.js 18+ | Android 8.0+ (API 26) |
| UI | Terminal REPL / Telegram / Discord | Jetpack Compose + Material 3 |
| Backend | Firebase Firestore + Auth | Firebase Firestore + Auth |
| Networking | firebase JS SDK | Retrofit 3 + OkHttp |
| Local storage | `~/.agentblue/config.json` | Room DB |
| Async | async/await | Kotlin Coroutines |

---

## License

MIT
