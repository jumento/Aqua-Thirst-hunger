# Changelog

## 1.1.0 (unreleased)

Port to Hytale server 0.7.x. Requires a 0.7.2 or newer 0.7 server; 1.0.0 does not load on 0.7.

### Changed
- HUDs use Hytale's native multi-HUD support. The old HUD bridge (AutoMultiHUD / MultipleHUD
  support and the packet interception that kept other mods' HUDs from wiping ours) is gone:
  no HUD manager mod is needed and other mods' HUDs are left alone.
- HUD position and config changes apply immediately; no server restart.
- The bars are hidden in spectator game modes.
- `/aquahunger hide` and `/aquahunger show` let a player hide their own bars.
- The thirst bar and the stamina boost use the player's real maximum thirst (the base
  `MaxThirst` plus the level bonus from integrations), which the bar used to cut off at 100.
- Config pages keep decimals (a value of `1.5` used to be saved as `1` when closing the page),
  accept `0,5` as well as `0.5`, apply valid fields even if another field is invalid, and are
  translated (English/Spanish).
- The server log now shows integration status and config problems; `EnableDebugLogs` in
  `ThirstConfig.json` adds verbose diagnostics.

### Fixed
- `ExternalFoodsDefaultConfig.json` (bundled values for foods from other mods) was missing
  from the JAR, so new installs had no defaults for them.
- `MaxHungerLevel` was ignored (the cap was always 200), so the Hard preset's 150 never applied.
- `DamageIntervalSeconds` was ignored; dehydration damage followed the depletion tick instead.
  Existing configs are migrated so the damage cadence stays the same.
- Standing still made all hunger/thirst costs 10x cheaper, including mining and sprinting in
  place. Now only the passive metabolism slows down, after 5 seconds without moving.
- Being below the stamina-boost threshold removed the vanilla fruit/vegetable buff the player
  got from eating; only the boost given by the mod is removed now.
- Life-per-hunger kept spending thirst while thirst was disabled, until healing stopped.
- The HUD could keep showing an old value for several seconds after eating or drinking.
- The food editor (`/aquacheff`) did not pre-fill the item in hand.
- A config file with `"ConfigVersion": null` or invalid JSON stopped the mod from loading;
  invalid files are now copied to `<name>.invalid.bak` and defaults are used.
- Mod items whose ID contains "Canteen" from other mods were forced to restore no hunger.

### Config migration
- `ConfigVersion` goes from `1.7.5` to `1.8.0`, with a `.bak` copy of each migrated file.
  Values out of range (for example a tick rate of 0) are clamped and logged.
