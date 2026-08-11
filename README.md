# Attack Type Mod

Minecraft Fabric 1.20.1 模组，实现"攻击类型"和"罪孽属性"伤害系统。所有攻击具有物理类型（斩击/突刺/打击）和罪孽属性（七大罪），每个实体拥有可变的抗性配置。

## 伤害系统规则

### 攻击类型

| 类型 | 来源 |
|------|------|
| **斩击 (SLASH)** | 剑、斧、三叉戟（近战） |
| **突刺 (PIERCE)** | 箭矢、投掷三叉戟等弹射物 |
| **打击 (BLUNT)** | 空手、镐、锹等其余物品 |
| **无类型 (NONE)** | 摔落、窒息、中毒、火焰等非物理伤害 |

### 罪孽属性

七种罪孽附魔：暴怒(WRATH)、色欲(LUST)、怠惰(SLOTH)、暴食(GLUTTONY)、忧郁(GLOOM)、傲慢(PRIDE)、嫉妒(ENVY)。最高5级，适用于所有物品。攻击默认无罪孽属性，仅通过附魔概率触发。

### 物理抗性

五个等级，基于抗性乘数判定：

| 等级 | 乘数范围 |
|------|---------|
| 致命 | > ×1.5 |
| 脆弱 | > ×1.0 |
| 一般 | = ×1.0 |
| 耐性 | > ×0.5 |
| 抵抗 | ≤ ×0.5 |

### 物理抗性附魔

防具专属，3种（斩击/突刺/打击抗性），最高4级，每级减少 ×0.05 伤害。

### 伤害公式

```
最终伤害 = (物理基础伤害 × 物理抗性 × 护甲抗性附魔) × 实体基础抗性(护甲/保护/附魔等)
         + (罪孽等级 × 3 + 1) × 罪孽抗性
```

- 罪孽伤害强行扣除，不受物理减伤影响
- 单次攻击只取等级最高的罪孽属性

### 抗性系统

- 实体初始所有抗性为 ×1.0，总积为 1.0
- 每 2 天（48000 ticks）随机重分配（玩家除外）
- 每 4 个周期总积减少 0.01
- 玩家通过 U 键打开 GUI 手动分配抗性，总积保持不变

---

## 代码架构

```
src/
├── client/java/org/attack_type/
│   ├── client/Attack_typeClient.java    # 客户端入口
│   ├── gui/ResistanceScreen.java        # 抗性分配 GUI
│   └── network/
│       ├── ClientResistanceCache.java   # 客户端抗性缓存
│       └── NetworkHandlerClient.java    # 客户端网络注册
├── main/java/org/attack_type/
│   ├── Attack_type.java                 # 主入口 ModInitializer
│   ├── api/
│   │   ├── AttackType.java              # 攻击类型枚举
│   │   ├── AttackTypeMapper.java        # 攻击类型/罪孽映射
│   │   ├── ResistanceProfile.java       # 抗性数据模型
│   │   └── SinType.java                 # 罪孽属性枚举
│   ├── component/
│   │   └── ResistanceManager.java       # 实体抗性管理
│   ├── enchantment/
│   │   ├── ModEnchantments.java         # 附魔注册
│   │   ├── PhysicalResistanceEnchantment.java
│   │   └── SinEnchantment.java
│   ├── mixin/
│   │   └── MixinLivingEntity.java       # 伤害计算注入
│   └── network/
│       ├── ModPackets.java              # 网络包标识
│       └── NetworkHandler.java          # 服务端网络处理
```

---

## 代码解析

### 1. 核心枚举

**AttackType** — 定义三种物理攻击类型 + NONE 表示无类型：

```java
// src/main/java/org/attack_type/api/AttackType.java
public enum AttackType {
    SLASH,   // 斩击：剑/斧/三叉戟近战
    PIERCE,  // 突刺：弹射物
    BLUNT,   // 打击：空手/其余物品
    NONE;    // 无类型：环境伤害
}
```

**SinType** — 七种罪孽属性：

```java
// src/main/java/org/attack_type/api/SinType.java
public enum SinType {
    WRATH,    // 暴怒
    LUST,     // 色欲
    SLOTH,    // 怠惰
    GLUTTONY, // 暴食
    GLOOM,    // 忧郁
    PRIDE,    // 傲慢
    ENVY;     // 嫉妒
}
```

### 2. AttackTypeMapper — 攻击分类器

核心逻辑：根据 `DamageSource` 判定攻击类型和罪孽属性。

