# Shulker Styles: notes for agents

Start here, then `README.md` (what it does) and `docs/PROTOCOL.md`. `docs/TEST_PLAN.md` and `docs/CI.md` cover
the rest. Kickoff brief: the owner's `shulker-kickoff.md`.

## What this is

Banner patterns and labels on shulker boxes for a small Paper server (owner: tedtalksbits, MC name TedTalks2), so
40+ boxes can be told apart. Standalone: its own repo and versions, unrelated to Logic Blocks, Blueprints or the frog
plugin (their patterns were copied, their files never edited).

- `shulker-plugin/`: Paper plugin (Java 25). Stores everything, works without the mod.
- `shulker-plugin/src/main/java/dev/whizzle/shulker/model/`: pure Java (`Protocol`, `BoxInfo`, `Layer`, `Icons`,
  `Wire`), **compiled into both** the plugin and the mod (`fabric-client/build.gradle`). Change it once.
- `fabric-client/`: Fabric client mod. Separate Gradle build.
- `paper-selftest/`: development-only plugin that checks the plugin on a real Paper server.

## Status (2026-11-05)

- **Slice 1 (server side) done, not yet playtested.** Plugin 0.1.0, client 0.1.0, protocol 1.
  - Recipe: box + patterned banner, shapeless, crafting grids and crafters (`BannerRecipe`).
  - Storage: item `banner_patterns` + PDC icon; placed box PDC (`Styles`). Kept through placing (player,
    dispenser) and every drop path (`Keeper`).
  - Washing in a water cauldron (`Washing`), chosen icon (`/shulkerstyles icon`), `/shulkerstyles info`.
  - Protocol: hello/welcome, query/box, then pushes for watched positions (`ClientBridge`).
  - Client: handshake, `BoxCache`, scans shulker block entities in range every 10 ticks, `/shulkermod`. Draws nothing.
- Plugin: 25 unit tests pass. Real-server self-test on Paper 26.3-211: 15 checks pass (crafter with/without a
  patterned banner, dispenser place + break, dispenser into an occupied spot, breakNaturally, piston, TNT, a
  sourceless explosion, two neighbouring boxes in one explosion, plain box stays plain, info: patterns/name/chosen
  and automatic icon, wash + cauldron level).
- Client: compiles against 26.3; `BoxCacheTest` (4) passes in a plain-JUnit build (the Loom test classpath needs
  Windows/Linux natives the cloud cache lacked). **Not yet run in a real game.**
- **Not seen in a real game:** a player breaking a box, the 2x2/3x3 grid, the cauldron click, Ctrl+middle-click,
  the mod's handshake and queries. `docs/TEST_PLAN.md` tests 1-17.
- CI: workflows written (copied from Logic Blocks), **not yet run** (the owner pushes).

## Open items

