# Development

## Prerequisites and build

Install JDK 25. Check that `java -version` and `./gradlew --version` report Java 25. The wrapper downloads Gradle; Loom downloads Minecraft and Fabric dependencies on the first build, so internet access is required.

Use IntelliJ IDEA 2025.3 or newer. Import the repository as a Gradle project and select JDK 25 as the Gradle JVM. Use `./gradlew genSources` for Minecraft sources. Versions are configured in `gradle.properties`, Java compatibility in `build.gradle`, and runtime requirements in `fabric.mod.json`. Keep these consistent when deliberately changing the target.

Run `./gradlew build` to compile common and client code and produce the mod JAR under `build/libs/`. The JAR without the `-sources` suffix is the installable artifact. Install it alongside Fabric API in a Minecraft 26.3 Fabric instance.

## Development runs

`./gradlew runClient` launches the development client. `./gradlew runServer` launches a dedicated server. The generated `run/` directory contains local settings, logs, and saves and is ignored by Git. For a server, read the generated EULA and accept it yourself if you agree before relaunching.

Use disposable test worlds when gameplay development begins: the design includes theft and world edits. For multiplayer checks, connect a compatible client to the dedicated server using the same mod and Fabric API versions.

## Source sets and resources

Common/server-safe code goes in `src/main/java`; client-only code goes in `src/client/java`. Never import Minecraft client classes into the common source set. Fabric metadata declares separate entrypoints for these source sets.

Use `Nightshift.id("path")` for identifiers in the `nightshift` namespace. Shared assets belong under `src/main/resources/assets/nightshift/`; server data belongs under `src/main/resources/data/nightshift/` when added.

Add providers to `NightshiftDataGenerator`, then use `./gradlew runDatagen`. The entrypoint currently registers no providers and generates no gameplay data.

## Verification

Run `./gradlew build` after code or resource changes. GitHub Actions also builds with JDK 25 and uploads `build/libs/`. The build retains the foundation’s Fabric Loader JUnit tests and strict `-Xlint:all,-classfile -Werror` compilation (the external JOML legacy classfile annotation warning is exempt; source warnings remain errors), and also runs dependency-free checks for encounter timing and visibility boundaries, save serialization, and per-player data isolation via `gameplayRulesTest`. These checks do not replace in-world verification.

Before considering a gameplay milestone complete, verify it in a client world and check dedicated-server startup for client class loading errors. For multiplayer-sensitive changes, test two players and confirm anger, stolen items, and effects stay scoped to the correct player. Record actual checks performed; compilation alone does not verify gameplay.

## Stalking and effects verification

Use a disposable, reasonably level outdoor test world. The current spawn search uses loaded surface terrain within 24–36 blocks and within 12 blocks of the player's elevation, so underground sightings are outside this milestone.

1. Wait 3–5 minutes for the first sighting. Look toward it and confirm the pale humanoid stands still, its head snaps independently, and Hollow/Seen appear without stacking. Look away and back to check unseen movement. Approach within 8 blocks, or wait 30 seconds, to check disappearance.
2. Check Hollow's reduced world audio and empty-direction sounds for 30 seconds. Check Seen's faint heartbeat for 90 seconds and the next sighting arriving 2–3 minutes after the previous one. `/effect give @s nightshift:hollow 30` and `/effect give @s nightshift:seen 90` can isolate audio/effect checks in a world with commands enabled.
3. Use the marked scene commands below to test passive bridge/wall construction, the distant visual copy, and the quiet tunnel encounter. Existing blocks must survive construction, and builders must stop on aggression or an attack target. A long watch look-back now remains a quiet encounter; the face overlay and scream are removed.
4. Save and reload during a calm gap and confirm the encounter deadline persists. Test dimension changes, resource reloads, effect expiration, and leaving the world to ensure audio returns normally.
5. On a dedicated server with two clients, confirm effects stay with their target and both players freeze ordinary unseen movement by watching. Verify independent encounter deadlines and owner cleanup. Scene geometry is shared visually; only its owner's proximity causes retreat.

