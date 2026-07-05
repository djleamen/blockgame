# Blockgame Roadmap

A staged plan for evolving Blockgame from a flat 64×64×3 demo into something
that mechanically resembles early-alpha Minecraft. Each phase is sized so it
can land as one or two PRs and maps to existing GitHub issues where possible.

Status legend: `done` · `in progress` · `planned`

## Phase 1 — Minecraft-alignment basics (done)

Goal: the world looks and feels like Minecraft within the current
fixed-size architecture. No new threading, still immediate-mode OpenGL.

Closes / advances: #1, #3 (partial), #8 (partial)

- Expanded block palette: bedrock, stone, dirt, grass, sand, oak log,
  oak planks, oak leaves, cobblestone (existing). Place in hotbar by
  default.
- Procedural texture generator gets matching entries (stone, sand,
  log top/side, planks, leaves) — generated in-memory at boot so the
  game no longer depends on PNGs on disk.
- Heightmap terrain instead of a 3-layer slab:
  - bedrock floor (y = 0)
  - stone fill up to `height − 4`
  - dirt for the next 3 blocks
  - grass on top
  - sand wherever height is at sea level
  - a handful of small oak trees scattered on grass
  - simple value-noise based heightmap (deterministic seed)
- Render-time face culling: only emit a face if the neighbouring block
  is air or out of bounds. Removes the per-frame budget being burned
  on hidden quads and makes the bigger world viable.
- Physics tuned to Minecraft-ish numbers (gravity ≈ 28 m/s², jump
  velocity ≈ 8.4 m/s ⇒ ~1.25-block jump height, terminal velocity
  capped). Movement becomes fully delta-time based so the feel is
  framerate-independent.
- Controls: right-click places blocks (Enter remains as fallback),
  Left-Shift sneaks (slower, no edge-step yet), Left-Ctrl sprints.
- Block outline: a thin wireframe is drawn around whichever block the
  crosshair is currently pointing at.

## Phase 2 — World & survival feel (in progress)

Closes / advances: #3, #4, #1 (rest of the palette)

- Chunk container (16×16×worldHeight) so the renderer/world generator
  stop assuming a single fixed array. `done` for render meshing; world
  storage is still one dense array.
- World height to 128 (still finite, just taller).
- More blocks: water (non-flowing for now), gravel, coal/iron ore. `done` —
  still water fills to sea level and renders translucent, gravel patches
  on beaches, coal/iron veins seeded in the stone layer.
- Inventory: pick up broken blocks into the hotbar with stack counts;
  placing consumes from the stack (stone drops cobblestone, grass drops
  dirt, leaves drop nothing). `done` — including the 27-slot storage
  opened with `E` (click to pick up / place / merge / swap stacks,
  overflow pickups spill into storage, contents persisted in the save).
- Item durability + a wooden pickaxe as a stub for tool tiers.

## Phase 3 — Persistence (done)

Closes: #2

- Save world (block array + player pose + hotbar + time of day) to a
  single `world.bgsave` file via `DataOutputStream` with a tiny header +
  RLE block payload. `done`
- Load on startup if `world.bgsave` exists, otherwise regenerate from
  seed. `done`
- Autosave every 60 s and on clean shutdown. `done`

## Phase 4 — Day/night and lighting (in progress)

Closes: #5, #6

- Day/night cycle on a ~10 minute clock; sky colour, fog colour and a
  global brightness factor interpolate between dawn/day/dusk/night, and
  the clock is persisted in the save file. `done`
- Sun-direction directional light (`GL_LIGHTING` + `GL_LIGHT0`) plus a
  basic per-block light level (sky light flood fill) so caves render
  dark. Still immediate mode for now.

## Phase 5 — Infinite world

Closes: rest of #3

- Streaming chunks around the player (8-chunk render distance).
- Chunk generation on a worker thread, mesh upload on the main thread.
- This is where we finally move from `glBegin/glEnd` to VBOs per
  chunk; immediate mode cannot keep up at this scale.

## Phase 6 — Physics & fluids (in progress)

Closes: #8

- Smoother gravity / collision (swept-AABB instead of axis-aligned
  point test). `done` — physics now mirrors vanilla's per-axis
  resolution order (Y, then X, then Z) with the full player AABB clipped
  against every overlapping block, so standing on block edges, landing,
  and ceiling bonks behave like Minecraft.
- Water spread + simple buoyancy / swim controls.
- Lava as a damage source.

## Phase 7 — Multiplayer

Closes: #7

- Client/server split with the existing `Game` becoming the client.
- Authoritative server in a separate process speaking a tiny binary
  protocol over TCP (join, block change, player pose, chat).
- No anti-cheat / no encryption — local LAN only, documented as such.

## Non-goals (for now)

- Survival mobs / AI
- Crafting grid
- Redstone
- Modding API

These are intentionally deferred until Phase 2–4 stabilise.
