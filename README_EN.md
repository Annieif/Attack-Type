# Attack Type Mod

A Minecraft Fabric 1.20.1 mod that adds a **compound damage system** with **Physical Attack Types** and **Seven Deadly Sin Attributes**.

---

## Overview

Every direct attack in Minecraft is now classified into one of 4 physical types, and can carry one of 7 sin attributes. Each entity has 10 resistance values (3 physical + 7 sin) that determine how much damage it takes. Players can collect **Sin Fragments** through unique gameplay mechanics tied to each sin, and expend them to trigger sin-empowered attacks.

---

## Core Concepts

### Physical Attack Types

All direct damage is classified into 4 physical types:

| Type    | Enum     | Source                                    |
|---------|----------|-------------------------------------------|
| Slash   | `SLASH`  | Swords, axes, tridents (melee)            |
| Pierce  | `PIERCE` | Arrows (all types), thrown tridents       |
| Blunt   | `BLUNT`  | Fists, pickaxes, shovels, snowballs, eggs, other projectiles |
| None    | `NONE`   | Fall damage, suffocation, fire, poison, etc. (non-entity sources) |

> **Projectile rule**: Not all projectiles are Pierce. Only `ArrowEntity` and `TridentEntity` (thrown) are Pierce. Snowballs, eggs, and other projectiles are Blunt.

### Seven Deadly Sin Attributes

| Sin       | Enum        | Color      | Enchantment ID           |
|-----------|-------------|------------|--------------------------|
| Wrath     | `WRATH`     | Red        | `attack_type:wrath`      |
| Lust      | `LUST`      | Orange     | `attack_type:lust`       |
| Sloth     | `SLOTH`     | Yellow     | `attack_type:sloth`      |
| Gluttony  | `GLUTTONY`  | Lime Green | `attack_type:gluttony`   |
| Gloom     | `GLOOM`     | Sky Blue   | `attack_type:gloom`      |
| Pride     | `PRIDE`     | Deep Blue  | `attack_type:pride`      |
| Envy      | `ENVY`      | Purple     | `attack_type:envy`       |

Each sin corresponds to a weapon enchantment (max level 5). An enchanted weapon's attacks carry that sin attribute.

### Sin Attack Particle Effects

Each sin attack spawns 12 colored `DustParticleEffect` particles around the target, making it easy to identify which sin type is being used:

| Sin       | Particle Color (RGB)          |
|-----------|-------------------------------|
| Wrath     | Red (1.0, 0.2, 0.2)           |
| Lust      | Orange (1.0, 0.5, 0.0)        |
| Sloth     | Yellow (1.0, 0.9, 0.1)        |
| Gluttony  | Lime Green (0.2, 0.8, 0.2)    |
| Gloom     | Sky Blue (0.2, 0.7, 1.0)      |
| Pride     | Deep Blue (0.1, 0.2, 0.8)     |
| Envy      | Purple (0.6, 0.2, 1.0)        |

---

## Damage Calculation

### Damage Formula

```
applyDamage(amount × physMult) + sinDamage
```

Where:

- `physMult = physicalResistance × armorPhysicalEnchantment` (4 armor pieces multiplied independently, each = 1 - 0.05 × enchantment level)
- `sinDamage = (sinLevel × 3 + 1) × sinResistance`
- `applyDamage()` is the vanilla method, which internally applies armor/protection reduction

**Key insight**: Sin damage is added *after* `applyDamage()`, bypassing all physical damage reduction.

### Sin Attack Levels

| Level | Bonus Damage | Fragment Cost |
|-------|-------------|---------------|
| Lv.1  | +4          | 40            |
| Lv.2  | +7          | 70            |
| Lv.3  | +10         | 100           |

### Resistance System

Each entity has 10 resistance multipliers (3 physical + 7 sin), ranging from ≥0.01 with no upper limit, rounded to 2 decimal places.

| Grade      | Multiplier     | Effect                                      |
|------------|----------------|---------------------------------------------|
| Fatal      | > 1.5          | Greatly increased damage taken              |
| Vulnerable | > 1.0 (≠ 1.0)  | Increased damage taken                      |
| Normal     | = 1.0          | Normal damage                               |
| Tough      | < 1.0          | Reduced damage taken                        |
| Resistant  | ≤ 0.5          | Greatly reduced damage taken                |
| Immune     | = 0.0          | Completely immune to this damage type       |

