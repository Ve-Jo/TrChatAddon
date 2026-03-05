# TrChatAddon

`TrChatAddon` is a Bukkit/Purpur-side addon for **TrChat** focused on local chat quality-of-life and network message presentation.

## Features

- Sends a feedback message when nobody was close enough to hear the sender in local chat
- Adds a per-receiver directional indicator for distance-based chat channels
- Injects chat color into player messages using LuckPerms meta
- Falls back to prefix color extraction when explicit chat color meta is missing
- Registers a PlaceholderAPI expansion for direction-related placeholders when PlaceholderAPI is installed
- Listens for proxy plugin messages on `trchataddon:main`
- Optional AuraSkills integration for skill level-up broadcast handling

## Screenshot

### Distance indicator in chat

![Distance indicator in chat](docs/screenshots/distance-indicator-chat.jpg)

## Requirements

- Bukkit/Paper/Purpur-compatible server
- `TrChat`
- Java 21 for building this project
- Optional: `PlaceholderAPI`
- Optional: `AuraSkills`
- LuckPerms is strongly recommended if you want per-player chat color based on meta

## Installation

1. Build the plugin jar.
2. Install `TrChat` on the backend server.
3. Optionally install `PlaceholderAPI` and/or `AuraSkills`.
4. Put the addon jar into the server `plugins` folder.
5. Start the server once to generate `config.yml`.
6. Adjust configuration values and restart/reload the server.

## Building

```bash
./gradlew build
```

## Configuration

Configuration file:

- `src/main/resources/config.yml`

Important keys:

- `no-one-heard-message`
- `direction-format`
- `auraskills-broadcast.enabled`
- `auraskills-broadcast.message`
- `thematic-messages`

### Placeholders in `direction-format`

- `%dist%` — distance to the message sender
- `%arrow%` — direction arrow relative to the receiving player
- `%player%` — sender name, if you choose to include it in your format

## Behavior Notes

### No-one-heard feedback

If an async chat event ends up with only the sender in the recipients set, the plugin sends the configured fallback message to the sender.

### Chat color injection

The addon tries to read:

- `%luckperms_meta_chat_color%`

If that is missing, it falls back to:

- `%luckperms_prefix%`

It preserves TrChat channel prefixes such as `!` and `@` by injecting the resolved color **after** the prefix instead of before it.

### Direction indicator

For distance-based TrChat channels, each receiver gets a directional prefix showing:

- approximate distance in blocks
- an arrow pointing toward the sender

## Technical Notes

- Main class: `org.ayosynk.trchataddon.TrChatAddon`
- Plugin messaging channel: `trchataddon:main`
- Dependency: `TrChat`
- Soft dependencies: `PlaceholderAPI`, `AuraSkills`
