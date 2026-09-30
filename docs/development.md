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

Run `./gradlew build` after code or resource changes. GitHub Actions also builds with JDK 25 and uploads `build/libs/`. There are currently no automated gameplay tests.

Before considering a gameplay milestone complete, verify it in a client world and check dedicated-server startup for client class loading errors. For multiplayer-sensitive changes, test two players and confirm anger, stolen items, and effects stay scoped to the correct player. Record actual checks performed; compilation alone does not verify gameplay.
