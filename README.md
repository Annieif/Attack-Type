# Attack Type Mod

Minecraft Fabric 1.20.1 模组，实现「攻击类型」和「罪孽属性」伤害系统。所有攻击具有物理类型（斩击/突刺/打击）和罪孽属性（七大罪），每个实体拥有可变的抗性配置；玩家额外拥有罪孽碎片系统，可积累/消耗碎片主动触发高强度罪孽伤害。

## 伤害系统规则

### 攻击类型

| 类型 | 来源 |
|------|------|
| **斩击 (SLASH)** | 剑、斧、三叉戟（近战） |
| **突刺 (PIERCE)** | 箭矢（含药箭）、投掷三叉戟 |
| **打击 (BLUNT)** | 空手、镐、锹、雪球、鸡蛋等其余物品和弹射物 |
| **无类型 (NONE)** | 摔落、窒息、中毒、火焰等非物理伤害 |

> **弹射物分类规则：** 并非所有弹射物都是突刺。仅箭矢（ArrowEntity）和投掷三叉戟（TridentEntity）为突刺，雪球、鸡蛋等弹射物均为打击。

### 罪孽属性

七种罪孽附魔：暴怒(WRATH)、色欲(LUST)、怠惰(SLOTH)、暴食(GLUTTONY)、忧郁(GLOOM)、傲慢(PRIDE)、嫉妒(ENVY)。最高5级，适用于所有物品。

### 罪孽触发

#### 玩家（碎片驱动）

玩家攻击的罪孽属性由「罪孽碎片」和「激活罪孽」共同决定（不再是纯附魔概率）。

- **碎片来源**：玩家受罪孽伤害时积累对应类型的碎片（对方有对应罪孽附魔 → 碎片）
- **溢出阈值 500**：碎片 ≥500 时该类型图标显示"溢"，下一次该罪孽攻击自动触发（优先对应附魔类型）
- **即死阈值 1000**：碎片 ≥1000 时显示"死"，直接秒杀攻击者（自反）
- **手动触发**：按 `\` 键消耗碎片触发当前激活罪孽，消耗由等级决定：
  - Lv.1：40 碎片；Lv.2：70 碎片；Lv.3：100 碎片
  - 武器拥有对应罪孽附魔时：每 1 附魔级减少 2 点消耗（最低 1）
- **激活罪孽切换**：按 `[` / `]` 键切换当前激活的罪孽类型（HUD 图标高亮）

#### 其他生物（随机 + 附魔加成）

非玩家 LivingEntity（僵尸、骷髅、监守者…）不使用碎片，改为每次攻击独立掷骰：

```
对 7 种罪孽各掷一次：

触发率 = 5% + 附魔等级 × 10%
等级   = 范围 [max(1, 附魔), min(3, 附魔+1)] 内随机
```

示例：
| 场景 | 暴怒触发率 | 等级范围 |
|------|-----------|---------|
| 僵尸空手 | 5% | 1 ~ 3 |
| 僵尸持有「暴怒 II 剑」 | 5+20 = 25% | 2 ~ 3 |
| 掠夺者持有「怠惰 IV 弩」 | 5+40 = 45% | 3（clamp） |

伤害生效前缓存（ThreadLocal）保证本次攻击的「罪孽类型 / 等级」配对一致。

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
- 单次攻击只取 1 种罪孽（碎片/随机中选中的）

### 抗性系统

- 实体初始所有抗性为 ×1.0，总积为 1.0
- 每 2 天（48000 ticks）随机重分配（玩家除外）
- 每 4 个周期总积减少 0.01
- 玩家通过 U 键打开 GUI 手动分配抗性，总积保持不变
- GUI 数值 clamp 在 [0, 5.0]，越界输入框边框变红并附加红色 `>` / `<` 标记

---

## 罪孽碎片 HUD

屏幕上方渲染 7 个带边框的 32×32 罪孽图标（按 Wrath/Lust/Sloth/Gluttony/Gloom/Pride/Envy 顺序），图标中心显示当前数量：

| 数量 | 显示 | 颜色 |
|------|------|------|
| 0 | 0 | 灰 0x999999 |
| 1~499 | 实际数字 | 白 0xFFFFFF |
| 500~999 | "溢" | 橙 0xFFBB44 |
| ≥1000 | "死" | 红 0xFF6666 |

- 激活罪孽（`[` / `]` 选择）：图标金色四角边框高亮
- 溢出/即死：图标外圈对应颜色粗边框

---

## 按键操作

| 按键 | 功能 |
|------|------|
| **U** | 打开/关闭抗性分配 GUI（ResistanceScreen） |
| **[** | 切换激活罪孽：向左切换（Envy → Pride → … → Wrath） |
| **]** | 切换激活罪孽：向右切换（Wrath → Lust → … → Envy） |
| **\\** | 触发激活罪孽攻击（按激活等级消耗对应碎片） |

---

## 代码架构

```
src/
├── client/java/org/attack_type/
│   ├── client/Attack_typeClient.java       # 客户端入口：键位注册 + 网络
│   ├── gui/
│   │   ├── ResistanceScreen.java           # 抗性分配 GUI（单列、无阴影、越界提示）
│   │   └── SinFragmentHUD.java             # 7 个罪孽图标 HUD（碎片数量 + 状态边）
│   └── network/
│       ├── ClientResistanceCache.java
│       ├── ClientFragmentCache.java        # 客户端激活罪孽缓存
│       └── NetworkHandlerClient.java       # 收 RESISTANCE_SYNC / FRAGMENT_SYNC
├── main/java/org/attack_type/
│   ├── Attack_type.java
│   ├── api/
│   │   ├── AttackType.java                 # SLASH / PIERCE / BLUNT / NONE
│   │   ├── AttackTypeMapper.java           # 攻击类型 + 罪孽判定（玩家/非玩家）
│   │   ├── ResistanceProfile.java          # 3物理+7罪孽 抗性 + totalProduct
│   │   └── SinType.java                    # WRATH…ENVY 七大罪
│   ├── component/ResistanceManager.java    # 实体抗性周期更新
│   ├── enchantment/
│   │   ├── ModEnchantments.java            # 7罪孽 + 3物理抗性
│   │   ├── PhysicalResistanceEnchantment.java
│   │   └── SinEnchantment.java
│   ├── fragment/
│   │   ├── SinFragmentData.java            # 7种碎片计数 + 消耗常量(40/70/100) + 激活罪孽 (L1/L2/L3)
│   │   ├── SinFragmentManager.java         # 碎片增删、触发、溢出即死判定
│   │   └── SinFragmentConfig.java          # 自动触发配置（预留，暂未启用）
│   ├── mixin/MixinLivingEntity.java        # damage HEAD / applyDamage ModifyArg
│   ├── command/ResistanceCommand.java      # /attacktype get|set|reset|tick|fragment
│   └── network/
│       ├── ModPackets.java                 # RESISTANCE_SYNC|UPDATE / FRAGMENT_SYNC|TRIGGER
│       └── NetworkHandler.java             # 服务端收包 + 广播同步
└── main/resources/assets/attack_type/lang/
    ├── zh_cn.json                          # 中文：界面/指令/附魔/标签
    └── en_us.json                          # 英文：界面/指令/附魔/标签