1. Slice 2: draw patterns on placed boxes (moving with the lid) and on box items (inventory, hand, dropped). First
   read how 26.3 draws shulker boxes (block entity renderer, the item's special model renderer) and banners/shields
   (pattern layers).
2. Slice 3: labels (name + icon) on placed boxes and items; settings screen (on/off, distance). **Decision 4 (where
   labels show) is still open:** show the owner mockups first. Suggested: the side the box opens toward, and the lid.
3. Optional: a mod button for choosing the icon instead of the command.

## Decisions already made (don't reopen without asking)

| Decision | Why |
|---|---|
| Name "Shulker Styles", folder `software\shulker-styles` | Owner's call |
| The box keeps its own dye colour; the banner's layers go over it, its base colour is ignored | Owner's call (shields replace the colour; boxes don't) |
| Label icon: automatic (most common item inside, by total count, ties to the first slot) **and** the player can choose one | Owner: "both" |
| Choosing the icon: `/shulkerstyles icon` with the box in the main hand and the icon item in the off hand; `icon clear` | Agent's pick for slice 1 (no recipe or click conflicts); flagged to the owner |
| Removing patterns: water cauldron, one layer per use, one water level each, like banners; vanilla's undye only once no layers are left; sneaking places | Owner chose the cauldron; per-layer matches vanilla banners |
| Recipe: shapeless box + banner; banner used up; no result for a plain banner or a box that already has patterns; at most 16 layers | Kickoff, as shields |
| Item storage: vanilla `banner_patterns` component; chosen icon in item PDC `shulkerstyles:icon` | Kickoff; `banner_patterns` is what the renderer will read |
| Placed box storage: block entity PDC (`shulkerstyles:layers` text, `shulkerstyles:icon`) | Kickoff. Vanilla does keep unknown components on the block entity (`BlockEntity.components`, read from the 26.3 jar) but only creative breaks and Ctrl+pick copy them back; the loot table copies just name, contents, lock, loot table. Paper has no API to read them, so the PDC is the plugin's record |
| Drops restored per path: `BlockDropItemEvent` (player), `BlockBreakBlockEvent` (piston), explosion records matched in `ItemSpawnEvent`, and a fallback in `ItemSpawnEvent` matching a still-present box by position, type and exact contents | Each path fires different events; the self-test proves them |
| Protocol: answer per position, then push changes for watched positions (5-tick poll), range 64 | Kickoff shape (Logic Blocks); polling catches every change source (hoppers, commands, other plugins); 64 is vanilla's shulker box render distance |
| Name sent as plain text | Anvil names are plain; simpler for the renderer |
| Release tag = plugin version; the client keeps its own version; both jars in each release | Same as Blueprints and Logic Blocks |

## Working rules

- Commit locally; **the owner pushes** to GitHub. Agents commit as `Claude <noreply@anthropic.com>`, per commit:
  `git -c user.name=Claude -c user.email=noreply@anthropic.com commit ...`. Never change the repo's git config or
  rewrite the owner's commits.
- Reports: short, structured, anything needing the owner's decision at the **top, marked ⚠️**. No "•" separators.
- Verify on a real Paper server before claiming something works; MockBukkit is for unit tests only. Say plainly what
  was and wasn't seen in a real game.
- End each slice with `docs/TEST_PLAN.md` (Do this / Expect table, then "⚠️ Known limits and decisions for you") and
  update this file.

## Building and testing

- Plugin: `gradlew build` at the root (Java 25, Gradle 9.8.0). Jar: `shulker-plugin/build/libs/shulker-plugin-<v>.jar`.
- Client: `cd fabric-client && gradlew build`. Jar: `fabric-client/build/libs/shulker-styles-client-<v>.jar`.
- Self-test: `ci/run-selftest.sh <paper.jar> <new-folder>` (Linux, Git Bash, CI): a throwaway flat-world server
  with `-Dshulker.selftest.exit=true`; fails unless `plugins/ShulkerStylesSelfTest/report.txt` ends
  `SELFTEST PASSED`. Offline: `PAPER_SEED_DIR=<existing server folder>`. `SELFTEST_ONLY=explosion,piston` runs only
  steps whose names start that way. About 20 seconds.
- Dev loop: `run\build-and-install.cmd` (Windows) or `run/build-and-install.sh` runs `run/DevInstall.java`: stops
  the local test server (RCON if enabled, else waits for `stop`), builds and tests both, installs the plugin as
  `plugins/ShulkerStyles.jar` and the client in Minecraft's mods folder, offers to start the server. Defaults: the
  test server `..\blueprints-mc\run\local-paper`, blueprints-mc's JDK and Gradle cache. Overrides:
  `SHULKER_SERVER_DIR`, `SHULKER_MODS_DIR`, `SHULKER_GRADLE_ARGS`, `JAVA_HOME`, `GRADLE_USER_HOME`.
- CI on every push to `main`: plugin, client, self-test (`docs/CI.md`). Releases: tag `v<plugin version>`.

## Gotchas

- MockBukkit has no live block entities: `getState(false)` is a snapshot, `isSnapshot()` is unimplemented, and
  items stored in inventories lose data components. Test placed-box code in the self-test; `ClientBridge` takes a
  lookup function so tests can fake the world.
- `Block.getState(false)` on Paper is the live block entity: PDC and inventory changes apply without `update()`.
- An explosion's drops spawn after the blocks are gone, so `Keeper` records patterned boxes at the explode event.
- Crafter output slides: keep self-test scenarios far enough apart that one's drops don't land in another's check.
- From Logic Blocks, still true on 26.x: the GUI font has no ✓ ✕ ≠ ≤ ≥ → (use ASCII); `EditBox` has no `setFilter`;
  in a Fabric `Screen` don't call `extractBackground` yourself; `config.yml` defaults merge into existing maps.
  26.x rendering: GUI via `GuiGraphicsExtractor`/`extractRenderState`, world via `SubmitNodeCollector` and
  `LevelRenderEvents.COLLECT_SUBMITS` (Logic Blocks' `SideLabelRenderer` is a working example).

## Cloud workspace pitfalls

- No network to Maven Central, Paper/Fabric Maven, Mojang or GitHub releases. Build offline: stage the owner's
  `caches/modules-2/files-2.1` and `caches/fabric-loom/26.3` (+ `minecraftMaven`, `mojang_versions_manifest.json`),
  hard-link `files-2.1/<group>/<artifact>/<version>/<hash>/<file>` into a Maven layout, and use an init script that
  replaces every repository except Loom's own `file:` repositories (Loom serves the game jars from them).
- `apt-get install openjdk-25-jdk-headless` works in the container (Ubuntu's mirror is reachable); the owner's
  `.tools/jdk` is a Windows JDK.
- Staging: files must be in a connected folder and at most 400 MB each; split big tars. Stage into
  `shulker-styles\.tools\stage\` (gitignored) and delete it afterwards.
- The Fabric client's tests need LWJGL/jtracy natives for Linux that a Windows-built cache lacks: run its pure-Java
  tests in a plain-JUnit Gradle build of `model/**` + the class under test.
- Paper offline: copy `paper-26.3-211.jar` with `cache/`, `versions/` and `libraries/` from the owner's server.
- Getting commits to the owner: `git bundle create X.bundle <his HEAD>..main` (or `main` for the first), copy it to
  `.tools\transfer\`, `git pull --ff-only .tools/transfer/X.bundle main` there, delete `.tools/transfer`.
