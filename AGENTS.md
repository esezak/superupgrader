# Developer & Agent Guide: Super Upgrader

This document serves as the technical architecture reference, code map, and developer guide for **Super Upgrader**. It is intended for human contributors, maintainers, and AI coding agents working on this codebase.

For the player-facing guide, features overview, and installation instructions, see [README.md](README.md).

---

## 1. Project Overview & Environment

Super Upgrader is a Fabric mod for modern Minecraft that introduces an item upgrade station. Players place an item stack into the station and attempt to upgrade it toward a selected target item based on relative material valuation.

- **Minecraft Version:** `26.2`
- **Fabric Loader:** `>=0.19.0` (Configured: `0.19.5`)
- **Fabric API:** `0.160.0+26.2`
- **Java Version:** `25`
- **Build System:** Gradle with Loom (`1.17-SNAPSHOT` / `1.17.20`)
- **Upstream Origin:** Clean-room Fabric port and modernization of *Upgrader 1.2.0* (originally created by **EXEcheINZ** for Minecraft 1.21.11).

---

## 2. Code Map & Repository Structure

```text
superupgrader/
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions CI build & verification workflow
├── gradle/                         # Gradle wrapper binaries & configuration
├── src/
│   ├── main/
│   │   ├── java/com/esezak/superupgrader/
│   │   │   ├── SuperUpgrader.java            # Mod entrypoint; registers items, menus, network, & reload listeners
│   │   │   ├── config/
│   │   │   │   └── ModConfig.java            # JSON configuration reader/writer (config/superupgrader.json)
│   │   │   ├── gui/
│   │   │   │   ├── UpgraderInventory.java    # Single-slot input inventory container
│   │   │   │   └── UpgraderScreenHandler.java# Server container menu; owns slots, wager validation, & payouts
│   │   │   ├── item/
│   │   │   │   ├── ModItems.java             # Item registry (superupgrader:upgrader)
│   │   │   │   └── UpgraderItem.java         # Portable station item; handles right-click to open menu
│   │   │   ├── logic/
│   │   │   │   ├── CooldownTracker.java      # Per-player wager cooldown state manager
│   │   │   │   ├── SpinResult.java           # Server-side RNG roll record & serialization
│   │   │   │   └── UpgradeCalculator.java    # Mathematical odds formula (house edge, clamping)
│   │   │   ├── mixin/
│   │   │   │   └── ExampleMixin.java         # Server-side mixin placeholder
│   │   │   ├── network/
│   │   │   │   └── ModNetworking.java        # Custom packet payloads (C2S & S2C) registration & handling
│   │   │   └── value/
│   │   │       ├── BaseValues.java           # Seed price constants for foundational Minecraft materials
│   │   │       ├── DurabilityAdjuster.java   # Scaling formula for damaged items (down to 5% floor)
│   │   │       ├── ItemExclusionFilter.java  # Hardcoded & blacklisted item/class filters
│   │   │       ├── ItemValueCache.java       # Server-side cached valuation map & sorted target list
│   │   │       └── RecipeGraphWalker.java    # Multi-pass recipe propagation engine
│   │   └── resources/
│   │       ├── assets/superupgrader/
│   │       │   ├── icon.png                  # Mod icon
│   │       │   ├── items/upgrader.json       # Modern 1.21.4+ item model definition
│   │       │   ├── lang/*.json               # Multilingual translations (21 locales supported)
│   │       │   ├── models/item/upgrader.json # Item model JSON
│   │       │   └── textures/item/upgrader.png# Super Upgrader station item texture
│   │       ├── data/superupgrader/advancement/recipes/
│   │       │   └── upgrader_item.json        # Unlocks the recipe on the player's first server tick
│   │       ├── data/superupgrader/recipe/
│   │       │   └── upgrader_item.json        # Shaped crafting recipe (4 Gold, 4 Diamonds, 1 Anvil)
│   │       ├── fabric.mod.json               # Fabric mod metadata
│   │       └── superupgrader.mixins.json     # Common mixin configuration
│   └── client/
│       ├── java/com/esezak/superupgrader/
│       │   ├── SuperUpgraderClient.java      # Client entrypoint; screen registration & packet listeners
│       │   ├── client/
│       │   │   ├── SuperUpgraderDataGenerator.java # Fabric data generation entrypoint
│       │   │   └── mixin/ExampleClientMixin.java   # Client mixin placeholder
│       │   └── gui/
│       │       ├── ClientCatalog.java        # Client-side cache of server-sent target prices
│       │       ├── TargetSelectionScreen.java# 14-column searchable modal target item browser
│       │       ├── UpgraderScreen.java       # Main station GUI (288×240); handles buttons, presets, & input
│       │       ├── UpgraderStyle.java        # Color palette (charcoal/gold), panels, slots, and StyledButton
│       │       └── WheelRenderer.java        # Annulus wheel renderer; 12 o'clock sector, spinning animation
│       └── resources/
│           └── superupgrader.client.mixins.json # Client mixin configuration
├── build.gradle                    # Loom build script
├── gradle.properties               # Dependency versions and properties
└── settings.gradle                 # Gradle settings
```