```

---

## 调试指令（需 OP 权限 / permission level 2）

### 抗性

| 指令 | 说明 |
|------|------|
| `/attacktype get [实体]` | 查看自身或指定实体的抗性值 + Total Product |
| `/attacktype set <类型> <值> [实体]` | 设置指定抗性类型值（0.0~5.0 clamp） |
| `/attacktype reset [实体]` | 重置实体抗性为默认随机值 |
| `/attacktype tick [实体]` | 强制触发一次抗性周期更新 |

### 碎片（玩家专用）

| 指令 | 说明 |
|------|------|
| `/attacktype fragment get [玩家]` | 查看 7 种碎片数 + 激活罪孽/等级（溢出、即死标记） |
| `/attacktype fragment add <罪孽> <数量> [玩家]` | 追加指定罪孽碎片 |
| `/attacktype fragment set <罪孽> <数量> [玩家]` | 强制设置碎片数（可直接到 500/1000 看特效） |

`<罪孽>` 枚举名大小写兼容：`wrath / lust / sloth / gluttony / gloom / pride / envy`

类型参数：`slash`、`pierce`、`blunt`、`wrath`、`lust`、`sloth`、`gluttony`、`gloom`、`pride`、`envy`

---

## 代码解析

### 1. 核心枚举

**AttackType** — 三种物理攻击类型 + NONE：

```java
public enum AttackType { SLASH, PIERCE, BLUNT, NONE; }
```

**SinType** — 七种罪孽属性：

```java
public enum SinType { WRATH, LUST, SLOTH, GLUTTONY, GLOOM, PRIDE, ENVY; }
```

### 2. AttackTypeMapper — 攻击 & 罪孽分类器

物理分类：弹射物仅箭矢 / 投掷三叉戟 → PIERCE，其余弹射物 → BLUNT；近战士兵/斧/三叉戟 → SLASH，其它 → BLUNT。

罪孽判定分两条分支（**与旧版"取附魔最高级"不同**）：

- **玩家**：检查当前激活罪孽（优先碎片消耗触发）→ 若某类型溢出且可支付消耗则用该类型
- **非玩家**：`rollMobSin()` 用 `(uuid LSB ^ world.time ^ 坐标)` 作种子对 7 种罪孽各掷 5%+10%/lv，命中且几率最大者胜出（保证 1~3 级范围内，附魔级越高等级范围上限越稳）

缓存 `ThreadLocal<MobSinResult>` 确保同一个 `damage()` 调用里 `getSinType()` + `getSinLevel()` 返回配对一致，每次伤害结束由 `MixinLivingEntity.addSinDamageToFinal` 调 `clearMobSinCache()` 清空。

### 3. ResistanceProfile — 抗性数据模型

```java
Map<AttackType, Float> physicalResistances;   // 3种
Map<SinType, Float> sinResistances;           // 7种
float totalProduct;    // 乘积约束，GUI 分配后 normalize 拉回
long lastUpdateTick;  // 用于 2 天周期检测
```

归一化：`ratio = pow(totalProduct / curProduct, 1/10)` 等比缩放 10 值，使乘积累永远等于 `totalProduct`。

### 4. SinFragmentManager — 玩家碎片系统

```java
// SinFragmentData
int WRATH_fragments, LUST_fragments, ... ENVY_fragments;  // 七种
SinType activeSinType;
int activeSinLevel;           // 1/2/3
long activeSinExpiryTicks;    // 激活持续时间（L1最长，L3最短）
```

阈值：

| 阈值 | 定义 | 行为 |
|------|------|------|
| 500 | OVERFLOW 溢出 | 下次对应罪孽攻击自动触发消耗（扣对应等级），HUD 显示"溢" |
| 1000 | KILL 即死 | 直接 `player.kill()`，HUD 显示"死" |

消耗公式：`cost(level, enchantLv) = max(1, baseCost(level) - 2 × enchantLv)` ，其中 baseCost 为 L1=40, L2=70, L3=100

### 5. MixinLivingEntity — 伤害计算注入

两个 ThreadLocal 贯穿整条 `damage()` 链：`PENDING_PHYS_MULT`（抗性×物理抗性附魔）、`PENDING_SIN_DAMAGE`（(lv×3+1)×罪孽抗性）。

| 注入点 | 作用 |
|------|------|
| `damage HEAD` | 计算 pendingMult 和 pendingSinDamage，玩家 overflow 即死在此触发 |
| `applyDamage ModifyArg` | `finalAmount = amount × mult + sinDamage`；最后清除 MOB_SIN_CACHE |

### 6. 附魔系统

- **罪孽附魔 SinEnchantment × 7**：`Rarity.RARE`、`EnchantmentTarget.BREAKABLE`、`MAINHAND`，`isAcceptableItem(ItemStack)→true`（任意物品），max 5。
- **物理抗性附魔 PhysicalResistanceEnchantment × 3**：`EnchantmentTarget.ARMOR`，四护甲槽，max 4，每级 `resist = 1 - 0.05×lv` 乘积叠加。

### 7. 网络通信

```
ModPackets (Identifiers):
  RESISTANCE_SYNC   S→C   ResistanceProfile NBT（玩家登录 / 更改后广播）
  RESISTANCE_UPDATE C→S   玩家 ResistanceScreen 提交 10×float + product
  FRAGMENT_SYNC     S→C   7×int fragments + activeSin + level + expiry
  FRAGMENT_TRIGGER  C→S   客户端按 \ 触发：激活罪孽 ordinal + 期望等级
