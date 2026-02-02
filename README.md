# Aqua-Thirst-hunger 🌊🥪

> [!NOTE]
> This project is a **fork** of the original [HytaleHungerMod](https://github.com/Aex12/HytaleHungerMod) by **Aex12**.
> Special thanks to Aex12 for the solid foundation and to the Hytale modding community for their invaluable resources.

---

**Aqua-Thirst-hunger** is an advanced survival mod for Hytale that introduces both **Hunger** and **Thirst** mechanics. It expands upon the original work with a deeper focus on realism, difficulty, and integration with other food mods like *AndieChef*.

## ✨ Key Features (What's New?)

* **Thirst System**: A parallel hydration system with its own HUD, depletion logic, and dehydration effects.
* **AndieChef Integration**: Native support for complex foods (Sushi, Yakimeshi, Sake) with hand-tuned values.
* **Hardcore Balance**: Default values reduced by 75% for a true survival challenge.
* **Fruit Hydration**: Consuming fruits provides a hydration bonus (x5 multiplier).
* **Raw Meat Poisoning**: Eating raw meat now carries a risk of poisoning (Poison_T1 effect).
* **Atomic Config Reload**: Update your settings in-game via `/aquahunger reload` without restarting the server.
* **Native Canteen**: Includes a craftable Canteen (from inventory or cooking workbench) to restore hydration, essential when no other food mods are present.
* **Universal Compatibility**: Support for any food mod; items automatically restore thirst based on their rarity tier unless customized in the config.
* **Enhanced HUD Positioning**: Presets for side-by-side Hunger/Thirst bars (BelowHotbarLeft/Right).
* **Configuration Versioning & Smart Migration**: Robust system (v1.5.0) that automatically updates your config files while preserving your personal customizations and creating backups.

## 🛠️ Dependencies

* **[MultipleHUD](https://github.com/Buuz135/MHUD)**: **Required** for the visual bars to display correctly alongside other HUD elements.

## 📜 Commands

* `/aquahunger`: Main command for hunger management and config reload.
* `/aquathirst`: Management for thirst levels.

---

## Features

* **Hunger Bar**: A visual representation of the player's hunger level displayed on the HUD.
* **Hunger Depletion**: Hunger decreases over time, influenced by actions like stamina usage or mining blocks.
* **Hunger Saturation**: Buffer above 100% providing a delay before depletion starts.
* **Preview Hunger Restoration**: HUD preview of restoration values during the eating animation.
* **Status Effects**: Reduced movement and disabled sprint when hunger is critically low.
* **Starvation Damage**: Health depletion when hunger reaches zero.
* **Safe Areas**: Depletion is paused in designated invulnerable zones.

## Actions affecting hunger

* **Basal Metabolic Rate**: Slow depletion even when idle.
* **Stamina usage**: Sprinting, blocking, and charged attacks accelerate depletion.
* **Hitting blocks**: Small depletion per block hit, reduced by using more efficient tools.

## Configuration

This mod creates configuration files under `mods/Aqua-Thirst-hunger/`.

### Hunger Statistics

* **Maximum Hunger**: 100 (non-configurable).
* **Initial/Respawn Hunger**: Configurable (Default: 50).
* **Starvation Rates**: Customizable via `HungerConfig.json`.

### Food Values

* **Tier-based Restoration**: Configure values for Common through Unique items in `FoodValuesConfig.json` (Hunger) and `ThirstFoodValuesConfig.json` (Thirst).
* **Item Overrides**: Specific values for individual items can be defined in both configuration files.

## Performance

Optimized for high-population servers:

* Distributed updates across ticks.
* Event-driven logic where possible.
* Partial UI updates to minimize TPS impact.

---

## Credits & License

Original code by [Aex12](https://github.com/Aex12). Enhanced by **jume**, **andiemg**, and **antigravity**.
Licensed under GNU Affero General Public License.