---

## 3. Core Subsystems & Technical Architecture

### 3.1 Economy & Dynamic Pricing Engine (`com.esezak.superupgrader.value`)

1. **Seed Constants ([BaseValues.java](src/main/java/com/esezak/superupgrader/value/BaseValues.java)):**
   - Defines static base prices for raw, uncraftable materials (e.g., Cobblestone = 1, Logs = 4, Iron = 40, Gold = 60, Diamonds = 400, Netherite Scrap = 1,200, Nether Star = 4,000, Dragon Egg = 20,000).
2. **Recipe Graph Propagation ([RecipeGraphWalker.java](src/main/java/com/esezak/superupgrader/value/RecipeGraphWalker.java)):**
   - Scans all recipe categories from the server `RecipeManager`: Crafting (shaped/shapeless), Cooking (smelting, blasting, smoking, campfire), Stonecutting, and Smithing transforms.
   - Runs iteratively for up to 16 passes until convergence.
   - Selects the cheapest known ingredient cost alternative when multiple recipes exist.
   - Divides total ingredient cost by recipe result count.
   - Handles smithing transforms: `cost = base_item + addition_item` (smithing templates are deliberately excluded from the cost to match the reference economy). Armor-trim recipes are ignored.
3. **Rarity Fallbacks:**
   - Items unresolved after 16 passes receive a tier fallback based on `Rarity`:
     - Common: 25
     - Uncommon: 150
     - Rare: 600
     - Epic: 2,500
   - *Note:* Fallback values are strictly leaf nodes; they are never fed back into recipe ingredient calculations.
4. **Durability Scaling ([DurabilityAdjuster.java](src/main/java/com/esezak/superupgrader/value/DurabilityAdjuster.java)):**
   - `adjustedValue = baseValue * clamp((maxDamage - damageValue) / maxDamage, 0.05, 1.0)`.
   - Prevents 1-durability tools from becoming worthless while disallowing full-price exploits on broken gear.
5. **Cache Lifecycle ([ItemValueCache.java](src/main/java/com/esezak/superupgrader/value/ItemValueCache.java)):**
   - Recomputed at server startup and on datapack reloads (`ServerLifecycleEvents.END_DATA_PACK_RELOAD`).
   - Sorted ascending by item value, then by registry ID for consistent client catalog ordering.

### 3.2 Odds & Mathematical Model (`com.esezak.superupgrader.logic`)

- **Formula ([UpgradeCalculator.java](src/main/java/com/esezak/superupgrader/logic/UpgradeCalculator.java)):**

  ```math
  \text{wagerValue} = \text{unitValue} \times \text{count} \times \text{durabilityFactor}
  ```

  ```math
  \text{rewardValue} = \text{targetUnitValue} \times \text{targetCount}
  ```

  ```math
  \text{chance} = \text{clamp}\left(0.90 \times \frac{\text{wagerValue}}{\text{rewardValue}}, 0.001, 0.90\right)
  ```

- **Constants:**
  - `HOUSE_EDGE`: `0.90` (10% house margin)
  - `MIN_CHANCE`: `0.001` (0.1%)
  - `MAX_CHANCE`: `0.90` (90.0%)
- **Roll Execution ([SpinResult.java](src/main/java/com/esezak/superupgrader/logic/SpinResult.java)):**
  - Executed exclusively on the server with `java.util.Random`.
  - The gold winning sector spans $[0^\circ, \text{chance} \times 360^\circ)$ anchored at 12 o'clock ($0^\circ$).
  - If won: marker stops at a random uniform angle inside $[0^\circ, \text{arc})$.
  - If lost: marker stops uniformly in the remaining fail arc $[\text{arc}, 360^\circ)$.
  - Fixed spin duration: 3,500 ms.

