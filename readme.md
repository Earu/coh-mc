# Chat Over Head (COH)

Shows what a player is typing above their head in real time and sticks around for a few
seconds when the message is sent.

Rendering is server-side with vanilla `text_display` entities. Install only needs to be
serverside for rendering, for typing in real time you need the client install too.

## Branches

| Branch    | Loaders          |
|-----------|------------------|
| `1.21.11` | Fabric, NeoForge |
| `1.21.1`  | Fabric, NeoForge |
| `1.20.1`  | Fabric, Forge    |

`./gradlew build` outputs jars to `fabric/build/libs` and `neoforge/build/libs`.

## Privacy

Typed text reaches the server before you press enter, as a custom payload (no chat
signing involved). Opt-outs:

- `config/coh/client.json`: `"enabled": false` disables all transmission.
- Messages starting with `-- hide`, `// hide` or `# hide` are never sent.
- Commands are never sent.
- `config/coh/server.json`: `"enabled": false` disables the feature server-wide.

## Server config (`config/coh/server.json`)

| Key                  | Default | Meaning                                        |
|----------------------|---------|------------------------------------------------|
| `enabled`            | `true`  | Master switch                                  |
| `viewRange`          | `0.5`   | Visibility range (about 32 blocks)             |
| `throttleMs`         | `150`   | Min interval between typing updates            |
| `popupSeconds`       | `6.0`   | How long sent messages linger                  |
| `idleTimeoutSeconds` | `15.0`  | Removes typing bubbles that stop updating      |
| `maxChars`           | `256`   | Length clamp                                   |
| `lineWidth`          | `200`   | Word-wrap width                                |
| `hideWhileSneaking`  | `true`  | Sneaking hides your bubble                     |
| `showTypingIndicator`| `true`  | Show a "…" bubble while chat is open but empty |
| `followMode`         | `RIDE`  | `RIDE` (passenger) or `TELEPORT` (fallback)    |
| `headOffsetY`        | `0.9`   | Bubble height above the head                   |

## API

Add the jar to your compile classpath and use `gg.earu.coh.api.ChatOverHead`. Server side only, keyed by UUID.

```kotlin
ChatOverHead.stateOf(uuid)          // ChatState: IDLE, TYPING or POPUP
ChatOverHead.millisInStateOf(uuid)  // wall-clock time in that state, 0 when IDLE
ChatOverHead.activePlayers()        // Map<UUID, ChatState> of everyone not IDLE, a snapshot
ChatOverHead.addListener { change -> change.playerId; change.previous; change.current; change.previousMs }
```

TYPING means the chat box is open, POPUP means a sent message is still showing. The two never overlap. Typing with a hide prefix still counts as TYPING; `showTypingIndicator` only affects rendering. The text being typed is never exposed.

Listeners fire on the server thread, only when the state changes. A second message while a popup is up refreshes the popup without an event and restarts its clock. Every way out of a state is reported: idle timeout, popup expiry, a message sent with a hide prefix or as a command, the player leaving, the server stopping. Before the server starts or with COH disabled everyone is IDLE. The same changes go through the loader's own pipeline: `CohEvents.STATE_CHANGE` on Fabric, `ChatStateChangedEvent` on the NeoForge game bus.
