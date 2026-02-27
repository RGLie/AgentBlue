# AgentBlueDesktop - Desktop Commander for AI Android Agent

A cross-platform desktop/web app for remotely controlling [AgentBlue](../AgentBlue) on Android devices. Send natural-language commands from your PC and monitor the AI agent's execution in real time — all powered by Firebase Firestore.

**Live Web App**: https://agentblue-d83e5.web.app

## Key Features

- **Session Pairing** — Connect to an Android device with an 8-digit code
- **Remote Command Dispatch** — Send natural-language commands to the Android agent
- **Real-Time Monitoring** — Watch the agent's progress, step-by-step reasoning, and live status
- **Command History** — Browse past commands and their results
- **Compact Mode** — Quick-launch command input via global hotkey
- **Desktop Notifications** — Get notified when commands complete or fail

## UI Modes

### Main Mode (480×720)
- Session management panel
- Command input field
- Agent status panel (progress bar, step list, reasoning details)
- Command history list

### Compact Mode (520×110)
- Always-on-top minimal window
- Command input only
- Toggle via global hotkey: `Cmd+Shift+Space` (macOS) / `Ctrl+Shift+Space` (Windows/Linux)

## Architecture

```
┌──────────────────────────────────────────────────────┐
│  MainPage                                            │
│  ┌────────────────┐  ┌─────────────────────────────┐ │
│  │  SessionPanel  │  │  CommandInput               │ │
│  │  (Session      │  │  (Input + Status Display)   │ │
│  │   Management)  │  │                             │ │
│  └────────┬───────┘  └─────────────┬───────────────┘ │
│           │                        │                 │
│  ┌────────┴───────┐  ┌────────────┴────────────────┐ │
│  │ AgentStatus    │  │  CommandHistory             │ │
│  │ Panel          │  │  (Past Commands)            │ │
│  │ (Live State)   │  │                             │ │
│  └────────┬───────┘  └─────────────┬───────────────┘ │
└───────────┼────────────────────────┼─────────────────┘
            │                        │
            ▼                        ▼
┌──────────────────────────────────────────────────────┐
│  FirebaseService                                     │
│  ┌──────────────┐ ┌───────────────┐ ┌──────────────┐ │
│  │  Session      │ │  Command      │ │  State       │ │
│  │  Management   │ │  Send/Receive │ │  Stream      │ │
│  └──────────────┘ └───────────────┘ └──────────────┘ │
└──────────────────────────┬───────────────────────────┘
                           │
                           ▼
                ┌─────────────────────┐
                │  Firebase Firestore │
                │  (Real-Time Sync)   │
                └─────────────────────┘
                           │
                           ▼
                ┌─────────────────────┐
                │  AgentBlue          │
                │  (Android Agent)    │
                └─────────────────────┘
```

## Tech Stack

| Category | Technology |
|----------|------------|
| Framework | Flutter 3.3.0+ |
| Language | Dart |
| Real-Time DB | Firebase Firestore |
| Auth | Firebase Auth (Anonymous) |
| Window Management | window_manager |
| Hotkeys | hotkey_manager |
| Notifications | local_notifier |
| ID Generation | uuid |
| Hosting | Firebase Hosting |

## Project Structure

```
lib/
├── main.dart                     # App entry point, MainPage, window management
├── firebase_options.dart         # Firebase platform config (auto-generated)
├── services/
│   ├── firebase_service.dart     # Firebase communication (sessions, commands, state)
│   ├── hotkey_service.dart       # Global hotkey registration & management
│   └── notification_service.dart # Desktop notification handling
├── widgets/
│   ├── session_panel.dart        # Session create / connect / disconnect UI
│   ├── command_input.dart        # Command input field
│   ├── command_history.dart      # Command history list
│   └── agent_status_panel.dart   # Agent status display panel
└── theme/
    └── app_colors.dart           # Color theme definitions
```

## Connection with AgentBlue (Android)

Both projects share the same Firebase project (`agentblue-d83e5`) and communicate in real time through Firestore.

### Session Pairing Flow

