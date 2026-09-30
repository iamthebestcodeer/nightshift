# Nightshift project instructions

## Before changes

- Gameplay: read `plans/horror-mod-design-plan.md` for behavior and `docs/architecture.md` → Build order for the stationary-entity checkpoint and subsequent milestones. Work within the requested milestone.
- Uncertainty: ask the owner about anything unclear before implementing the dependent change. Items marked `(proposal)` require an owner decision before implementation.
- Architecture: before changing persistence, progression, AI, effects, entities, or gameplay calculations, read `docs/architecture.md` → Saved state and gameplay logic and Implementation boundaries. These define the saved-data foundation and separation of plain Java rules from game integration.
- Dependencies: before changing the Minecraft target, loader, Java version, or animation libraries, read `docs/architecture.md` → Agreed stack. Changes to that stack require an owner decision.

## Code quality

Performance and Clean Code, following Robert C. Martin (Uncle Bob), are extremely important. Before writing or reviewing code, read `docs/architecture.md` → Performance and Clean Code and apply its rules to every changed class and game-tick path.

## Design preference

The owner prefers minimalism in code and especially in UI. Write the simplest clear implementation that meets the requirements, with only necessary abstractions and dependencies. For UI, favor simple layouts, restrained visuals, concise text, and only essential controls and information.

## Verification

- Setup, source sets, resources, or game runs: read `docs/development.md` for the relevant workflow.
- Code or resource changes: run `./gradlew build`. Completion requires a passing build or an explicit report of the failure and remaining work.
- Gameplay milestones: perform the applicable client, dedicated-server, and per-player checks in `docs/development.md` → Verification before marking the milestone playable. Report which checks passed and which remain unverified; distinguish compilation, automated tests, and actual game runs.
