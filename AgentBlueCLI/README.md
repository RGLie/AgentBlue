# AgentBlueCLI

Open-source CLI tool to control your Android device with natural language — from your terminal, Telegram, or Discord.

## Overview

```
┌─────────────────────────┐        ┌──────────────────┐        ┌──────────────────────┐
│  AgentBlueCLI           │        │  Firebase         │        │  AgentBlue (Android) │
│                         │◄──────►│  Firestore        │◄──────►│                      │
│  · Terminal REPL        │        │  (Relay Server)   │        │  · Accessibility Svc │
│  · Telegram Bot         │        │                   │        │  · ReAct Loop        │
│  · Discord Bot          │        │                   │        │  · LLM API calls     │
└─────────────────────────┘        └──────────────────┘        └──────────────────────┘
```

Type a command in your terminal (or Telegram / Discord), and AgentBlue executes it on your Android device step-by-step using a ReAct loop powered by your choice of LLM.

## Requirements

- **Node.js** 18+
- **AgentBlue** Android app installed on your device ([../AgentBlue](../AgentBlue))

## Installation

### From npm (recommended)

```bash
npm install -g @agentblue/cli
```

### From source

```bash
git clone https://github.com/your-username/AgentBlue.git
cd AgentBlue/AgentBlueCLI
npm install
npm run build
cd packages/cli && npm link
```

## Quick Start

### 1. Initialize

```bash
agentblue init
```

Select your language and Firebase backend:

| Option | Description |
|--------|-------------|
| Shared server *(default)* | Use the developer-hosted Firebase project — no setup needed |
| Self-hosted | Point to your own Firebase project for full independence |

### 2. Start a session

```bash
agentblue start
```

```
AgentBlue v2.0.0
────────────────────────────────────────────
Session Code: ABCD1234
Open the AgentBlue app and enter this code in the Session field.
────────────────────────────────────────────

⠸ Waiting for device...
✓ Device connected!

> Search for BTS on YouTube

⠸ Processing... Step 3/15
  👆 [CLICK] "YouTube" → SUCCESS
  ⌨️ [TYPE] "BTS" → SUCCESS
  👆 [CLICK] Search → RUNNING...

✓ Done!

>
```

### 3. REPL slash commands (after `agentblue start`)

| Input | Action |
|-------|--------|
| `<natural language>` | Send command to Android |
| `/stop` | Cancel the currently running task |
| `/setting` | Change agent settings (max steps, delay, browser, language) |
| `/model` | Change AI model (provider, model, API key) |
| `/help` | Show available slash commands |
| `exit` / Ctrl+C | End session |

### 4. Remote settings (standalone or in REPL)

```bash
agentblue setting   # Agent settings on paired device
agentblue model     # AI model settings on paired device
```

### 5. Send a one-off command (for scripts / automation)

```bash
agentblue send "Send 'running late' to John on KakaoTalk"
```

## Telegram Integration

### Setup

```bash
agentblue attach telegram
```

1. Chat with [@BotFather](https://t.me/botfather) → `/newbot` to create a bot
2. Paste the token when prompted
3. Optionally restrict access to specific Chat IDs

### Usage

```
/run Search for BTS on YouTube
/run Open Chrome and go to github.com
/status
/session
/help
```

The bot starts automatically when you run `agentblue start`.

### Standalone daemon (for servers)

```bash
npm install -g @agentblue/telegram

AGENTBLUE_BOT_TOKEN=xxx \
AGENTBLUE_SESSION_ID=yyy \
agentblue-telegram
```

## Discord Integration

### Setup

```bash
agentblue attach discord
```

1. Create an application at [discord.com/developers](https://discord.com/developers/applications)
2. Under **Bot**, copy the token
3. Under **OAuth2**, invite the bot with `bot` + `applications.commands` scopes
4. Enter your Server ID, Channel ID, and Client ID

### Usage

```
/run command:Search for BTS on YouTube
/status
```

Results are delivered as live-updating embedded messages.

### Standalone daemon (for servers)

```bash
npm install -g @agentblue/discord

AGENTBLUE_BOT_TOKEN=xxx \
AGENTBLUE_SESSION_ID=yyy \
AGENTBLUE_GUILD_ID=zzz \
AGENTBLUE_CHANNEL_ID=www \
AGENTBLUE_CLIENT_ID=vvv \
agentblue-discord
```

## Self-Hosted Firebase

By default AgentBlueCLI uses a shared Firebase project. For full independence:

1. Create a project at [Firebase Console](https://console.firebase.google.com)
2. Enable **Firestore Database** (production mode)
3. Enable **Authentication → Anonymous**
4. Register an Android app with package `com.example.agentdroid` and download `google-services.json`
5. Apply the security rules from [docs/firebase-rules.md](docs/firebase-rules.md)
6. Run `agentblue init` and select **Self-hosted**

## Language

Language is configured during `agentblue init` and stored in `~/.agentblue/config.json`. To change it, run `agentblue init` again.

| Language | Value |
|----------|-------|
| English *(default)* | `"en"` |
| Korean | `"ko"` |

## Configuration

All settings are saved to `~/.agentblue/config.json`:

```json
{
  "language": "en",
  "firebase": { "apiKey": "...", "projectId": "...", "..." : "..." },
  "sessionId": "...",
  "sessionCode": "ABCD1234",
  "telegram": {
    "botToken": "...",
    "allowedChatIds": [123456789]
  },
  "discord": {
    "botToken": "...",
    "guildId": "...",
    "channelId": "..."
  }
}
```

## Commands

| Command | Description |
|---------|-------------|
| `agentblue init` | First-time setup wizard (Firebase + language) |
| `agentblue start` | Start an interactive session with your Android device |
| `agentblue send "<task>"` | Send a single command (non-interactive) |
| `agentblue setting` | Remotely change agent settings on paired device |
| `agentblue model` | Remotely change AI model settings on paired device |
| `agentblue attach telegram` | Configure Telegram bot |
| `agentblue attach discord` | Configure Discord bot |

## Package Structure

```
AgentBlueCLI/
├── packages/
│   ├── cli/        — Main CLI  (@agentblue/cli)
│   ├── telegram/   — Telegram daemon  (@agentblue/telegram)
│   └── discord/    — Discord daemon  (@agentblue/discord)
└── docs/
    ├── getting-started.md
    ├── telegram-setup.md
    └── discord-setup.md
```

## License

MIT
