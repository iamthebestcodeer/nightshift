# Development

## Prerequisites and build

Install JDK 25. Check that `java -version` and `./gradlew --version` report Java 25. The wrapper downloads Gradle; Loom downloads Minecraft and Fabric dependencies on the first build, so internet access is required.

Use IntelliJ IDEA 2025.3 or newer. Import the repository as a Gradle project and select JDK 25 as the Gradle JVM. Use `./gradlew genSources` for Minecraft sources. Versions are configured in `gradle.properties`, Java compatibility in `build.gradle`, and runtime requirements in `fabric.mod.json`. Keep these consistent when deliberately changing the target.

Run `./gradlew build` to compile common and client code and produce the mod JAR under `build/libs/`. The JAR without the `-sources` suffix is the installable artifact. Install it alongside Fabric API in a Minecraft 26.3 Fabric instance.

The build runs the Fabric Loader JUnit tests and compiles the client game tests. All Java source sets use `-Xlint:all -Werror`, so compiler lint warnings fail the build. Run `./gradlew runClientGameTest` separately for a real client/integrated-server test that spawns and renders the stationary Understudy, uses the debug commands, saves two UUID records, closes the world, and reopens it to verify the records, phase, and entity.

## Development runs

`./gradlew runClient` launches the development client. `./gradlew runServer` launches a dedicated server. The generated `run/` directory contains local settings, logs, and saves and is ignored by Git. For a server, read the generated EULA and accept it yourself if you agree before relaunching.

Use disposable test worlds when gameplay development begins: the design includes theft and world edits. For multiplayer checks, connect a compatible client to the dedicated server using the same mod and Fabric API versions.

## Source sets and resources

Common/server-safe code goes in `src/main/java`; client-only code goes in `src/client/java`. Never import Minecraft client classes into the common source set. Fabric metadata declares separate entrypoints for these source sets.

Use `Nightshift.id("path")` for identifiers in the `nightshift` namespace. Shared assets belong under `src/main/resources/assets/nightshift/`; server data belongs under `src/main/resources/data/nightshift/` when added.

Add providers to `NightshiftDataGenerator`, then use `./gradlew runDatagen`. The entrypoint currently registers no providers and generates no gameplay data.

## Verification

Run `./gradlew build` after code or resource changes. GitHub Actions also builds with JDK 25 and uploads `build/libs/`. Unit tests cover actual saved-data disk writes/reloads, player and world isolation, mutation tracking, phase serialization, command syntax, and command permissions. The separate client game test exercises the stationary checkpoint in a disposable world.

Before considering a gameplay milestone complete, verify it in a client world and check dedicated-server startup for client class loading errors. For multiplayer-sensitive changes, test two players and confirm anger, stolen items, and effects stay scoped to the correct player. Record actual checks performed; compilation alone does not verify gameplay.

## Checkpoint debug commands

These commands require gamemaster permission (operator level 2), including inspection of hidden state. No gameplay HUD or anger meter is added.

| Command | Behavior |
| --- | --- |
| `/nightshift anger` or `/nightshift anger get [player]` | Inspect anger; defaults to the executing player. |
| `/nightshift anger set <value> [player]` | Set nonnegative integer anger; defaults to the executing player. Console callers must name an online player. |
| `/nightshift phase` | Inspect the saved world phase. |
| `/nightshift phase set <phase>` | Set `oddities`, `watching`, `stalking`, `grudge`, `siege_nights`, `bed_theft`, or `ritual`. These are debug labels without encounter/progression behavior yet. |
| `/nightshift spawn [x y z]` | Spawn a stationary Understudy at the command source or the supplied position; relative coordinates work. |

Vanilla `/summon nightshift:understudy` also works. The entity does not spawn naturally. Theft count and the bed theft flag have a persistence API and tests; actual theft is a later milestone.

Checkpoint verification (2026-10-01): `./gradlew build` passed, including seven unit tests and strict compiler lint. The unit tests include the unknown-phase reload regression, which verifies that player records survive an unrecognized saved phase. `./gradlew runClientGameTest` passed in an actual client/integrated server, including spawn/render dispatch, stationary behavior, debug commands, world reopen, both UUID records, all three player fields, saved phase, and entity reload. The custom skin was also inspected in a game screenshot. `./gradlew runServer` loaded Nightshift without client-class errors but stopped at the unaccepted `run/eula.txt`; full dedicated-server startup remains unverified. Two connected players have not been tested; automated UUID isolation is verified.
