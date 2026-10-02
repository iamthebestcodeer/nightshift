# Architecture and milestones

The [design plan](../plans/horror-mod-design-plan.md) defines behavior. This document describes implementation boundaries, not additional gameplay decisions. The stationary checkpoint, operator state/debug commands, UUID-keyed progression foundation, stalking Understudy, Hollow, Seen, and four command-driven environmental scenes are implemented. The generic look-back face/scream has been removed; rare big scares await a later design. Singleplayer client-world checks have passed; dedicated-server world and multiplayer verification remain pending. Theft and later milestones are unimplemented.

## Agreed stack

- Minecraft 26.x: the newest version offered by the official Fabric generator at project creation. This project targets 26.3; do not automatically upgrade it.
- Java 25 and IntelliJ IDEA 2025.3 or newer.
- Official Fabric template with Fabric API, Java as the language, and split client/server sources.
- Fabric only. No Architectury or NeoForge now; add support only if there is a concrete reason to support NeoForge later.
- No GeckoLib initially. Consider it only if vanilla humanoid animation cannot meet the requirements, most likely for minions.

## Saved state and gameplay logic

Saved data is the foundation. Anger, theft count, the bed theft flag, and other hidden per-player state belong in persistent data keyed by player UUID. World progression builds on that state; permanent banishment is world-wide.

AI and status effects read from this saved state rather than maintaining independent copies of it. The Understudy is the main entity, driven by AI that reacts to the saved state. Persistence must be part of the first stateful feature, not added after parallel systems have accumulated their own state.

Keep gameplay calculations, such as anger calculations and base detection rules, in plain Java classes independent of Fabric APIs. Isolate Minecraft world access and Fabric registration, events, and networking behind integration code. Pure calculations should accept ordinary data rather than directly querying the game world, keeping a future port manageable.

## Performance and Clean Code

Performance and Clean Code, following Robert C. Martin's principles, are extremely important project requirements. Use clear names, focused classes and methods, explicit dependencies, and a single authoritative source for state. Keep gameplay rules separate from integration code and avoid speculative abstractions.

Keep work on the game tick bounded. Avoid scanning entire worlds or repeating expensive base detection every tick; schedule and scope checks, and invalidate cached results when relevant inputs change. Measure expensive behavior in representative worlds before claiming it meets performance requirements. Test meaningful gameplay rules independently where practical.

## Implementation boundaries

| Location | Responsibility |
| --- | --- |
| `nightshift.Nightshift` | Common entrypoint, identifier, logger, future registration wiring |
| `nightshift.entity` | The Understudy and minions |
| `nightshift.effect` | Effect registration and server mechanics |
| `nightshift.world` | Saved world progression, permanent banishment, UUID-keyed player state |
| `nightshift.encounter` | Stalking, anger, theft, midnight actions, base checks, bed theft, ritual coordination |
| `nightshift.item` | Wards, loot, recovery, ritual items after their designs are settled |
| `nightshift.client.NightshiftClient` | Client initialization and future registration wiring |
| `nightshift.client.render` | Appearance and unsettling movement presentation |
| `nightshift.client.effect` | Sound, overlays, flashes, other client presentation |
| `nightshift.client.NightshiftDataGenerator` | Future asset/data generation providers |

The server owns encounters, saved state, and world changes. Clients display what the server authorizes. Keep world-wide progression and permanent banishment separate from per-player anger, effects, and theft/recovery records. Choose save formats and networking payloads when implementing the first feature that needs them, following the saved-state foundation above.

`NightshiftSavedData` stores the world phase and a UUID-keyed map of immutable `PlayerState` records in the overworld's `data/nightshift/state.dat`. Every dimension retrieves that same save through the server. Records contain nonnegative integer anger and theft count plus a bed theft flag, with zero/false defaults. Mutations mark the save dirty; reads do not create records. Plain Java state and phase labels live in `nightshift.encounter`; codecs and Minecraft storage access live in `nightshift.world`. Phase changes currently affect saved labels only, with no automatic progression or banishment behavior.

Passive marked bridge/wall scenes are an approved exception while the actor is not attacking. They place into empty cells only and never break blocks. Centralize block-edit eligibility when implementing sieges: The Understudy may place or break only at midnight and only when angry at its target. Minion wall breaking needs its own explicit rules; minions cannot build or bridge. Do not silently treat these as the same actor.

## Build order

Start with the following sequence, then continue the design plan's build order. Keep each milestone playable before moving to the next.

1. Generate the official template, open it in IntelliJ IDEA 2025.3 or newer, and run `./gradlew runClient`. The template is present, builds, and launches the client; IntelliJ has not yet been verified.
2. Register The Understudy as an entity that simply stands there.
3. Add watch-and-vanish behavior, with AI reading saved state as stateful behavior is introduced.
4. Add Hollow and Seen, then environmental scenes. Rare big jump scares are deferred.
5. Continue with theft and the remaining milestones below.