Earlier singleplayer checks passed natural scheduling, visible freezing, unseen movement, head turns, approach/timeout, Hollow/Seen application and expiration, sound-engine gain/playback, resource reload, dimension retreat, and actual save/reopen. The previous generic scare checks are obsolete following its removal. Sound quality has not been assessed by listening.

`runServer` initialized Nightshift without client class loading errors, then stopped because `run/eula.txt` is unaccepted. Dedicated-server world startup and actual two-client multiplayer checks remain unverified, so the milestones are not marked fully verified as playable.

Run `./gradlew runClientGameTest` to repeat the real-client tests. They create isolated flat worlds under `build/run/clientGameTest/saves/`, drive player input, and save screenshots under `build/run/clientGameTest/screenshots/`. The initial calm gap is real, so the complete run takes several minutes. The test mod lives only in `src/gametest/` and is excluded from the installable JAR. Ordinary builds compile this harness but do not launch a GUI. This session's successful logs are retained under `build/reports/nightshift-inworld/`.

## In-game test commands

Enable commands in a singleplayer test world, or use operator permission level 2 on a server. Run the commands as the player being tested. `/nightshift test` lists the available actions.

| Command | Action |
| --- | --- |
| `/nightshift test watch` | Replaces your nearby sighting with one ahead on safe loaded surface terrain. It moves while unseen and leaves within eight blocks or after 30 seconds. |
| `/nightshift test effects` | Applies fresh, unstacked Hollow for 30 seconds and Seen for 90 seconds. |
| `/nightshift test bridge <from> <to> <block>` | Builds a horizontal cardinal line, up to 64 cells, at up to two cells per tick. Coordinates identify the bridge floor. |
| `/nightshift test wall <from> <to> <block>` | Completes the marked vertical rectangle, up to 32 cells wide and 16 high, filling air only. |
| `/nightshift test replica <from> <to> <destination>` | Shows a temporary visual copy of the marked reference at the destination minimum corner. Up to 4096 source cells and 512 non-air shapes; wall width/height limits do not apply. |
| `/nightshift test tunnel` | Places a stationary actor in clear supported space 4–6 blocks behind you; leaves when you approach within two blocks. |
| `/nightshift test clear` | Removes your nearby actors, apparitions, Hollow, and Seen. Completed construction blocks remain. |

Example commands in a flat test world (ground surface at Y=-61):

```mcfunction
/nightshift test bridge -8 -61 15 8 -61 15 minecraft:stone
/nightshift test wall -4 -60 10 4 -57 10 minecraft:stone
/nightshift test replica -4 -60 10 4 -57 10 -4 -60 30
/nightshift test tunnel
/nightshift test clear
```

Carve a ravine under the bridge in a disposable world first; existing ground cells are deliberately preserved. Adapt coordinates to your world, or use relative coordinates. Bridge/wall materials are restricted to inert masonry: stone, cobblestone, stone bricks, bricks, deepslate, cobbled deepslate, polished deepslate, deepslate bricks, and deepslate tiles. TNT, sponge, pumpkins, and other blocks with placement side effects are rejected. Mark the desired finished wall rectangle; the command fills missing blocks rather than guessing your design. For a rebuilt base, prepare the finished reference yourself and mark its bounding box. The apparition copies block states, with no inventories, sign text, or other block-entity data; it has no collision and disappears after 30 seconds or within eight blocks of its first visible block.

All scene cells must be loaded, inside world bounds, and within 96 blocks of you. Repeated actor scenes replace your previous nearby actor; repeated replicas replace your previous nearby apparition. Scene actors last up to 30 seconds and retreat within two blocks. These are explicit commands, with automatic ravine/build/base/tunnel detection deferred. Passive construction may run outside midnight only while not attacking; it never overwrites or breaks blocks. Occupied cells are retried within the actor’s lifetime, and construction retreats before placing when the owner is already close. Builder spawn and movement require clear body space. `clear` does not undo placed blocks.