```

### 8. 抗性分配 GUI（ResistanceScreen）

单列布局（修复了旧版双列标签叠在左输入框上的 bug）：

```
[ 标签 64px ]  [ 输入框 72px ]  [ 状态 耐性/脆弱 ]  { >5? 红色 > 标记 }
```

- `TextFieldWidget.setDrawsBackground(false)`，自行 `drawFieldBorder()` 画 1px 灰边（overflow 变红色 0xFFFF5555）+ 深灰底，**去除默认橙色选中框阴影不跟随的缺陷**。
- 实时乘算 `product = Π 10抗性`，底部显示 `Total Product: x.xxxx  ✓ OK / ✗ <1`（≥1 绿色，否则红色）。

### 9. 客户端入口（Attack_typeClient）

注册 4 个 KeyBinding（U / [ / ] / \），在 `END_CLIENT_TICK` 里处理：

- `U` → `setScreen(new ResistanceScreen())`
- `[` / `]` → `cycleActiveSin(+1/-1)` → hotbar 区提示 `Active Sin: X`
- `\` → `sendTriggerPacket(level)` 发包给服务端，由服务端确认消耗并应用

### 10. 国际化 (i18n)

`assets/attack_type/lang/{zh_cn,en_us}.json` 覆盖：

- 键位分类 & 名称：`category.attack_type` / `key.attack_type.*`
- 屏幕文本：`screen.attack_type.resistance/apply/physical/sin/total_product_*/ok/lt1/vulnerable/resist`
- HUD：`hud.attack_type.active_sin / overflow_char / death_char`
- 附魔：`enchantment.attack_type.<sin>` + `.desc`、物理抗性 `slash/pierce/blunt_resistance` + `.desc`
- 罪孽名称：`sin.attack_type.<sin>`；物理类型 `attack_type.attack_type.<type>`
- 指令：`cmd.attack_type.err_* / get_title / total_product / physical_title / sin_title / resistance_row / set_ok / reset_ok / tick_ok` 以及碎片 `frag_*` 系列

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

## 多语言

目前已内置：

| 文件 | 语言 |
|------|------|
| `assets/attack_type/lang/zh_cn.json` | 简体中文（默认） |
| `assets/attack_type/lang/en_us.json` | English |

切换 Minecraft 语言设置后，GUI、HUD、指令反馈、附魔名称/描述会全部对应切换。