The stationary entity (build-order step 2) is the first checkpoint within milestone step 1. Its commands and saved state remain available alongside stalking/effects. Dedicated-server worlds and two-client verification remain pending.

| Step | Deliverable | Manual verification focus |
| --- | --- | --- |
| 1 | Stalking Understudy: appears, watches, vanishes, strange movement and sound | Appearance, approach/vanish behavior, server compatibility |
| 2 | Hollow, Seen, marked environmental scenes | Observable tells, passive construction, temporary apparition, quiet tunnel |
| 3 | Theft from open chests | Container eligibility, per-player records, recovery |
| 4 | Minions and basic loot | Roles, no bridging/building, retreat dissolution |
| 5 | Anger and midnight block actions | Both edit conditions, mining/building pace |
| 6 | Base tiers and enclosure checks | Enclosed bases, moat/trench defense, invalid tiny shelters |
| 7 | Bed theft | Fixed release time, limited recoverable theft, defensive counterplay |
| 8 | Permanent banishment, remaining effects, polish | Save/reload permanence, agreed ritual and cleanup behavior |

## Decisions needed before dependent implementation

Ask the project owner whenever a requirement is unclear. The plan does not yet settle:

- Items labeled **(proposal)**: copied-player skin, ritual details, and post-banishment cleanup.
- Ritual ingredients, their sources, failure behavior, and what remains after banishment.
- Midnight window, anger thresholds/decay, and later phase timing.
- What an “open” container means and how recoverable stolen items are assigned.
- Later effect durations/stacks/tells, minion stats/drops, and enclosure dimensions/material scoring.
- Later rendering, sounds, and art assets beyond the first two milestones.

Do not invent these values during setup or interpret proposals as accepted decisions.

## Approved first-playable settings

The owner approved these settings for stalking and the first two effects:

- Independent per-player encounters, with a fixed pale humanoid skin and vanilla sounds. Any nearby observer freezes movement; Hollow and Seen are scoped to the target. Physical entities and constructed blocks can be seen by nearby players.
- Sightings every 3–5 minutes, reduced to 2–3 minutes by Seen. A sighting lasts at most 30 seconds and vanishes when its target approaches within 8 blocks. Only loaded chunks are considered; failed placement retries after 20 seconds. This milestone uses surface locations near the player's elevation.
- Head snaps, a rigid body, and movement only when unseen. Collision and support checks prevent walking through blocks or over edges. Passive scene construction is the explicit exception; siege block editing and advanced navigation belong to later milestones.
- Hollow lasts 30 seconds, reduces world audio to a quarter of its normal gain, and produces faint positional phantom noises. Seen lasts 90 seconds, has a faint heartbeat tell, and shortens the next sighting's deadline. Neither effect stacks in this milestone.
- The generic face overlay and scream are removed. Big jump scares will be designed later and used only a few times; no count or replacement presentation has been chosen.
- Marked areas and operator test commands identify the first bridge, wall, and base scenes. No automatic detection or architectural improvement is implemented. Wall corners specify the desired finished plane; the replica reference is an explicitly prepared finished design.
- Bridges are horizontal cardinal lines of at most 64 cells, built at up to two cells per tick even while watched. Walls are vertical planes, at most 32 cells wide and 16 high. Both use an explicit inert-masonry allowlist and place into air only, retain existing blocks, stop immediately on aggression or an attack target, and leave completed blocks in the world. Their actor stops at completion, turns toward the player, and retreats within two blocks or after 30 seconds.
- The base copy is a non-colliding, temporary visual apparition of the marked block states. It neither changes the destination nor copies inventories or other block-entity data. Replica dimensions are limited by volume rather than wall width/height. The reference volume is capped at 4096 cells and the visible copy at 512 non-air cells. It expires after 30 seconds or when its owner approaches within eight blocks of its first visible block. Its render distance includes the geometry’s extent, and tracking is anchored to actual visible geometry rather than an empty marked corner.
- The quiet tunnel command chooses supported empty space 4–6 blocks behind the player. The actor stays still with its arms at its sides, then leaves within two blocks or after 30 seconds. Automatic mining/tunnel triggers are deferred.
- Encounter deadlines are saved by UUID in server saved data across dimensions. Active entities are temporary and are not saved; reserved deadlines survive reconnects and world reloads. The old scare deadline remains in the save format for compatibility but no longer drives gameplay. Normal status-effect persistence uses Minecraft's player data. Long-term anger, theft, progression, and banishment are still outside this milestone.

Debug `/nightshift spawn` and vanilla summon create persistent stationary checkpoint entities. Encounter-owned entities are unsaved and retreat normally. Encounter deadlines use their separate UUID-keyed encounter save; anger, theft, and phase remain authoritative in `NightshiftSavedData`. Phase commands change saved labels without gating the approved first-playable encounter scheduler.
