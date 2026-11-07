# Shulker Styles

Banner patterns and labels on shulker boxes, so 40 boxes in a wall can be told apart at a glance. A Paper plugin
(stores everything, works for everyone) plus a Fabric client mod (draws it). Players without the mod see normal boxes
and lose nothing.

By **tedtalksbits**. Minecraft / Paper 26.3.

## Using it

- **Patterns:** craft a shulker box with a patterned banner (any grid, or a crafter). The box takes the banner's
  pattern layers and the banner is used up, like a shield. The box keeps its own colour; re-dyeing keeps the
  patterns. A box with patterns can't take another banner.
- **Washing:** use the box on a water cauldron: the top layer comes off and the water goes down one level, like a
  banner. Sneak to place the box on the cauldron instead.
- **Label name:** rename the box in an anvil.
- **Labels (with the mod):** each placed box shows its icon on its sides and top, and the box you look at shows its
  name above it, within 16 blocks (`/shulkermod` to change). Box items show the icon in the corner of their slot.
- **Label icon:** automatically the item the box holds most of. To choose one: open the box and use the slot to the
  right of its screen (with the mod), or hold the box with the item in your off hand and run `/shulkerstyles icon`.
  `/shulkerstyles icon clear` goes back to automatic.
- `/shulkerstyles info`: what the box you hold or look at carries (also works without the mod).

The style stays with the box when it's broken (survival or creative), broken by a piston or an explosion, placed by
a dispenser, or picked with Ctrl+middle-click. (The self-test proves the piston, explosion, dispenser and
crafter paths on a real server; the player paths are in `docs/TEST_PLAN.md`.)

## Status

Slice 1 (server side): recipe, storage, washing, icon, protocol. Slice 2: the mod draws the patterns (seen working in
game). Slice 3: labels and the icon slot (plugin 0.2.0, client 0.3.0, protocol 2); not yet seen in a game.

## Layout

| Folder | What |
|---|---|
| `shulker-plugin/` | Paper plugin. `model/` is pure Java shared with the mod (protocol, layers, icons) |
| `fabric-client/` | Fabric client mod (own Gradle build) |
| `paper-selftest/` | Development-only plugin: checks the plugin on a real Paper server |
| `ci/`, `.github/` | CI and releases (`docs/CI.md`) |
| `run/` | Dev loop: build, test and install on the local test server |

See `AGENTS.md` for status and decisions, `docs/PROTOCOL.md`, `docs/TEST_PLAN.md`.
