# Nightshift

A Minecraft horror mod about **The Understudy**, a silent, player-shaped rival that stalks, steals, builds, and holds a grudge. Players learn its rules, defend their base, and eventually banish it permanently.

The [design plan](plans/horror-mod-design-plan.md) is the source of truth. This repository currently contains the development foundation only; gameplay milestones have not been implemented. Items marked **(proposal)** remain undecided.

## Development

- Minecraft **26.3**, Fabric Loader **0.19.5**, Fabric API **0.161.0+26.3**
- **JDK 25**; included Gradle wrapper **9.7.1**
- Build: `./gradlew build`
- Development client: `./gradlew runClient`
- Development server: `./gradlew runServer`

On Windows, use `gradlew.bat`. Build artifacts are written to `build/libs/`.

See [development instructions](docs/development.md) for IDE setup and verification, and [architecture and milestones](docs/architecture.md) for implementation boundaries.

## License

[CC0-1.0](LICENSE).