```java
// src/main/java/org/attack_type/api/AttackTypeMapper.java
public static AttackType getAttackType(DamageSource source) {
    // 弹射物 → 突刺
    if (source.getSource() instanceof ProjectileEntity) {
        return AttackType.PIERCE;
    }
    // 近战 → 按武器判定
    if (source.getSource() instanceof LivingEntity attacker) {
        ItemStack weapon = attacker.getMainHandStack();
        if (weapon.isEmpty()) return AttackType.BLUNT;
        // 剑/斧/三叉戟 → 斩击，其余 → 打击
        String name = weapon.getItem().toString().toLowerCase();
        if (name.contains("sword") || name.contains("axe") || name.contains("trident"))
            return AttackType.SLASH;
        return AttackType.BLUNT;
    }
    return AttackType.NONE; // 环境伤害等
}
```

罪孽判定：遍历7种罪孽附魔，取等级最高的：

```java
public static SinType getSinType(LivingEntity attacker) {
    ItemStack weapon = attacker.getMainHandStack();
    SinType bestSin = null;
    int bestLevel = 0;
    for (SinType sinType : SinType.values()) {
        Enchantment enchantment = ModEnchantments.getSinEnchantment(sinType);
        int level = EnchantmentHelper.getLevel(enchantment, weapon);
        if (level > bestLevel) {
            bestLevel = level;
            bestSin = sinType;
        }
    }
    return bestSin;
}
```

### 3. ResistanceProfile — 抗性数据模型

存储一个实体的所有抗性值，支持序列化、随机化、归一化。

```java
// src/main/java/org/attack_type/api/ResistanceProfile.java
public class ResistanceProfile {
    private final Map<AttackType, Float> physicalResistances;  // 3种物理抗性
    private final Map<SinType, Float> sinResistances;          // 7种罪孽抗性
    private float totalProduct;  // 总积，初始 1.0
    private long lastUpdateTick; // 上次更新 tick
}
```

**关键方法：**

- `randomizeResistances()` — 随机打乱10个抗性值（3物理+7罪孽），保持总积不变
- `normalize()` — 将当前10个值的乘积缩放回 `totalProduct`，确保总积约束
- `writeNbt()` / `readNbt()` — NBT 序列化，用于网络同步和持久化

归一化算法：计算当前乘积 `product`，然后用 `ratio = pow(totalProduct / product, 1/10)` 的等比缩放调整所有值。

### 4. ResistanceManager — 抗性管理器

全局管理所有实体的抗性配置，实现周期更新。

```java
// src/main/java/org/attack_type/component/ResistanceManager.java
public class ResistanceManager {
    private static final Map<UUID, ResistanceProfile> PROFILES = new ConcurrentHashMap<>();
    public static final long UPDATE_INTERVAL_TICKS = 24000L * 2; // 2天
}
```

**关键逻辑：**

- `getProfile(entity)` — 惰性初始化，玩家也会随机初始分配
- `tickEntityResistance(entity, worldTime)` — 每2天触发随机重分配，每8天（4周期）总积减少0.01
- 玩家不触发周期自动更新（由 GUI 手动控制）

### 5. MixinLivingEntity — 伤害计算注入

通过 Mixin 修改 `LivingEntity.damage()`，实现自定义伤害公式。

```java
// src/main/java/org/attack_type/mixin/MixinLivingEntity.java
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    // 使用 ThreadLocal 在线程间传递计算结果
    private static final ThreadLocal<Float> PENDING_PHYS_MULT = new ThreadLocal<>();
    private static final ThreadLocal<Float> PENDING_SIN_DAMAGE = new ThreadLocal<>();
}
```

**三个注入点：**

| 注入 | 时机 | 作用 |
|------|------|------|
| `@Inject(method="damage", at=@At("HEAD"))` | 伤害计算前 | 计算物理抗性乘数、罪孽伤害 |
| `@ModifyArg(applyArmorToDamage)` | 护甲减伤前 | 将物理抗性乘数应用于护甲减伤 |
| `@ModifyArg(applyDamage)` | 最终扣血 | 叠加罪孽伤害（无视减伤） |

**伤害计算流程：**

```
damage() 被调用
  → HEAD: 计算 physMult(物理抗性×护甲附魔) 和 sinDamage(罪孽伤害)
  → applyArmorToDamage: 护甲减伤 × physMult
  → applyDamage: 最终伤害 + sinDamage
```

### 6. 附魔系统

**SinEnchantment** — 罪孽附魔（7种），适用于所有物品，最高5级：