```
AgentBlueDesktop                    Firebase                      AgentBlue (Android)
      │                               │                               │
      │  1. Click "Create Session"     │                               │
      │  → createSession()            │                               │
      │  → Generate 8-digit code      │                               │
      │  ─────────────────────────►   │                               │
      │     sessions/{id}             │                               │
      │     code: "ABCD1234"          │                               │
      │     status: "waiting"         │                               │
      │                               │                               │
      │     Display code on screen    │                               │
      │                               │                               │
      │                               │   2. User enters the code     │
      │                               │   → Query session             │
      │                               │     (code + waiting)          │
      │                               │  ◄─────────────────────────── │
      │                               │                               │
      │                               │   3. Set androidUid           │
      │                               │      status → "paired"        │
      │                               │  ◄─────────────────────────── │
      │                               │                               │
      │  4. listenSessionStatus()     │                               │
      │     Detect "paired"           │                               │
      │     → Update UI               │                               │
      │  ◄─────────────────────────   │                               │
```

### Command Execution Flow

```
AgentBlueDesktop                    Firebase                      AgentBlue (Android)
      │                               │                               │
      │  User types: "Search for      │                               │
      │   music on YouTube"           │                               │
      │  ─────────────────────────►   │                               │
      │  commands/{id}                │                               │
      │  status: "pending"            │                               │
      │                               │   FirebaseCommandListener     │
      │                               │   detects pending command     │
      │                               │  ─────────────────────────►   │
      │                               │                               │
      │                               │   status → "processing"       │
      │                               │   Start ReAct loop            │
      │                               │  ◄─────────────────────────   │
      │                               │                               │
      │  agentStateStream()           │   Real-time state updates     │
      │  ◄─────────────────────────   │  ◄─────────────────────────   │
      │  ┌─────────────────────┐      │   agentState/current:         │
      │  │ Step 1/10           │      │   step: 1, reasoning: "..."   │
      │  │ Finding the YouTube │      │                               │
      │  │ app and tapping it  │      │                               │
      │  └─────────────────────┘      │                               │
      │                               │                               │
      │  ┌─────────────────────┐      │   step: 2, reasoning: "..."   │
      │  │ Step 2/10           │      │                               │
      │  │ Tapping the search  │      │                               │
      │  │ button              │      │                               │
      │  └─────────────────────┘      │                               │
      │                               │                               │
      │                               │   status → "completed"        │
      │                               │   result: "Task done"         │
      │  Result received + alert      │  ◄─────────────────────────   │
      │  ◄─────────────────────────   │                               │
```

### Firestore Data Structure

```
sessions/{sessionId}
├── code: String                    # 8-digit pairing code
├── desktopUid: String              # Desktop user UID
├── androidUid: String              # Android user UID
├── status: String                  # "waiting" | "paired" | "disconnected"
├── createdAt: Timestamp
│
├── commands/{commandId}            # Commands subcollection
│   ├── command: String             #   Command text
│   ├── status: String              #   "pending" → "processing" → "completed"/"failed"
│   ├── result: String              #   Execution result
│   ├── deviceId: String            #   Device ID
│   ├── createdAt: Timestamp
│   └── updatedAt: Timestamp
│
└── agentState/current              # Real-time agent state
    ├── status: String              #   "IDLE" | "RUNNING" | "COMPLETED" | "FAILED"
    ├── currentCommand: String
    ├── currentStep: int
    ├── maxSteps: int
    ├── currentReasoning: String
    └── liveSteps: Array<Map>       #   Each step's action, target, reasoning, success
```

## Build & Run

### Prerequisites

- Flutter SDK 3.3.0+
- Dart SDK
- Firebase CLI (`firebase-tools`)

### Run Locally

```bash
# Install dependencies
flutter pub get

# Run on macOS
flutter run -d macos

# Run on Windows
flutter run -d windows

# Run on Web
flutter run -d chrome
```

### Web Build & Deploy

```bash
# Build for web
flutter build web

# Deploy to Firebase Hosting
firebase deploy --only hosting
```

### Supported Platforms

| Platform | Status |
|----------|--------|
| macOS | Supported |
| Windows | Supported |
| Linux | Supported |
| Web | Supported (Firebase Hosting) |

## Usage

1. Launch the app and click **"Create Session"**
2. Share the displayed **8-digit code** with the Android AgentBlue app
3. Once paired, type a natural-language command in the **command input field**
4. Watch the agent's execution in real time
5. (Optional) Press `Cmd+Shift+Space` to toggle **Compact Mode** for quick command entry

## Security

- Firestore security rules restrict access to authenticated users only
- Only session owners (desktopUid / androidUid) can access their session data
- Anonymous Firebase Auth — no account required
- 8-digit codes exclude ambiguous characters (0/O, 1/I/L) to prevent confusion

## License

This is a private project.
