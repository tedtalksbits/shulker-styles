# Test plan: Shulker Styles, slice 1 (server side)

The server side passed the real-Paper self-test (crafter, dispenser, breakNaturally, piston, TNT, explosions,
info, wash). Nothing here has been seen in a real game yet. The mod **draws nothing yet**; use `/shulkermod` (mod)
and `/shulkerstyles info` (plugin) to see what each side knows.

## Setup (once, about 5 minutes)

1. Close Minecraft (Windows won't replace a mod jar the game has open).
2. Double-click `run\build-and-install.cmd`. It stops the test server (`..\blueprints-mc\run\local-paper`), builds
   and tests both parts, installs `plugins\ShulkerStyles.jar` and the client mod in `%APPDATA%\.minecraft\mods\`,
   then offers to start the server.
3. Launch **fabric-loader-26.3** and join. Make a couple of patterned banners in a loom (a red creeper on white, a
   blue border on black). The recipe needs a **banner** with patterns, not a banner pattern item (the loom template,
   whose icon also shows a creeper).

The mod draws nothing yet: boxes look the same as before. That's slice 2.

## Tests

| # | Do this | Expect |
|---|---|---|
| 1 | `/shulkermod` | "Shulker Styles: connected (range 64, ...)". |
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

## ⚠️ Known limits and decisions for you

1. **Choosing the icon is a command for now** (`/shulkerstyles icon`, item in the off hand). I picked it because a
   recipe or a click would clash with dyeing, placing or washing. A button in the mod could come in slice 3: say if
   you'd rather have something else.
2. **The automatic icon tells modded players what's inside a box** (the most common item), within 64 blocks, even
   if they couldn't open it. Fine for friends and family; it's what "automatic" means.
3. **Washing takes one layer per use**, like a banner. Say if you'd rather wash all layers at once.
4. **Players without the mod see patterns only in the box's tooltip** (vanilla lists them) and through
   `/shulkerstyles info`, not on the box itself.
5. **Where labels show on a placed box (decision 4) is still open.** I'll bring mockups with slice 3.
6. Not checked by the self-test, only by you: a player breaking a box (tests 8-9), the crafting grids (2-3), the
   cauldron click (13-14), Ctrl+middle-click (10), and the mod's handshake and live updates (1, 6-7, 11-12).