```java
// src/main/java/org/attack_type/enchantment/SinEnchantment.java
public class SinEnchantment extends Enchantment {
    public SinEnchantment(SinType sinType) {
        super(Rarity.RARE, EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{MAINHAND});
    }
    public int getMaxLevel() { return 5; }
    public boolean isAcceptableItem(ItemStack stack) { return true; } // 所有物品
}
```

**PhysicalResistanceEnchantment** — 物理抗性附魔（3种），仅防具，最高4级：

```java
// src/main/java/org/attack_type/enchantment/PhysicalResistanceEnchantment.java
public class PhysicalResistanceEnchantment extends Enchantment {
    public PhysicalResistanceEnchantment(AttackType attackType) {
        super(Rarity.RARE, EnchantmentTarget.ARMOR,
              new EquipmentSlot[]{HEAD, CHEST, LEGS, FEET});
    }
    public int getMaxLevel() { return 4; }
    public float getResistanceMultiplier(int level) {
        return 1.0f - level * 0.05f; // 每级减5%
    }
}
```

**ModEnchantments** — 附魔注册中心，使用 `Registries.ENCHANTMENT` 注册：

```java
// src/main/java/org/attack_type/enchantment/ModEnchantments.java
public static final SinEnchantment WRATH = registerSin(SinType.WRATH);
// ... 7种罪孽
public static final PhysicalResistanceEnchantment SLASH_RESISTANCE = registerPhys(AttackType.SLASH);
// ... 3种物理抗性
```

### 7. 网络通信

**ModPackets** — 定义两个网络包标识符：

```java
// src/main/java/org/attack_type/network/ModPackets.java
RESISTANCE_SYNC   // 服务端→客户端：同步抗性数据
RESISTANCE_UPDATE // 客户端→服务端：玩家提交抗性变更
```

**NetworkHandler**（服务端）— 接收玩家抗性更新并广播：

```java
// src/main/java/org/attack_type/network/NetworkHandler.java
ServerPlayNetworking.registerGlobalReceiver(RESISTANCE_UPDATE, (server, player, ...) -> {
    // 读取玩家提交的10个抗性值 + totalProduct
    // 调用 normalize() 确保总积一致
    // 广播回客户端
});
```

**NetworkHandlerClient**（客户端）— 接收服务端同步：

```java
// src/client/java/org/attack_type/network/NetworkHandlerClient.java
ClientPlayNetworking.registerGlobalReceiver(RESISTANCE_SYNC, (client, ...) -> {
    // 反序列化并缓存到 ClientResistanceCache
});
```

**ClientResistanceCache** — 客户端内存缓存，GUI 读取/写入：

```java
// src/client/java/org/attack_type/network/ClientResistanceCache.java
public class ClientResistanceCache {
    private static ResistanceProfile cachedProfile;
}
```

### 8. ResistanceScreen — 抗性分配 GUI

U 键打开，每个抗性提供 ±0.1 和 ±0.01 微调按钮，应用后通过 `RESISTANCE_UPDATE` 发包到服务端。

```java
// src/client/java/org/attack_type/gui/ResistanceScreen.java
public class ResistanceScreen extends Screen {
    // 3种物理抗性 + 7种罪孽抗性 = 10行 × 4个按钮
    // 实时显示当前值、等级标签、总积
}
```

### 9. Attack_typeClient — 客户端入口

注册 U 键快捷键和网络包：

```java
// src/client/java/org/attack_type/client/Attack_typeClient.java
KeyBinding resistanceKey = new KeyBinding("key.attack_type.resistance",
    InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.attack_type");
// 按键处理：打开 ResistanceScreen
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    while (resistanceKey.wasPressed()) {
        MinecraftClient.getInstance().setScreen(new ResistanceScreen());
    }
});
```

### 10. Attack_type — 主入口

```java
// src/main/java/org/attack_type/Attack_type.java
public class Attack_type implements ModInitializer {
    public void onInitialize() {
        ModEnchantments.initialize();       // 注册10种附魔
        NetworkHandler.registerServer();    // 注册服务端网络包
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkHandler.sendResistanceSync(handler.player); // 玩家加入时同步
        });
    }
}
```

---

## 构建与安装

```bash
./gradlew build    # 构建 mod，产物在 build/libs/
```

将 `build/libs/Attack_Type-1.0-SNAPSHOT.jar` 放入 Minecraft 1.20.1 的 `mods` 文件夹。

**依赖：**
- Fabric Loader ≥ 0.14
- Fabric API

---

## 按键操作

| 按键 | 功能 |
|------|------|
| U | 打开/关闭抗性分配 GUI |