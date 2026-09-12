# Mirror Recorder

GitHub: <https://github.com/Dedlock500543/Mirror-RecorderMod>

Download the project archive (ZIP): <https://github.com/Dedlock500543/Mirror-RecorderMod/archive/refs/heads/main.zip> —
the file is saved as `Mirror-RecorderMod-main.zip`; the folder after unpacking is `Mirror-RecorderMod-main`.

Русская версия: [README_RU.md](README_RU.md)



A client-side mod for **Minecraft 1.12.2 / Forge 14.23.5.2864** that records player input
(movement, camera, clicks, jumping, chat, hotbar slots) and plays it back.
The mod does not run its own physics simulation — it replays the actual key presses,
so playback stays consistent with the game mechanics.

> The mod is **client-side only**. It must not be installed on a server.

---

## Requirements

| Component | Version |
|---|---|
| Minecraft | 1.12.2 |
| Minecraft Forge | 14.23.5.2864 or newer in the 1.12.2 branch |
| Java (game) | Java 8 |
| Java (building from source) | **JDK 8** — ForgeGradle 3 does not work on JDK 9+ |
| Baritone | optional, hooked via reflection |

---

## Installation

1. Install Minecraft Forge for 1.12.2.
2. Put `Mirror-Recorder-1.12.2-1.0.0.jar` into the `mods` folder of your game directory.
3. Launch the game.

The paths below are relative to **your** game directory (`.minecraft` or the instance folder
in MultiMC / Prism / CurseForge, etc.). The mod resolves them through Forge and writes nowhere else.

| What | Where |
|---|---|
| Config | `config/mirror_recorder.cfg` |
| Recordings | `mirror_recorder/slot_N.nbt` (`.bak` — backup, `.tmp` — temporary file) |
| Export | `mirror_recorder/exports/*.mrr` |
| Trash | `mirror_recorder/trash/` (up to 100 most recently deleted recordings) |
| Diagnostics log | `logs/mirror-debug.log` (only when diagnostics are enabled) |

---

## Controls

Keys are **unbound by default** to avoid conflicts with other mods.
Bind them in `Options → Controls → Mirror Recorder`, or simply use the commands.
Key names are automatically translated into the game language.

| Command | Action |
|---|---|
| `/mirror gui` | open the mod's main window |
| `/mirror record <slot>` | start recording |
| `/mirror play <slot>` | play back once |
| `/mirror loop <slot>` | play back in a loop |
| `/mirror limit <slot> <N>` | limit the number of loops (`0` — no limit) |
| `/mirror stop` | stop everything |
| `/mirror list` | list occupied slots |
| `/mirror name <slot> <name>` | rename a recording |
| `/mirror delete <slot>` | delete a recording (goes to trash) |
| `/mirror marker <slot> [text]` | set the recording description |

100 slots are available, up to 72,000 frames per slot (1 hour at 20 TPS).

---

## Features

- Input recording and playback without changing the physics.
- Loop playback with a loop limit and no delay between loops.
- Playback speed 0.25×–4×. Presses from skipped frames are replayed in the same order; the camera angle is taken from the last frame of the step. Route stabilization works only at 1.0× speed.
- Bounded route stabilization — input only, no teleports.
- Auto-return to the start point (optionally via Baritone, if installed).
- Route visualization and start marker (dot / ring / beacon), configurable color.
- Status HUD; inside the mod windows it is moved with **Shift + mouse drag**, so a plain click always reaches the buttons underneath.
- Import/export recordings as `.mrr`, slot duplication, trash.
- Interface languages: Russian, English, Ukrainian, German, Polish. By default the language follows the game language (`auto`); a manual override is available in `Settings → Interface → Language`. Keybind names in the Controls screen are translated by the game itself according to the game language (not the mod language), and the category name there is always `Mirror Recorder` — it is not translated.
- Stops when the player takes damage (configurable) and on manual movement input — W, A, S, D or the space bar.

---

## Building from source

You need **JDK 8**. The build checks this itself and fails with a clear message on JDK 9+.

```bat
set "JAVA_HOME=<path-to-your-JDK-8>"
gradlew.bat --no-daemon build
```

```sh
export JAVA_HOME=<path-to-your-jdk8>
./gradlew --no-daemon build
```

Result: `build/libs/Mirror-Recorder-1.12.2-1.0.0.jar` (already reobfuscated, `jar.finalizedBy('reobfJar')`).

Run the client for testing: `gradlew runClient` (working directory `run/`).
Build environment summary: `gradlew modInfo`.

When changing the version, update it in three places:

1. `build.gradle` → `version`
2. `src/main/resources/mcmod.info` → `"version"`
3. `src/main/java/com/mirror/recorder/MirrorRecorder.java` → `VERSION`

---

## Good to know

- Recording survives world transfers (lobby → game): everything goes into one file, and playback waits for the new world and continues. The transfer itself must replay for real — the recorded walk into the portal, menu click or command.
- Keys pressed inside windows are recorded too: dropping items, hotbar-number swaps, closing the window with a key, typing on anvils and signs. Ctrl combos in text fields (clipboard paste) are not replayed — this is a known limitation.
- Recordings made with this version remember the open screen: during playback a shop or chest window closes where it closed while recording. Older recordings also close it, but based on recorded clicks, so within about half a second.
- If the mod keybinds in Controls were corrupted by an earlier version, set them once more and they will persist from now on.
- Chat playback sends **real** messages and commands to the server.
  On public servers, mind the rules — input automation may be forbidden.
- Chat recording is **off by default**, so private messages and passwords
  don't end up in recording files. Enable it in `Settings → Recording`.
- Baritone is not a dependency: without it, auto-return works in a simplified
  local alignment mode.
- Chat and commands from **imported** recordings are never sent: a foreign `.mrr` cannot
  talk or run commands on your behalf. The mod says so in chat on the first such frame.
  Your own recordings behave as before.
- Clicks inside game windows (chests, crafting tables) are replayed at the recorded cursor
  position and only in a window of the same type. The mod does not inspect container contents,
  so if the items are arranged differently than during recording, the click lands on another slot.
- A damaged recording now loads partially: broken frames are skipped, the rest is replayed,
  and the number of lost frames is written to `logs/`.

---

Author: **ToTcamii** (einprinz7@gmail.com) · <https://github.com/Dedlock500543/Mirror-RecorderMod>