### 3.3 Networking & Security Protocol (`com.esezak.superupgrader.network`)

All network communication uses Fabric's `PayloadTypeRegistry` with custom record payloads:

| Payload ID | Direction | Purpose | Contents |
| --- | --- | --- | --- |
| `superupgrader:valid_targets` | S2C | Synchronizes full server price catalog to client | List of `(Identifier, double value)` |
| `superupgrader:select_target` | C2S | Informs server of player's selected reward item | `Identifier targetId` |
| `superupgrader:set_target_count` | C2S | Updates desired target stack quantity | `int count` (1 to item max stack) |
| `superupgrader:request_spin` | C2S | Initiates upgrade attempt | Empty payload |
| `superupgrader:spin_result` | S2C | Sends outcome and final needle angle for animation | `SpinResult` record |
| `superupgrader:cooldown` | S2C | Notifies client of active cooldown remaining | `long cooldownRemainingMs` |

**Security & Anti-Exploit Measures:**
- **Server Authoritative:** Odds, outcomes, and rewards are computed on the server. The client only runs the visual animation based on the server's `SpinResult`.
- **Spin Lock:** During the 3,500 ms spin animation, input slots are locked. No items can be inserted or extracted.
- **Inventory Safeguards ([UpgraderScreenHandler.java](src/main/java/com/esezak/superupgrader/gui/UpgraderScreenHandler.java)):**
  - Only the input slot (slot 0) is consumed upon spin start.
  - Any interruption that closes the station (including closing the GUI, disconnecting, or dying) immediately settles the server's already-determined outcome; it does not wait for the remaining animation time or reroll the result.
  - On a win, the target stack is paid out immediately to inventory or dropped at the player's feet (`giveOrDrop`). On a loss, the wager remains consumed and nothing is paid out. Interrupting a spin never refunds the wager.
  - Input stacks that have not been wagered are returned to inventory or dropped when the station closes.

### 3.4 Client GUI & Rendering (`com.esezak.superupgrader.gui`)

- **Screen Layout ([UpgraderScreen.java](src/client/java/com/esezak/superupgrader/gui/UpgraderScreen.java)):**
  - Dimensions: 288×240 px, centered on screen.
  - Charcoal-gray bevels with gold accents defined in [UpgraderStyle.java](src/client/java/com/esezak/superupgrader/gui/UpgraderStyle.java).
- **Target Selection Screen ([TargetSelectionScreen.java](src/client/java/com/esezak/superupgrader/gui/TargetSelectionScreen.java)):**
  - 14 columns of item buttons with dynamic search box filtering by localized item name.
  - Smooth scrollbar and tooltip displaying unit value and projected win chance based on current wager.
- **Wheel Animation ([WheelRenderer.java](src/client/java/com/esezak/superupgrader/gui/WheelRenderer.java)):**
  - Renders continuous rasterized annulus rings (inner radius 30, outer radius 41) with 60 dial tick marks.
  - Cubic ease-out deceleration curve: `progress = clamp(t / 3500ms, 0, 1)`, needle angle interpolates through 5 full rotations (`1800° + finalAngle`).

---

## 4. Configuration Reference (`ModConfig.java`)

File path: `config/superupgrader.json` (inside the Minecraft instance folder).

```json
{
  "valueOverrides": {
    "minecraft:diamond": 500.0
  },
  "globalValueMultiplier": 1.0,
  "valueMultipliers": {
    "minecraft:elytra": 1.5
  },
  "blacklist": [
    "minecraft:tnt"
  ],
  "attemptCooldownSeconds": 0
}
```

- **`valueOverrides`**: Map of item ID to positive double. Overrides both initial seed and recipe-calculated prices, propagating into dependent recipes.
- **`globalValueMultiplier`**: Floating point factor applied to all calculated item prices.
- **`valueMultipliers`**: Per-item multiplier applied *after* recipe resolution.
- **`blacklist`**: List of item IDs completely excluded from being wagered or selected as targets.
- **`attemptCooldownSeconds`**: Enforces a minimum delay between wagers.

*Lifecycle Note:* Datapack reloads (`/reload`) recompute recipe graphs using active in-memory configs. Changes to `config/superupgrader.json` require a game/server restart to reload from disk.

---

## 5. Development & Build Commands

