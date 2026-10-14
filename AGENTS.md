# Adaptive Bosses — project instructions

**Status:** Level 4 project pointer; not a copy of the Constitution.
**Canonical Constitution:** https://github.com/MrWizard94-Compile/Agents-Constitution
**Accepted source commit:** `69830722d809244e1dfe2f6cd33f0e7dbbaacf5c`

Read `SKILL.md`, `AGENTS.md`, `SOP.md`, and applicable `law/*` files from that
same verified commit before working here. A generated local mirror is usable
only if its provenance manifest matches. Project guidance below supplies
local facts and may tighten, but not silently weaken, canonical law.

## Mission and stack

This repository builds the `adaptiveboss` NeoForge mod. Its declared target
is Minecraft 1.21.1, Java 21, and Gradle 8.14. Read the exact NeoForge and
mod versions at the current commit from `gradle.properties` before changing
dependencies or APIs; also inspect `build.gradle` and
`gradle/wrapper/gradle-wrapper.properties`. The mod's boss-detection,
adaptation, and combat behavior lives under
`src/main/java/com/adaptiveboss/`; its GameTests and unit tests live under
`src/gametest/` and `src/test/`.

## Verification and acceptance

- On Windows, run `.\gradlew.bat test --warning-mode all` for unit tests and
  `.\gradlew.bat check --warning-mode all` for the configured Checkstyle,
  coverage, and GameTest gates.
- On Linux, use `bash ./gradlew` with the same tasks and flags; the tracked
  wrapper is not marked executable.
- `check` includes `runGameTestServer` through `build.gradle`; inspect its
  fresh result rather than inferring GameTest success from unit tests.
- Build/test and GameTest results are source/runtime evidence, not fresh
  interactive client or pack-compatibility acceptance. A release needs the
  latter in the exact intended instance.
- Run the canonical `tools/check-adoption.py` against this Git root after
  changing pointers or workflow source pins.

The tracked pre-pack SOUL variant is historical, not active law. Historical
recovery (not active law): `git show 1d2c78b92cdb2b9d6ccb92d289abc97a5db3813a:.claude/SOUL.md.legacy-pre-pack-20260727-205752`
