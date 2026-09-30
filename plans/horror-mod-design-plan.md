# Nightshift: Design Plan

**Mod:** Nightshift  
**Entity:** The Understudy

Items marked **(proposal)** are my suggestions, not decisions. Change or cut them freely.

## The Pitch

A player-shaped thing lives in your world, is better at Minecraft than you, and holds a grudge. It watches, builds, steals, and breaks into your base, but only under strict rules you can learn. Minions are the everyday danger, and your base is the answer.

- **Target:** singleplayer first, multiplayer-friendly
- **Ending:** it can be banished permanently through a ritual that needs hard-to-get items. Until then it never stops.

---

## The Entity: The Understudy

- **Look:** a player model that moves wrong. Jerky head snaps, a stride that's too smooth or too fast, standing perfectly still for too long, turning to face you without moving its feet.
- **(proposal)** A skin that's slightly off from your own, so it reads as a worse copy that became better.
- **Never speaks.** It communicates only through builds, stolen items, and effects.
- **Competent:** never gets stuck, full gear, builds like a skilled player.

## Core Rules

1. It can only place or break blocks at **midnight**, and only when **angry at you**.
2. Outside that, it can still stalk, steal from open containers, apply effects, and summon minions.
3. It mines and builds at roughly player speed. A stone base falls quickly; a netherite house takes most of a night.
4. **Anger** is per player. It rises when you hurt its minions, use wards, or wreck its builds. It fades slowly over days of quiet. Players never see a meter, only behavior changes.
5. **Banishment:** it cannot be killed, but a ritual using hard-to-get items sends it away **forever**. This is the win condition.
6. **(proposal)** The ritual is a late-game project: rare minion loot plus items from other dimensions or bosses, performed at a specific spot at midnight, when it's angry and close. Attempting it should be dangerous.
7. **(proposal)** After banishment, its minions dissolve, its effects fade, and its builds stay as ruins.

---

## Phases of a World

1. **Oddities (early):** tidy tunnels, extra torches, a rearranged chest. No sightings yet.
2. **Watching:** it stands far off and vanishes if approached. First effects appear.
3. **Stalking:** it follows just outside your light and moves when you look away. Jump scares begin, with long calm gaps.
4. **Grudge:** minions appear, theft starts, anger matters.
5. **Siege nights:** angry midnights where it builds toward your base.
6. **Bed theft set piece:** once or twice per world.
7. **The ritual:** the player gathers the rare items and performs the banishment, ending it for good.

---

## Minions

- **Scout:** fast and weak. Reports your position and feeds the *Seen* effect.
- **Breaker:** slow and tanky. Tears at weak walls.
- **Ranged:** applies a mild effect on hit.
- **They cannot bridge or build**, so trenches and moats stop them.
- **They are the entity's hands:** summoned when you're Seen or Owed, and they dissolve when it retreats.
- **Loot:** healing items, materials for wards and the banishment ritual (some ritual items only drop from minions), and one rare drop that shortens a single effect.
- Fighting them in the dark stacks effects, so killing them is a choice.

---

## Status Effects

Cryptic names, no descriptions. Players learn them by experience and wait them out. Stacks increase strength and duration, so avoiding hits matters. Each effect needs a subtle tell so nothing feels random.

| Type | Effect | What it does |
|---|---|---|
| Senses | **Hollow** | Muffled sound, phantom noises from empty directions |
| Senses | **Static** | Visual noise at screen edges, worse in the dark |
| Physical | **Heavy** | Slightly slower, weaker mining |
| Physical | **Slip** | Small chance to drop the held item |
| Mental | **Whisper** | Fake chat messages, faint sound cues |
| Mental | **Flicker** | Brief screen flashes, small FOV wobble |
| Marked | **Seen** | It finds you faster and more often |
| Marked | **Owed** | It steals from you more, and takes better things |

---

## Base Defense Tiers

1. **Wood or open:** minions walk in, theft is easy.
2. **Stone plus moat or trench:** minions stall at the edge.
3. **Iron or deepslate, moat, and light:** minions mostly give up.
4. **Netherite house:** effects apply slowly, and the entity needs most of a night to break in.

Notes:
- A base must be a real enclosed space with a minimum size, so a 1x1 pillar or lava trick doesn't count.
- Unfinished breaches at dawn are a deliberate scare and a sign it can be beaten.

---

## Set Pieces

### Jump Scares
- Rare, earned by long tension, with a cooldown after each.
- Vary the triggers: mining alone, sleeping, looking at a crafting table.
- Silence before, a sting, then silence again.

### Bed Theft
- Happens once or twice per world.
- The player wakes paralyzed, watches the entity walk in, take a couple of items, and leave. Completely passive.
- Telegraphed by earlier signs, capped in what it takes, and the items are recoverable.
- A ward or a sealed, fortified room can cancel or weaken it.
- The player is always released after a fixed time.

## Recovering Stolen Items

Stolen gear ends up somewhere it built (a neat room, a hidden chest). Finding it is a mini-quest that gets harder the more it has taken.

---

## Singleplayer First, Multiplayer Friendly

- Design everything around one player, then make anger, theft, and effects per player.
- **(proposal)** In multiplayer it targets one player at a time, and shared bases follow the anger of whoever it's currently after.

---

## Build Order

Each step should be playable on its own.

1. Stalking entity: appears, watches, vanishes, with its weird movement and sound
2. Two effects (Hollow and Seen) and one jump scare
3. Theft from open chests
4. Minions with basic loot
5. Midnight and anger rules, with block breaking and building
6. Base tiers and the enclosure check
7. Bed theft set piece
8. Banishment ritual, remaining effects, polish and sound

---

## Open Questions

- [ ] Which exact items does the ritual need, and where do they come from?
- [ ] Does the ritual have a failure state (angry entity, minion wave)?
- [ ] After banishment, does anything remain (ruins, lore items, a trophy)?

### Settled
- Banishment is a ritual with hard-to-get items, and it is permanent.
- Anger is shown only through behavior, with no meter or explicit sign.
- Names: Nightshift (mod), The Understudy (entity).