All 10 resistance values are constrained by `totalProduct` (geometric mean). Modifying resistances via the GUI automatically normalizes them to maintain the total product.

### Total Product Decay

When any sin fragment count reaches **500** for the first time, the player's `totalProduct` decreases by **0.1** (minimum 0.1). This represents the gradual corruption of the entity's overall resistance as sins accumulate. The threshold marker resets when fragments drop below 500, allowing repeated decay.

---

## Sin Fragment System (Player-Only)

### Fragment Acquisition

Each sin has unique fragment acquisition methods that reflect its "personality":

#### Wrath — Chain Killing

Tracks the time interval between consecutive mob kills:

| Interval | Fragments |
|----------|-----------|
| ≤ 1 sec  | +7        |
| ≤ 2 sec  | +5        |
| ≤ 5 sec  | +3        |
| ≤ 10 sec | +1        |

#### Lust — Breeding & Mutation

| Action                                                     | Fragments |
|------------------------------------------------------------|-----------|
| Breed any animal                                           | +1        |
| Throw an egg                                               | +1        |
| Cure a zombie villager                                     | +10       |
| Lightning strike transforms Pig→Zombified Piglin / Creeper→Charged Creeper / Mooshroom→Brown/Red variant / Villager→Witch | +5        |
| Zombie converts to Drowned in water                        | +5        |

#### Sloth — Idleness & Sleep

| Action                                    | Fragments |
|-------------------------------------------|-----------|
| Stand still (no running, no jumping) per minute | +3        |
| Complete a full sleep cycle               | +5        |

> QoL: Players can sleep at any time (even during daytime) and can sleep repeatedly.

#### Gluttony — Eating

| Action          | Fragments |
|-----------------|-----------|
| Eat any food item | +1        |

> QoL: Players can eat even when hunger bar is full.

#### Gloom — Suffering & Witnessing

| Action                                                    | Fragments |
|-----------------------------------------------------------|-----------|
| Take damage (per point of damage)                         | +1        |
| Witness another entity take damage not caused by the player (per point) | +1        |

#### Pride — Achievement & Production

| Action                                                    | Fragments |
|-----------------------------------------------------------|-----------|
| Earn any advancement/progress                             | +10       |
| Craft 27 stacks (1,728 items) cumulatively                | +1        |
| Smelt, brew, or enchant (each operation)                  | +2        |

#### Envy — Comparison & Witnessing

| Action                                                    | Fragments |
|-----------------------------------------------------------|-----------|
| Nearby entity has better equipment than you (per minute)  | +3        |
| Witness any entity perform a sin-attribute attack (per attack) | +1        |

> Equipment tiers: Netherite > Diamond > Gold > Iron > Stone > Wood > None. Enchanted > Unenchanted.

### Fragment Thresholds

| Threshold | Status   | Behavior                                              |
|-----------|----------|-------------------------------------------------------|
| 500       | Overflow | HUD shows warning, next sin attack auto-triggers      |
| 1000      | Kill     | HUD shows danger, attacker is instantly killed (self) |

### Manual Trigger

Press `\` to manually trigger the active sin attack (consumes fragments):

- Press 1 time → Lv.1 (costs 40 fragments)
- Press 2 times → Lv.2 (costs 70 fragments)
- Press 3 times → Lv.3 (costs 100 fragments)

**Enchantment cost reduction**: When wielding a weapon with the matching sin enchantment, each enchantment level reduces the cost by 2 (minimum 1). Formula: `max(1, baseCost - 2 × enchantLevel)`

Press `[` / `]` to cycle the active sin type.

### Activation Duration

| Level | Duration (ticks) | Duration (seconds) |
|-------|-----------------|-------------------|
| Lv.1  | 80              | 4s                |
| Lv.2  | 140             | 7s                |
| Lv.3  | 200             | 10s               |

---

## Non-Player Mob Sin Trigger

Non-player LivingEntities don't use the fragment system. Instead, each attack rolls independently:

```
For each of the 7 sins:
  Trigger chance = 5% + enchantment level × 10%
  Sin level      = random in [max(1, enchantLevel), min(3, enchantLevel+1)]
