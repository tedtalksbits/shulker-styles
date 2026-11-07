# Test plan: Shulker Styles

Slice 1 (server side, tests 1-17) passed the real-Paper self-test; in a game, tests 2 and 5 passed. Slice 2 (patterns,
tests 18-28): 0.2.1 looked good in your game. **Slice 3 (labels, plugin 0.2.0 + client 0.3.0, tests 29-40) is new and
hasn't been seen on screen**: screenshots please. Plugin and mod must both be updated (protocol 2).
`/shulkermod info` (mod) and `/shulkerstyles info` (plugin) show what each side knows; `/shulkermod` opens the mod's
settings.

## Setup (once, about 5 minutes)

1. Close Minecraft (Windows won't replace a mod jar the game has open).
2. Double-click `run\build-and-install.cmd`. It stops the test server (`..\blueprints-mc\run\local-paper`), builds
   and tests both parts, installs `plugins\ShulkerStyles.jar` and the client mod in `%APPDATA%\.minecraft\mods\`,
   then offers to start the server.
3. Launch **fabric-loader-26.3** and join. In creative, **`/shulkerstyles demo`** builds a wall of 8 example boxes
   3 blocks in front of you (patterns, contents, names, one chosen icon) with a full water cauldron to its right, and
   gives you a kit: 4 patterned banners, 3 plain boxes and a ready patterned box. `/shulkerstyles demo kit` gives
   only the kit. Or make banners in a loom yourself (a red creeper on white, a blue border on black). The recipe needs a **banner** with patterns, not a banner pattern item (the loom template,
   whose icon also shows a creeper).

## Tests

| # | Do this | Expect |
|---|---|---|
| 1 | `/shulkermod info` | "Shulker Styles: connected (range 64, ...)". |
| 2 | In the **2x2** grid: a lime shulker box + the creeper banner | The result is a lime box. Take it: the banner is gone. `/shulkerstyles info` with it in hand: "Patterns: red creeper". |
| 3 | In a **crafting table**, shift-click the result of box + banner | Same as 2. |
| 4 | Try: the patterned box + another banner; a box + a plain banner | No result either time. |
| 5 | Hover the patterned box in your inventory | The tooltip lists the patterns ("Red Creeper Charge"): vanilla shows them even without the mod. Seen by the owner. |
| 6 | Put a few stacks in it (say 10 cobblestone, 40 iron), place it, look at it: `/shulkermod` | Patterns: red creeper. Icon: minecraft:iron_ingot (most common inside). |
| 7 | Add 64 cobblestone to the placed box, close it, `/shulkermod` again | Icon: minecraft:cobblestone (the mod got the change without asking again). |
| 8 | **Survival:** break it, pick it up, `/shulkerstyles info` | Patterns still there; the contents too (place it and open it). |
| 9 | **Creative:** break a non-empty patterned box | It drops with its patterns. |
| 10 | **Creative:** Ctrl+middle-click a placed patterned box | The box in your hand has the patterns (`/shulkerstyles info`). |
| 11 | Rename a box in an anvil ("Redstone"), place it, `/shulkermod` | Name: Redstone. |
| 12 | Hold a box, a diamond in your **off hand**: `/shulkerstyles icon`. Place it, `/shulkermod` | Icon: minecraft:diamond (chosen). Break it and place it again: still diamond. `/shulkerstyles icon clear` with it in hand: automatic again. |
| 13 | A full **water cauldron**: right-click it with a box that has 2 layers | The top layer is gone, the water one level lower. Again: no layers. Again (a dyed box): vanilla turns it into a plain box. |
| 14 | Sneak + right-click the cauldron with a patterned box | The box is placed on the cauldron; nothing washed. |
| 15 | Dye a patterned box (box + dye) | New colour, patterns kept. |
| 16 | A dispenser facing open air, a patterned box inside, a button. Press it, then break the box | Placed, and drops with its patterns. |
| 17 | Join with a client **without** the mod (or have someone do it) | Boxes look and work as normal; no errors. |

### Slice 2: patterns drawn (client 0.2.2)

Each face shows the middle square of the banner's pattern (your pick "B"). Screenshots of 18, 20, 21 and 22 please.

| # | Do this | Expect |
|---|---|---|
| 18 | Look at the patterned box in your **inventory** and **hotbar** | The pattern shows on the box icon, in the dye colour, over the box's own colour. |
| 19 | Hold it in your hand (first person, then F5 third person), drop it on the ground | The pattern shows in every view. |
| 20 | **Place** it and walk around it | The pattern on all four sides and on top. Not flickering at any angle. A creeper face is upright and not mirrored. |
| 21 | Open it (right-click) and watch the lid from the side | The lid's part of the picture rises and twists with the lid; the inside stays plain. |
| 22 | A wall of a few boxes with different banners (two or more layers on one, a gradient on one) | Layers stack in loom order; the gradient fades smoothly. Easy to tell apart at a glance? |
| 23 | Place a plain box next to them | No pattern. |
| 24 | Wash a placed-then-picked-up box in the cauldron, place it again | One layer fewer on the item at once, and on the placed box. |
| 25 | Place a patterned box on a wall (sideways) and on a ceiling | The pattern follows the box (sideways it lies sideways, like the lid). |
| 26 | Walk 70 blocks away and back | Far away the box disappears as in vanilla; back in range the pattern is there again within a second. |
| 27 | Break a placed patterned box and put a **plain** box in the same spot | The new plain box shows no pattern (no leftovers). |
| 28 | Place a patterned box and watch it | The pattern is there straight away (0.2.1 took up to half a second). |

### Slice 3: labels (plugin 0.2.0, client 0.3.0)

Your picks: the icon on the box, the name over the box you look at, within 16 blocks. Screenshots of 29, 30, 32 and
35 please.

| # | Do this | Expect |
|---|---|---|
| 29 | A few placed boxes with different contents (and patterns) | Each shows its icon (the item it holds most of) on its sides and top, small, over the pattern. Blocks show as little 3D blocks, items flat. |
| 30 | Walk around one and look from above | The icon on all four sides and the top, never mirrored or inside out; the bottom (on the floor) has none. |
| 31 | Stack boxes into a wall | Icons only on the faces you can see, none squeezed between two boxes. |
| 32 | Look at a box you renamed ("Redstone") | "Redstone" floats above it, like a name tag. Look away: gone. An unnamed box shows its icon's name in grey. |
| 33 | Walk away | Icons and names disappear past 16 blocks; patterns stay. |
| 34 | Open a box | Its icons disappear while it's open and come back when it closes. |
| 35 | Open a box: a slot to the right of its screen shows its icon | Hover it: what the icon is, automatic or chosen, and how to change it. |
| 36 | Click that slot (it turns yellow), then click a diamond in your inventory | The slot shows a diamond; the diamond stays where it was. Close the box: the icon on the box is a diamond. |
| 37 | Pick up an item with the mouse, click the slot with it | That item becomes the icon. Right-click the slot: back to automatic. |
| 38 | Break a box with a chosen icon, look at it in your inventory and hotbar | A small icon in the bottom-right corner of the box item; the chosen one, not the most common. Place it again: same icon. |
| 39 | `/shulkermod` | Settings: labels on/off, label distance, icons on box items on/off. Each takes effect at once. |
| 40 | A friend without the mod | Normal boxes, no slot, no errors. `/shulkerstyles icon` still works for them. |
| 41 | Creative, open ground: `/shulkerstyles demo`, facing each way once | The wall 3 blocks ahead, left to right: Mob drops, Diamonds, Farm, (unnamed string box) on top; Redstone, Metals (chosen icon: iron), Stone, Wood below. Cauldron on the right. Kit in your inventory. |
| 42 | `/shulkerstyles demo` again in the same spot; then in survival | "Not enough room" (nothing built over the first wall); in survival: "Demos only work in creative mode". |

## ⚠️ Known limits and decisions for you

1. **Plugin and mod go together:** protocol 2. An older mod on the updated server (or the reverse) draws nothing until
   both are updated; `/shulkermod info` says so.
2. **The automatic icon tells modded players what's inside a box** (the most common item), within 64 blocks, even
   if they couldn't open it. Fine for friends and family; it's what "automatic" means.
3. **Washing takes one layer per use**, like a banner (your call, for now).
4. **Players without the mod see patterns only in the box's tooltip** (vanilla lists them) and through
   `/shulkerstyles info`, not on the box itself.
5. **Icons hide while a box is open** (the lid would carry half the side icon away).
6. Not checked by the self-test, only by you: a player breaking a box (tests 8-9), the crafting grids (3), the
   cauldron click (13-14), Ctrl+middle-click (10), and the mod's handshake and live updates (1, 6-7, 11-12).
7. Slice 2 was checked by you in game (0.2.1). The cloud workspace can't run the 26.3 client, so drawing changes
   are only ever seen on your screen.
8. **A resource pack that changes shulker box items** would hide the patterns on items (placed boxes still show
   them). Changing resource packs mid-game (F3+T) keeps the old pattern pictures until you rejoin.
9. **Not seen on screen: all of slice 3.** The plugin side of the icon slot passed the self-test; the slot itself
   needs you. If an icon looks too big, sits too far off the face, or the name floats too high, say so with a
   screenshot: each is a number to adjust.
