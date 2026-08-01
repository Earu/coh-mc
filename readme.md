# Chat Over Head (COH)

See what players are typing above their heads, in real time — a Minecraft port of the
GMod rtchat/coh addon. When someone opens chat, a small bubble appears over their head;
as they type, everyone nearby watches the message being written. Sent messages linger
above the sender for a few seconds, then fade out.

## Viewers need no mod

All rendering is server-side, using vanilla `text_display` entities. A completely
unmodded client sees the bubbles. The mod only needs to be installed:

- **on the server** — spawns/updates the displays, shows sent-message popups for everyone
  (even players without the mod), and
- **on the client of anyone who wants their live typing broadcast** — it captures the
  chat box content and streams it to the server over an optional channel.

Every combination degrades gracefully: modded client on a vanilla server sends nothing;
vanilla client on a modded server still gets sent-message popups above their head.

## Branches

One branch per Minecraft version, each building Fabric and (Neo)Forge jars:

| Branch    | Loaders            |
|-----------|--------------------|
| `1.21.11` | Fabric, NeoForge   |
| `1.21.1`  | Fabric, NeoForge   |
| `1.20.1`  | Fabric, Forge      |

Build everything with `./gradlew build`; jars land in `fabric/build/libs` and
`neoforge/build/libs`.

## Privacy

Live typing means your keystrokes in the chat box leave your machine **before you press
enter**. Text is sent as a custom payload (never as chat packets, so chat signing and
chat reports are not involved), but you should know it happens. Escape hatches:

- Client config `config/coh/client.json` → `"enabled": false` disables all transmission.
- Start a message with `-- hide`, `// hide` or `# hide` and nothing is transmitted
  (same prefixes as the GMod original).
- Commands (anything starting with `/`) are never transmitted.
- Server config `config/coh/server.json` → `"enabled": false` kills the feature globally.

## Server config (`config/coh/server.json`)

| Key                  | Default | Meaning                                                     |
|----------------------|---------|-------------------------------------------------------------|
| `enabled`            | `true`  | Master switch                                               |
| `viewRange`          | `0.5`   | text_display view range (~32 blocks at default client view) |
| `throttleMs`         | `300`   | Server-side rate limit between typing updates               |
| `popupSeconds`       | `6.0`   | How long sent messages linger                               |
| `idleTimeoutSeconds` | `15.0`  | Kill typing bubbles that stop updating                      |
| `maxChars`           | `256`   | Length clamp                                                |
| `lineWidth`          | `200`   | Word-wrap width of the bubble                               |
| `hideWhileSneaking`  | `true`  | Sneaking hides your bubble                                  |
| `showTypingIndicator`| `true`  | Show a "…" bubble while chat is open but empty/concealed    |
| `followMode`         | `RIDE`  | `RIDE` (passenger, smooth) or `TELEPORT` (fallback)         |
| `headOffsetY`        | `0.9`   | Bubble height above the head                                |
