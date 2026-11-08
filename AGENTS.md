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
- `fabric-client/`: Fabric client mod. Separate Gradle build. `render/` draws patterns, `mixin/` hooks them into
  vanilla's shulker box renderer and item model types; `assets/minecraft/items/*_shulker_box.json` switch the box
  items to the mod's item model type.
- `paper-selftest/`: development-only plugin that checks the plugin on a real Paper server.

## Status (2026-11-06)

- **Slice 3 (labels) done; owner, 2026-11-07: looks good on 0.3.1 except the icon slot's clicks (fixed in 0.3.2,
  not yet confirmed).** Plugin 0.2.0, client 0.3.2, **protocol 2** (both must be
  updated together).
  - Placed boxes (owner's pick "B", 16 blocks): the label icon on every face except the one the box stands on, like
    an item in an invisible item frame (`render/Labels`); faces against a solid block or another box are skipped;
    hidden while the box is open. The box you look at gets its name floating above it (the icon item's name, grey,
    if it isn't renamed), drawn like Logic Blocks' side labels.
  - Box items: a half-size icon in the bottom-right corner in inventories and the hotbar
    (`GuiGraphicsExtractorMixin`); the chosen icon comes from the item's custom data (`PublicBukkitValues`), the
    automatic one from its contents.
  - Icon slot to the right of an open box's screen (`ui/IconSlot`): click then click an item, click with an item on
    the cursor, right-click for automatic. Server: `opened` / `seticon` (`ClientBridge`), only for the box that
    player has open.
  - `/shulkerstyles demo [kit]` (creative only, `Demo`): a 4 x 2 wall of example boxes 3 blocks ahead with a water
    cauldron, and a kit (4 patterned banners, 3 plain boxes, a patterned box); refused if the space isn't empty.
    Owner asked for it: crafting test boxes every time was tedious.
  - Client 0.3.1: the owner found the icon slot's hover tooltip never showed. Cause: drawn from Fabric's
    `afterExtract`, which runs after the frame's tooltips are drawn (`Screen.extractRenderStateWithTooltipAndSubtitles`
    ends with `extractDeferredElements`). Now `afterForeground` (for container screens: after the slots, before the
    carried item and tooltips). Picking mode is now a yellow slot with "Now click an item" under it; "Icon" label.
  - Client 0.3.2: clicking the icon slot did nothing (owner). Cause: 26.x numbers mouse buttons like SDL (left 1,
    right 3), so the slot read a left click as a right click ("automatic"). Now `InputConstants.MOUSE_BUTTON_*`;
    `SourceRulesTest` fails on any literal button comparison.
  - Settings (`/shulkermod`): labels on/off, label distance 4-64 (16), inventory icons on/off. `/shulkermod info` is
    the old status command.
  - Checked here: plugin 32 unit tests; self-test 18 checks (+2 demo) on Paper 26.3-211 (new: setting/clearing the icon keeps
    the patterns); mixin audit passes with the two new mixins; client compiles. **Not checked: anything on screen,
    and the icon slot round trip** (needs a player).

- **Slice 2 (patterns drawn): seen working in the owner's game (0.2.1).** Client 0.2.2 (plugin unchanged, 0.1.0; protocol 1).
  - Placed boxes: the layers the server reported are drawn over the box, the lid's part moving with the lid.
  - Items (inventory, hand, dropped, item frames): from the item's own `banner_patterns`.
  - How: each layer is one more pass of vanilla's own (posed) shulker box model with a mask texture: the banner
    pattern laid out on the box texture at 4x (`MaskLayout`), tinted with the dye colour, render type
    `bannerPattern`, as shields draw theirs. Mixins: `ShulkerBoxRendererMixin` (render state + after the model),
    `ShulkerBoxRenderStateMixin`, `SpecialModelRenderersMixin` (item model type `shulkerstyles:shulker_box`).
  - Checked here: `ci/mixin-audit/run.sh` (every mixin applied in a real Fabric Loader, no game started),
    `MaskLayout` unit tests, `MixinPackageTest`, and a picture of the real creeper pattern laid on the real lime box
    texture. **Not checked: anything on screen** (no 26.3 client here).
  - Client 0.2.0 as first delivered gave the owner a black screen: a helper interface sat in the mixin package
    (Mixin refuses to load non-mixins from it). Fixed in 0.2.1; the audit and `MixinPackageTest` now catch it.
  - **Owner, 0.2.1 in game: "looks good"**, apart from a short delay between placing a box and its pattern showing
    (the 10-tick scan). 0.2.2 asks the moment a shulker block entity loads (`ClientBlockEntityEvents`), and forgets a
    position when its block entity loads or unloads, so a new box never shows a broken one's pattern.

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

1. Slice 2 needs the owner's in-game check (`docs/TEST_PLAN.md` tests 18-27), then fixes from his screenshots
   (likely suspects: a face mirrored, layer order, z-fighting, brightness).
2. Slice 3 needs the owner's in-game check (`docs/TEST_PLAN.md` tests 29-40) and fixes from his screenshots
   (likely suspects: icon size or distance from the face, an icon facing inward, the name's height, the corner icon
   hiding behind the item).
3. Later, not committed to: a key to toggle labels; icons that ride the lid while it opens.

## Decisions already made (don't reopen without asking)

| Decision | Why |
|---|---|
| Name "Shulker Styles", folder `software\shulker-styles` | Owner's call |
| The box keeps its own dye colour; the banner's layers go over it, its base colour is ignored | Owner's call (shields replace the colour; boxes don't) |
| Label icon: automatic (most common item inside, by total count, ties to the first slot) **and** the player can choose one | Owner: "both" |
| Demos only in creative mode | Same rule as Logic Blocks: they hand out free items |
| Labels on placed boxes: the icon on every face except the one the box stands on; the name pops up over the box you look at; within 16 blocks by default (settings 4-64) | Owner picked "B" and 16 blocks from mockups of A (icon + name always) / B / C (only on look) |
| Unnamed box under the crosshair shows its icon item's name (grey) | Something useful to read; renaming is optional |
| Icons hide while a box is open | The lid would carry half the side icon away; simpler than making icons ride the lid |
| Choosing the icon: a button in the shulker box's screen (mod, slice 3): click the icon slot by the title, then any item in the box or your inventory; click again for automatic. `/shulkerstyles icon` (off hand) stays as the fallback for players without the mod | Owner: the command alone isn't good enough UX; picked the in-screen button over dropping an item onto the box or an item frame |
| Removing patterns: water cauldron, one layer per use, one water level each, like banners; vanilla's undye only once no layers are left; sneaking places | Owner chose the cauldron, then confirmed one layer per use "for now" |
| Recipe: shapeless box + banner; banner used up; no result for a plain banner or a box that already has patterns; at most 16 layers | Kickoff, as shields |
| Item storage: vanilla `banner_patterns` component; chosen icon in item PDC `shulkerstyles:icon` | Kickoff; `banner_patterns` is what the renderer will read |
| Placed box storage: block entity PDC (`shulkerstyles:layers` text, `shulkerstyles:icon`) | Kickoff. Vanilla does keep unknown components on the block entity (`BlockEntity.components`, read from the 26.3 jar) but only creative breaks and Ctrl+pick copy them back; the loot table copies just name, contents, lock, loot table. Paper has no API to read them, so the PDC is the plugin's record |
| Drops restored per path: `BlockDropItemEvent` (player), `BlockBreakBlockEvent` (piston), explosion records matched in `ItemSpawnEvent`, and a fallback in `ItemSpawnEvent` matching a still-present box by position, type and exact contents | Each path fires different events; the self-test proves them |
| Protocol: answer per position, then push changes for watched positions (5-tick poll), range 64 | Kickoff shape (Logic Blocks); polling catches every change source (hoppers, commands, other plugins); 64 is vanilla's shulker box render distance |
| Name sent as plain text | Anvil names are plain; simpler for the renderer |
| Pattern fit on a box face: the middle square of the banner pattern (rows 10-29 of 40), on all four sides, the lid's top and the base's bottom; lid and base share one continuous picture | Owner picked "B: square crop" from mockups of A squashed / B crop / C strip |
| Patterns drawn as extra passes of vanilla's posed box model with per-pattern mask textures (4x the box texture), tinted per layer | Same technique as shields; follows the lid for free; one mask per pattern, cached |
| Mask skips the box texture's see-through pixels | A pattern must not fill the box's gaps |
| Box items switch to the mod's item model type by overriding vanilla's 17 `items/*_shulker_box.json` in the mod's resources | 26.3's shulker item renderer takes no item data; a new type can read `banner_patterns` like the shield's |
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
- Mixin audit: `ci/mixin-audit/run.sh` (Linux, CI; offline add `--offline -I <init script>` and set
  `GRADLE_USER_HOME`): starts Fabric Loader with the mod, applies every mixin, exits before the game. Prints
  `AUDIT RESULT OK` or fails.
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

- **Protocol 2:** plugin 0.2.0 and client 0.3.0 only work with each other; an older mod on a new server (or the
  reverse) draws nothing and `/shulkermod info` says to update.
- **Mouse buttons in 26.x are SDL-numbered: left 1, middle 2, right 3** (not 0/1/2). Compare with
  `InputConstants.MOUSE_BUTTON_LEFT/RIGHT`; vanilla's own code does (`AbstractContainerScreen.mouseClicked`).
- **Screen overlays: draw from `ScreenEvents.afterForeground`, not `afterExtract`.** `afterExtract` runs after the
  frame's tooltips, so tooltips set there never show and the drawing covers item tooltips.
- **Only mixins in `client/mixin/`.** Duck interfaces and helpers go elsewhere (`client/render/`), or the game
  fails to load resources (black screen). Run `ci/mixin-audit/run.sh` after any mixin change; CI runs it too.
- Box item model override: a resource pack that replaces `items/*_shulker_box.json` wins over the mod's, and then
  box items show no patterns (placed boxes still do).
- Masks are made from the current resource packs on first use and again on joining a server, not when resource
  packs change mid-game (F3+T).
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
- Decompile with `procyon` (`apt-get install procyon-decompiler`) on classes from Loom's
  `caches/fabric-loom/26.3/minecraft-merged.jar`; vanilla assets are in `minecraft-client.jar`.
- The Fabric client's tests need LWJGL/jtracy natives for Linux that a Windows-built cache lacks: run its pure-Java
  tests in a plain-JUnit Gradle build of `model/**` + the class under test.
- Paper offline: copy `paper-26.3-211.jar` with `cache/`, `versions/` and `libraries/` from the owner's server.
- Getting commits to the owner: `git bundle create X.bundle <his HEAD>..main` (or `main` for the first), copy it to
  `.tools\transfer\`, `git pull --ff-only .tools/transfer/X.bundle main` there, delete `.tools/transfer`.