Gradle is configured with Loom for Minecraft 26.2 and Java 25.

```bash
# Package the production mod JAR (outputs to build/libs/superupgrader-1.0.0.jar)
./gradlew assemble

# Full build lifecycle including check tasks
./gradlew build

# Launch client development environment
./gradlew runClient

# Launch dedicated server development environment
./gradlew runServer

# Offline build (when dependencies are already cached)
./gradlew assemble --offline
```

*Build Tips:*
- JVM arguments in `gradle.properties` set `-Xmx1G` and `org.gradle.parallel=true`.
- IntelliJ configuration cache should remain disabled (`org.gradle.configuration-cache=false`) due to Loom compatibility.

---

## 6. Verification & Testing Checklist

When making changes to logic, GUI, or networking, execute the following manual test matrix:

1. **Crafting & Opening:**
   - Join a world with a fresh player: verify the Super Upgrader recipe unlocks in the recipe book without collecting items. The hidden recipe advancement grants it on the first server tick and persists per player.
   - Join an existing world with the mod newly installed: verify existing players also receive the recipe.
   - Craft the Super Upgrader with 4 Gold Ingot, 4 Diamonds, and 1 Anvil.
   - Right-click with the item in hand: verify the GUI opens and plays at standard GUI scale.
2. **Pricing Math:**
   - Wager 4 Iron Ingots (unit price 40 = 160 points) toward 1 Diamond (unit price 400):
     - Expected chance: $0.90 \times (160 / 400) = 36.00\%$.
   - Increase target Diamond quantity to 2 (800 points):
     - Expected chance: $18.00\%$.
3. **Durability Handling:**
   - Wager a damaged diamond sword (e.g., 50% durability):
     - Verify value is halved compared to a pristine sword.
4. **Preset Buttons:**
   - Click `×2`, `×4`, `×8`: verify target switches to the closest match and quantity resets to 1.
   - Click `30%`, `50%`, `70%`: verify chance matches closest available item.
5. **Animation & Outcome Sync:**
   - Spin the wheel: verify the gold zone stays fixed at 12 o'clock.
   - Verify the needle smoothly decelerates over 3.5 seconds.
   - Confirm winning spin lands in the gold zone; losing spin lands in the dark zone.
6. **Edge Cases & Disconnect Handling:**
   - Interrupt a winning spin by closing the screen, disconnecting, or dying: verify the target stack is paid out immediately, without waiting for the animation to finish, and is paid only once.
   - Repeat with a losing spin: verify the wager stays consumed and no item is paid out or refunded.
   - Close the station before starting a spin: verify the unwagered input stack is returned.
   - Fill all inventory slots and win an upgrade: verify overflow is dropped at the player's position.
   - Datapack reload: execute `/reload` on a server and confirm prices remain synchronized without disconnect.

---

## 7. Roadmap & Architectural Proposals

These features are planned or under consideration for future revisions:

### 7.1 Potion Support via Brewing Recipe Graph
- **Current Limitation:** Minecraft potions share base item IDs (`minecraft:potion`, `minecraft:splash_potion`) and rely on Data Components (`minecraft:potion_contents`) for effects. The current catalog only indexes `Item` instances.
- **Proposed Architecture:**
  1. Extend `TargetEntry` and catalog definitions to support `(Item, DataComponentMap)` variant pairs.
  2. Implement a `BrewingGraphWalker` to inspect `PotionBrewing` recipes.
  3. Traverse from water bottles through base ingredients (Nether Wart, Redstone, Glowstone, Gunpowder, Dragon's Breath).
  4. Display effect name, amplifier, and duration in target tooltips.

### 7.2 Spawn Egg Valuation
- **Current Limitation:** All `SpawnEggItem` instances are excluded via [ItemExclusionFilter.java](src/main/java/com/esezak/superupgrader/value/ItemExclusionFilter.java).
- **Proposed Architecture:**
  1. Add a config toggle: `enableSpawnEggs: true/false`.
  2. Establish a baseline unit price of 20,000 points (matching the Dragon Egg).
  3. Allow per-mob spawn egg overrides via `valueOverrides`.

### 7.3 Maintenance & Housekeeping
- **License Metadata:** Both `fabric.mod.json` and the root `LICENSE` file are aligned to the MIT License.
- **GUI Scaling:** Continue tuning widget margin offsets on small GUI scales (`GuiScale: 1` or `2`).