```

Uses a deterministic seed (UUID + coordinates + world time) for consistency across client and server.

| Scenario                     | Wrath Trigger Rate | Level Range |
|------------------------------|-------------------|-------------|
| Zombie with bare hands       | 5%                | 1 ~ 3       |
| Zombie with Wrath II sword   | 25%               | 2 ~ 3       |
| Pillager with Sloth IV crossbow | 45%            | 3 (clamped) |

---

## Non-Player Resistance Decay

Non-player entities decay every 2 in-game days (48,000 ticks = 40 minutes):

1. Randomly shuffle resistance values (30 swaps, factor 0.8~1.2)
2. Normalize to current total product
3. Every 4 full cycles (8 in-game days), decrease total product by 0.01 (minimum 0.01)

Players are exempt from this decay (resistance is managed via the GUI).

---

## Enchantment System

### Sin Enchantments (7 types)

| Property         | Value                  |
|------------------|------------------------|
| Rarity           | RARE                   |
| Max Level        | 5                      |
| Applicable Slot  | Mainhand               |
| Enchanting Table Weight | 1 + (level - 1) × 10 |

### Physical Resistance Enchantments (3 types: Slash/Pierce/Blunt)

| Property         | Value                                          |
|------------------|------------------------------------------------|
| Rarity           | RARE                                           |
| Max Level        | 4                                              |
| Applicable Slots | Helmet, Chestplate, Leggings, Boots            |
| Enchanting Table Weight | 1 + (level - 1) × 8                     |
| Damage Reduction | Per piece: 1 - 0.05 × level, all 4 multiplied  |

---

## Controls

| Key | Action                                                    |
|-----|-----------------------------------------------------------|
| U   | Open Resistance Allocation GUI                            |
| [   | Cycle active sin type left                                |
| ]   | Cycle active sin type right                               |
| \   | Trigger sin attack (press 1-3 times for level selection)  |

---

## Sin Fragment HUD

The top-left corner of the screen displays 7 sin icons (32×32) with fragment counts:

- **Normal** (white text): 0 ~ 499 fragments
- **Overflow** (orange text + border): 500 ~ 999 fragments
- **Kill** (red text + border): 1000+ fragments (imminent death)
- **Zero** (gray text): 0 fragments

Corner highlight indicates the currently active sin for manual triggering.

---

## Commands

Permission level 2 (operator). Root command: `/attacktype`.

### Resistance Management

| Command                                    | Description                              |
|--------------------------------------------|------------------------------------------|
| `/attacktype get [entity]`                 | View entity resistance + grade labels    |
| `/attacktype set <type> <value> [entity]`  | Set resistance value (≥0.0, no upper limit) |
| `/attacktype reset [entity]`               | Reset to random resistances              |
| `/attacktype tick [entity]`                | Manually trigger one decay cycle         |
| `/attacktype test`                         | Spawn 10 test dogs with extreme resistances (hostile to each other) |

### Fragment Management

| Command                                              | Description                    |
|------------------------------------------------------|--------------------------------|
| `/attacktype fragment get [player]`                  | View fragment data + status    |
| `/attacktype fragment add <sin> <amount> [player]`   | Add fragments                  |
| `/attacktype fragment set <sin> <amount> [player]`   | Set fragment count             |

Type parameters (case-insensitive): `slash` / `pierce` / `blunt` / `wrath` / `lust` / `sloth` / `gluttony` / `gloom` / `pride` / `envy`

### Test Dogs (`/attacktype test`)

Spawns 10 wolves with descriptive names indicating their resistance profiles, all hostile to each other:

| Name              | Resistance Profile                          |
|-------------------|---------------------------------------------|
| All 0.0           | All physical & sin resistances at 0.0       |
| Slash 0.0         | Only Slash resistance at 0.0                |
| Pierce 0.0        | Only Pierce resistance at 0.0               |
| Blunt 0.0         | Only Blunt resistance at 0.0                |
| Wrath 0.0         | Only Wrath sin resistance at 0.0            |
| All 50.0          | All resistances at 50.0 (extreme tank)      |
| All 0.0 prod=0.1  | All 0.0 with totalProduct = 0.1             |
| Slash 100         | Slash 100.0, everything else 0.01           |
| Pierce 100        | Pierce 100.0, everything else 0.01          |
| Blunt 100         | Blunt 100.0, everything else 0.01           |

---

## Code Architecture

```
src/
├── client/java/org/attack_type/
│   ├── client/
│   │   ├── Attack_typeClient.java              # Client entry: keybinds + HUD + network
│   │   └── Attack_typeDataGenerator.java       # Data generator entry (reserved)
│   ├── fragment/
│   │   └── ClientFragmentCache.java            # Client fragment cache (HUD read-only)
│   ├── gui/
│   │   ├── ResistanceScreen.java               # Resistance allocation GUI
│   │   └── SinFragmentHUD.java                 # Fragment HUD rendering
│   └── network/
│       ├── ClientResistanceCache.java          # Client resistance cache
│       └── NetworkHandlerClient.java           # Client network packet receiver
├── main/java/org/attack_type/
│   ├── Attack_type.java                        # Mod main entry point
│   ├── advancement/
│   │   └── ModAdvancements.java                # Advancement system
│   ├── api/
│   │   ├── AttackType.java                     # Physical attack type enum
│   │   ├── AttackTypeMapper.java               # Attack type/sin determination core
│   │   ├── ResistanceProfile.java              # Resistance data model + NBT serialization
│   │   └── SinType.java                        # Sin attribute enum
│   ├── command/
│   │   └── ResistanceCommand.java              # /attacktype debug commands
│   ├── component/
│   │   └── ResistanceManager.java              # Global resistance management + periodic decay
│   ├── config/
│   │   └── ModConfig.java                      # Global config (hot reload + presets)
│   ├── effect/
│   │   ├── BurstEffect.java                    # Burst effect (thorns + resistance adjustment + damage conversion)
│   │   ├── CostIncreaseEffect.java             # Cost increase effect
│   │   ├── EffectCategory.java                 # Effect category enum (strengthen/guard/boost/weaken/vulnerable/reduce)
│   │   ├── FragmentBoostEffect.java            # Fragment boost effect
│   │   ├── FragmentDrainEffect.java            # Fragment drain effect
│   │   ├── IgnoreResistanceEffect.java         # Ignore resistance effect
│   │   ├── ModPotions.java                     # Potion registry + two-stage brewing recipes
│   │   ├── ModStatusEffects.java               # Status effect registry (60 effects)
│   │   ├── NoCostEffect.java                   # No cost effect
│   │   └── SinCategoryEffect.java              # Sin/physical category status effect
│   ├── enchantment/
│   │   ├── ModEnchantments.java                # Enchantment registry
│   │   ├── PhysicalResistanceEnchantment.java  # Physical resistance armor enchantment
│   │   └── SinEnchantment.java                 # Sin weapon enchantment
│   ├── fragment/
│   │   ├── SinFragmentAcquisition.java         # 7 sin fragment acquisition system
│   │   ├── SinFragmentData.java                # Fragment data model + consumption constants
│   │   └── SinFragmentManager.java             # Fragment management (add/remove/trigger/overflow/kill)
│   ├── mixin/
│   │   ├── MixinLivingEntity.java              # LivingEntity damage calculation injection + particles + effects
│   │   ├── MixinPlayerEntity.java              # Gluttony: allow eating at full hunger
│   │   ├── MixinServerPlayerEntity.java        # Sloth: sleep anytime + sleep detection
│   │   ├── MixinLightningStrike.java           # Lust: lightning strike mutation detection
│   │   ├── MixinAnimalEntity.java              # Lust: animal breeding detection
│   │   ├── MixinZombieVillagerEntity.java      # Lust: zombie villager cure detection
│   │   ├── MixinZombieEntity.java              # Lust: zombie to drowned conversion
│   │   └── MixinPlayerAdvancementTracker.java  # Pride: advancement earned detection
│   └── network/
│       ├── ModPackets.java                     # Network packet channel identifiers
│       └── NetworkHandler.java                 # Server-side network packet handling + push
└── main/resources/
    ├── assets/attack_type/lang/
    │   ├── zh_cn.json                           # Simplified Chinese
    │   └── en_us.json                           # English
    ├── assets/attack_type/sin_types.json        # Sin attribute configuration
    └── assets/attack_type/textures/gui/sin_fragment/
        ├── wrath.png ~ envy.png                 # 7 sin icons (32×32)
