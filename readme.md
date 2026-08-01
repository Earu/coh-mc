# Chat Over Head (COH)

Minecraft port of the GMod rtchat/coh addon. What a player is typing shows above their
head in real time, and sent messages linger there for a few seconds.

Rendering is server-side with vanilla `text_display` entities, so viewers don't need the
mod. Install it on the server, and on the client of anyone who wants their typing shared.

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