`watch` reserves the next normal encounter deadline; use disposable worlds. Actions target the command player; operators can use `/execute as <player> run nightshift test <action>`. Living non-spectators can start scenes. If placement fails, move to a loaded clear supported area.

The command regression drives actual client camera input through visible freezing, unseen movement, and a ten-second quiet look-back. The owner's earlier watch mismatch remains unreproduced in the flat first-person test world; this test makes those expectations explicit.

Current focused verification passed `./gradlew build` (including geometry, timing, saved-data, and data-isolation rules) and real singleplayer client-world commands for bridge completion/straightness, protection of existing blocks, wall completion, construction cancellation on aggression, actor replacement, replica synchronization without physical edits, 30-second expiry, approach disappearance, repeat replacement/clear, and the tunnel look-back/approach. The quiet watch command regression also passed. Screenshots were inspected for all four scenes. Isolating the apparition exposed a distance-culling bug; an explicit render range and regression check fixed it. The final scene log is `build/reports/nightshift-inworld/environmental-scenes.log`, and the watch command log is `build/reports/nightshift-inworld/environmental-scenes-first.log`.

These small flat-world tests do not establish performance in representative complex worlds. Actual two-client isolation and a dedicated-server world remain unverified.

Bug regression checks also passed in actual client worlds: destructive material rejection beside redstone, collision-safe initial spawn and per-tick construction movement, retreat before any placement, occupied-cell retry, a 40-cell replica reference, a sparse replica with a distant empty marked corner reaching the client, and unsupported ocean sightings being rejected. Presentation checks inspect actual sound-engine category gains after stop/reload, resource reload, dimension change, and Hollow removal. A client-only sound-engine reset hook invalidates the cached gain, including while paused. The head-turn harness now waits for an actual server head-snap tick. Logs: `build/reports/nightshift-inworld/bug-regressions-construction.log` (scene checks passed; its later presentation check found the harness timing issue) and `build/reports/nightshift-inworld/bug-regressions-audio.log` (presentation rerun passed).

## Checkpoint debug commands

These commands require gamemaster permission (operator level 2), including inspection of hidden state. No gameplay HUD or anger meter is added.

| Command | Behavior |
| --- | --- |
| `/nightshift anger` or `/nightshift anger get [player]` | Inspect anger; defaults to the executing player. |
| `/nightshift anger set <value> [player]` | Set nonnegative integer anger; defaults to the executing player. Console callers must name an online player. |
| `/nightshift phase` | Inspect the saved world phase. |
| `/nightshift phase set <phase>` | Set `oddities`, `watching`, `stalking`, `grudge`, `siege_nights`, `bed_theft`, or `ritual`. These are debug labels without encounter/progression behavior yet. |
| `/nightshift spawn [x y z]` | Spawn a stationary Understudy at the command source or the supplied position; relative coordinates work. |

Vanilla `/summon nightshift:understudy` also works. Operator/vanilla summons remain stationary and persistent; normal per-player sightings now spawn through the encounter scheduler. Theft count and the bed theft flag have a persistence API and tests; actual theft is a later milestone.

Checkpoint verification (2026-10-01): `./gradlew build` passed, including seven unit tests and strict compiler lint. The unit tests include the unknown-phase reload regression, which verifies that player records survive an unrecognized saved phase. `./gradlew runClientGameTest` passed in an actual client/integrated server, including spawn/render dispatch, stationary behavior, debug commands, world reopen, both UUID records, all three player fields, saved phase, and entity reload. The custom skin was also inspected in a game screenshot. `./gradlew runServer` loaded Nightshift without client-class errors but stopped at the unaccepted `run/eula.txt`; full dedicated-server startup remains unverified. Two connected players have not been tested; automated UUID isolation is verified.

After merging the stationary-checkpoint foundation from `master`, the build passed its seven JUnit tests plus encounter rules. A combined real-client checkpoint/watch run passed debug-command behavior, both player-state records, phase/entity save and reopen, renderer dispatch, and watching commands. Its log is `build/reports/nightshift-inworld/master-merge-checkpoint-watch.log`. All five client test entrypoints are retained.
