# Changelog para pegar en CurseForge al subir la 1.1.2

La política de moderación de CurseForge exige que cada archivo lleve changelog. Pega el texto de abajo
en el campo **Changelog** del archivo (tipo Markdown) y elige la versión de juego **0.7**.

---

## Aqua Thirst & Hunger 1.1.2 — Hytale 0.7 update

Requires Hytale server **0.7.5 or newer** (0.7.x). Version 1.0.0 does not load on Hytale 0.7.

### What's new since 1.0.0
- **Works on Hytale 0.7.** The bars use the game's native multi-HUD support, so AutoMultiHUD and
  MultipleHUD are no longer needed and other mods' HUDs are left alone.
- HUD position and config changes apply right away, without a restart. The bars hide in spectator
  mode. New commands `/aquahunger hide` and `/aquahunger show`.
- **RPG Leveling (0.4.x):** the integration now connects: XP, lower consumption as you level, and a
  level-up mark on the bars.
- **Endless Leveling (14.x):** detected again under its new plugin ID.
- **MMO Skill Tree (1.7.x):** no more double restart, and skill tree rewards and Glutton items work
  again.
- New command `/aquahunger integrations`: shows which of these mods are connected.

### Fixes
- `MaxHungerLevel` and `DamageIntervalSeconds` are now applied. Existing configs are migrated, keep
  behaving the same and get a `.bak` copy.
- Standing still no longer makes mining or sprinting 10x cheaper; only passive drain slows down.
- The stamina boost no longer removes the vanilla fruit/vegetable buff.
- Life-per-hunger no longer spends thirst while thirst is disabled.
- The bars update right after eating or drinking.
- Config pages keep decimals and check each field on its own.
- Default values for foods from other mods are bundled again.
- THIRST skill tree: the "Super Healthy" reward now works.

### Note for moderators
Source code: https://github.com/jumento/Aqua-Thirst-hunger (branch `update/hytale-0.7`). The JAR is a
plain Gradle build and is not obfuscated. It uses reflection only to talk to three optional mods
(RPG Leveling, Endless Leveling, MMO Skill Tree) without requiring them. When MMO Skill Tree is
installed, it writes its two custom skills into that mod's own config folder (`mods/mmoskilltree`),
which is how MMO Skill Tree supports custom skills.