```

---

## Network Communication

| Channel             | Direction | Content                                        |
|---------------------|-----------|------------------------------------------------|
| `resistance_sync`   | S→C       | Full resistance NBT sync                       |
| `resistance_update` | C→S       | Player submits resistance changes (10×float + totalProduct) |
| `fragment_sync`     | S→C       | Full fragment NBT sync                         |
| `fragment_trigger`  | C→S       | Manual sin trigger (ordinal + level)           |

---

## Status Effects & Potion System

### Effect Categories

The mod adds 60 status effects (StatusEffect) in three categories:

#### Sin/Physical Category Effects (48 types)

6 categories × (7 sins + 1 physical) = 48 types:

| Category | Keyword | Effect | Formula |
|----------|---------|--------|---------|
| Strengthen | `strengthen` | Deal +N% damage of this type | `1 + 0.3 × (amplifier + 1)` |
| Guard | `guard` | Take -N% damage of this type | `1 - 0.3 × (amplifier + 1)` |
| Boost | `boost` | Add +N flat damage | `2 × (amplifier + 1)` |
| Weaken | `weaken` | Deal -N% damage of this type | `1 - 0.3 × (amplifier + 1)` |
| Vulnerable | `vulnerable` | Take +N% damage of this type | `1 + 0.3 × (amplifier + 1)` |
| Reduce | `reduce` | Reduce flat damage by N | `2 × (amplifier + 1)` |

#### Generic Effects (5 types)

| Effect | Keyword | Description |
|--------|---------|-------------|
| Fragment Boost | `fragment_boost` | Gain +N extra fragments per acquisition |
| No Cost | `no_cost` | Sin trigger costs no fragments |
| Ignore Resistance | `ignore_resistance` | N% of damage ignores physical/sin resistance |
| Fragment Drain | `fragment_drain` | Lose N fragments every 5 seconds |
| Cost Increase | `cost_increase` | Sin trigger costs +N% more fragments |

#### Burst Effects (7 types × 5 levels)

Using "Burst of Gloom" as an example:

| Effect | Formula |
|--------|---------|
| Sin Resistance | +0.3 × N |
| Physical Resistance | -0.5 × N |
| Thorns | Deal 3 × N sin damage to attacker when hit |
| Damage Conversion | All incoming damage becomes sin-typed |

### Brewing System

Uses a **two-stage brewing** system:

```
Awkward Potion + Sin Material → Sin Base Potion → + Category Material → Specific Potion
```

| Sin | Base Material | Category Materials |
|-----|--------------|-------------------|
| Wrath | Blaze Rod | Fire Charge(Strengthen) / Iron Ingot(Guard) / Prismarine Crystals(Boost) / Poisonous Potato(Weaken) / String(Vulnerable) / Popped Chorus Fruit(Reduce) |
| Lust | Rose Bush | Same as above |
| Sloth | Feather | Same as above |
| Gluttony | Rotten Flesh | Same as above |
| Gloom | Ink Sac | Same as above |
| Pride | Gold Ingot | Same as above |
| Envy | Emerald | Same as above |
| Burst | — | Echo Shard (added to sin base potion) |

**Upgrades & Conversions:**
- Glowstone Dust → +1 level (sin/physical/generic max Lv3, burst max Lv5)
- Redstone → Extended duration (base 3min → extended 8min)
- Gunpowder → Splash potion
- Dragon's Breath → Lingering potion

---

## Technical Highlights

- **ThreadLocal caching**: `MixinLivingEntity` uses `PENDING_PHYS_MULT` and `PENDING_SIN_DAMAGE` ThreadLocals to pass data between HEAD injection and ModifyArg injection, ensuring consistent resistance multiplier and sin damage pairing within a single `damage()` call
- **ConcurrentHashMap**: `ResistanceManager` and `SinFragmentManager` use thread-safe maps for entity/player data management
- **Deterministic randomness**: Non-player mob sin rolls use `(UUID LSB ^ worldTime ^ coordinates)` as seed for cross-side consistency
- **NBT serialization**: `ResistanceProfile` and `SinFragmentData` support full NBT read/write for persistence and network sync
- **Geometric mean normalization**: `ResistanceProfile.normalize()` uses `(totalProduct / curProduct)^(1/10)` to proportionally scale all 10 resistance values
- **Sin particle effects**: Each sin attack spawns 12 `DustParticleEffect` particles with sin-specific colors around the target
- **Fragment acquisition system**: `SinFragmentAcquisition` implements 7 independent fragment acquisition mechanics via Fabric API events + 8 Mixin injections
- **QoL features**: `MixinPlayerEntity` allows eating at full hunger, `MixinServerPlayerEntity` allows sleeping anytime

---

## System Rules Summary

### Attack Type Determination

1. Projectiles: only `ArrowEntity` / `TridentEntity` (thrown) → Pierce; all others → Blunt
2. Melee weapons (sword/axe/trident) → Slash; bare hands/other items → Blunt
3. Non-entity sources (fall/fire/poison/etc.) → NONE

### Sin Trigger Rules

4. Players: manual trigger (consume fragments) or overflow auto-trigger (≥500 fragments)
5. Non-players: independent roll per sin, 5% + enchantment level × 10% trigger rate, level range [max(1, enchantLv), min(3, enchantLv+1)]
6. Fragments ≥1000: attacker is instantly killed (self)

### Damage Calculation Rules

7. `applyDamage(amount × physMult) + sinDamage`
8. `physMult = physical resistance × Π(1 - 0.05 × armorEnchantLevel)`
9. `sinDamage = (sinLevel × 3 + 1) × sin resistance`
10. Sin damage is added after `applyDamage()`, bypassing physical reduction

### Resistance Management Rules

11. All resistance values clamped to ≥0.01 (no upper limit), 2 decimal places
12. Normalization uses geometric mean scaling, maintaining product = totalProduct
13. Player resistance is manually managed via GUI, no automatic decay
14. Non-player entities randomize resistance every 48,000 ticks, totalProduct -0.01 every 4 cycles
15. Players get randomized resistance on first creation, then normalized

### Fragment Management Rules

16. Manual trigger cost = max(1, baseCost - 2 × enchantLevel)
17. Overflow threshold: 500; Kill threshold: 1000
18. Player disconnect auto-cleans fragment data
19. Any sin fragment first reaching 500 reduces totalProduct by 0.1 (minimum 0.1)

### Enchantment Rules

20. Sin enchantments: RARE / 5 levels / mainhand / any item
21. Physical resistance enchantments: RARE / 4 levels / 4 armor slots / -5% damage per level

### Network Rules

22. Resistance changes must be normalized server-side before syncing back to client
23. Fragment triggers are validated server-side; client only sends requests
24. Auto-sync resistance + fragment data on player join

---

## Build

```bash
./gradlew build
```

Output: `build/libs/Attack_Type-1.0-SNAPSHOT.jar`

### Dependencies

- Minecraft 1.20.1
- Fabric Loader ≥ 0.14
- Fabric API

---

## Localization

The mod currently supports:

- **English** (en_us.json) — full coverage
- **Simplified Chinese** (zh_cn.json) — full coverage

---

## Configuration

All sin-related parameters can be configured in `assets/attack_type/sin_types.json`:

- Sin type properties (name, color, icon, enchantment parameters)
- Fragment system parameters (costs, thresholds, durations, gain amounts)
- Fragment acquisition rules (all 7 sins with amounts and conditions)
- Particle effect colors (RGB values for each sin)
- QoL feature toggles (always eat, always sleep)
- Mob sin trigger parameters (base chance, enchantment scaling, level range)
- HUD rendering parameters (colors, position, alpha, size)
- Enchantment defaults (rarity, max level, enchantment table weight)
- Damage formula reference

Changes take effect after restarting the game.

---

## License

This project is for educational and experimental purposes.