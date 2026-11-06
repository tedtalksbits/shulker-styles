# Client <-> server protocol (v1)

Plugin messages on `shulkerstyles:*` channels. Encoding is Minecraft's `FriendlyByteBuf` style: VarInt, big-endian
Int32, Bool (1 byte), String (VarInt byte length + UTF-8). Both sides use the shared `dev.whizzle.shulker.model`
classes (`Wire`, `Protocol`, `BoxInfo`, `Layer`), compiled into the plugin and the mod.

| Channel | Direction | Body |
|---|---|---|
| `shulkerstyles:hello` | C->S | VarInt protocol |
| `shulkerstyles:welcome` | S->C | VarInt protocol, VarInt range (blocks) |
| `shulkerstyles:query` | C->S | VarInt n (1-64), n x (Int32 x, y, z) |
| `shulkerstyles:box` | S->C | Int32 x, y, z, Bool is a box, then if so a **BoxInfo** |

**BoxInfo**: VarInt layer count (0-16), each layer = String pattern id (`minecraft:creeper`), VarInt dye id (0 white,
1 orange, 2 magenta, 3 light_blue, 4 yellow, 5 lime, 6 pink, 7 gray, 8 light_gray, 9 cyan, 10 purple, 11 blue,
12 brown, 13 green, 14 red, 15 black); then String name (plain text, "" if not renamed), String icon item id (""
for none), Bool icon chosen by the player (false: the most common item inside). Layers are bottom first, as on
banners.

## Behaviour

- The client sends `hello` once the server announces the channels. The server answers `welcome` whatever version
  the client sent; a client whose protocol differs from the server's stays quiet and draws nothing.
- The client asks about the shulker boxes it sees within `range` of the player (`range` in the plugin's
  `config.yml`, default 64: Minecraft draws shulker boxes up to 64 blocks away). The server answers every position
  that is loaded and within range with `box` (Bool false where there's no shulker box: forget it) and stays silent
  for the rest.
- Every answered position stays **watched**: every 5 ticks the server compares what the mod would draw there with
  what it last sent, and sends a new `box` when it changed (box placed, broken, styled, renamed, contents changed the
  icon). Watching stops, silently, once the player is more than `range` + 16 blocks away, the chunk unloads or the
  player changes world; the client forgets positions beyond `range` and asks again when it comes back.
- Limits per player: 40 messages and 512 positions per second, 2048 watched positions (oldest dropped). Anyone may
  ask. The answer includes the most common item inside, which a player could otherwise only see by opening the box.
- Bump `Protocol.VERSION` when any message's bytes change (one constant, shared by both sides). Release the plugin
  and the mod together then.

## Stored data (not on the wire)

- Item: vanilla's `minecraft:banner_patterns` component; the chosen icon in the item's persistent data,
  `shulkerstyles:icon` (an item id).
- Placed box: the block entity's persistent data: `shulkerstyles:layers` (`minecraft:creeper red,minecraft:border
  blue`, `Layer.toText`) and `shulkerstyles:icon`.
