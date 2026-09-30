# Architecture and milestones

The [design plan](../plans/horror-mod-design-plan.md) defines behavior. This document describes implementation boundaries, not additional gameplay decisions. Package scaffolding contains documentation only; no entities, effects, items, event handlers, or persistence are registered yet.

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

Centralize block-edit eligibility when implementing sieges: The Understudy may place or break only at midnight and only when angry at its target. Minion wall breaking needs its own explicit rules; minions cannot build or bridge. Do not silently treat these as the same actor.

## Build order

Start with the following sequence, then continue the design plan's build order. Keep each milestone playable before moving to the next.

1. Generate the official template, open it in IntelliJ IDEA 2025.3 or newer, and run `./gradlew runClient`. The template is present and builds; IntelliJ and client launch have not yet been verified.
2. Register The Understudy as an entity that simply stands there.
3. Add watch-and-vanish behavior, with AI reading saved state as stateful behavior is introduced.
4. Add Hollow and Seen, plus one jump scare.
5. Continue with theft and the remaining milestones below.

The gameplay milestones below are all unimplemented. The stationary entity is the first checkpoint within step 1.

| Step | Deliverable | Manual verification focus |
| --- | --- | --- |
| 1 | Stalking Understudy: appears, watches, vanishes, strange movement and sound | Appearance, approach/vanish behavior, server compatibility |
| 2 | Hollow, Seen, one jump scare | Observable tells, effect behavior, calm gaps and cooldown |
| 3 | Theft from open chests | Container eligibility, per-player records, recovery |
| 4 | Minions and basic loot | Roles, no bridging/building, retreat dissolution |
| 5 | Anger and midnight block actions | Both edit conditions, mining/building pace |
| 6 | Base tiers and enclosure checks | Enclosed bases, moat/trench defense, invalid tiny shelters |
| 7 | Bed theft | Fixed release time, limited recoverable theft, defensive counterplay |
| 8 | Permanent banishment, remaining effects, polish | Save/reload permanence, agreed ritual and cleanup behavior |

## Decisions needed before dependent implementation

Ask the project owner whenever a requirement is unclear. The plan does not yet settle:

- Items labeled **(proposal)**: copied-player skin, ritual details, post-banishment cleanup, multiplayer target selection.
- Ritual ingredients, their sources, failure behavior, and what remains after banishment.
- Midnight window, anger thresholds/decay, phase timing, encounter frequency, and cooldowns.
- What an “open” container means and how recoverable stolen items are assigned.
- Effect durations/stacks/tells, minion stats/drops, and enclosure dimensions/material scoring.
- Rendering, skin, sounds, and art assets needed for the first playable milestone.

Do not invent these values during setup or interpret proposals as accepted decisions.
