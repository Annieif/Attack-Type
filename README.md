# Attack Type Mod

Minecraft Fabric 1.20.1 模组，实现「攻击类型」与「罪孽属性」复合伤害系统。

## 核心概念

### 物理攻击类型

所有直接攻击被归为以下 4 种物理类型：

| 类型 | 枚举值      | 判定来源                  |
|----|----------|-----------------------|
| 斩击 | `SLASH`  | 剑、斧、三叉戟（近战模式）         |
| 突刺 | `PIERCE` | 箭矢（含药箭）、投掷三叉戟         |
| 打击 | `BLUNT`  | 空手、镐、锹、雪球、鸡蛋等其余物品/弹射物 |
| 无  | `NONE`   | 摔落、窒息、火焰、中毒等非实体来源伤害   |

> **弹射物分类规则**: 并非所有弹射物都是突刺。仅 `ArrowEntity` 和 `TridentEntity`（投掷模式）为突刺；雪球、鸡蛋等弹射物均为打击。

### 罪孽属性 (七大罪)

| 罪孽 | 枚举值        | 颜色 | 附魔 ID                  |
|----|------------|----|------------------------|
| 暴怒 | `WRATH`    | 红  | `attack_type:wrath`    |
| 色欲 | `LUST`     | 橙  | `attack_type:lust`     |
| 怠惰 | `SLOTH`    | 黄  | `attack_type:sloth`    |
| 暴食 | `GLUTTONY` | 草绿 | `attack_type:gluttony` |
| 忧郁 | `GLOOM`    | 天蓝 | `attack_type:gloom`    |
| 傲慢 | `PRIDE`    | 深蓝 | `attack_type:pride`    |
| 嫉妒 | `ENVY`     | 紫  | `attack_type:envy`     |

每种罪孽对应一个武器附魔（最高 5 级），附魔后攻击附带该罪孽属性。

---

## 伤害计算

### 伤害公式

```
applyDamage(amount × physMult) + sinDamage
```

其中：

- `physMult = 物理抗性 × 护甲物理抗性附魔`（4 件护甲独立乘算，每件 = 1 - 0.05 × 附魔等级）
- `sinDamage = (罪孽等级 × 3 + 1) × 罪孽抗性`
- `applyDamage()` 为原版方法，内部会再应用护甲/保护附魔等减伤

**关键**: 罪孽伤害在 `applyDamage()` 之后直接叠加，不受物理减伤影响。

### 罪孽等级

| 等级   | 伤害加成 | 碎片消耗 |
|------|------|------|
| Lv.1 | +4   | 40   |
| Lv.2 | +7   | 70   |
| Lv.3 | +10  | 100  |

### 抗性系统

每个实体拥有 10 种抗性值（3 物理 + 7 罪孽），范围 ≥0.01（无上限），精确到 2 位小数。

| 等级 | 乘数            | 效果          |
|----|---------------|-------------|
| 致命 | > 1.5         | 受到该类型伤害大幅增加 |
| 脆弱 | > 1.0 (≠ 1.0) | 受到该类型伤害增加   |
| 一般 | = 1.0         | 正常伤害        |
| 耐性 | < 1.0         | 受到该类型伤害减少   |
| 抵抗 | ≤ 0.5         | 受到该类型伤害大幅减少 |
| 免疫 | = 0.0         | 完全免疫该类型伤害   |

所有抗性的几何平均值受 `totalProduct` 约束，GUI 修改后自动归一化。

---

## 罪孽碎片系统（玩家专属）

### 碎片获取途径

每种罪孽有独立的碎片获取方式，体现各罪孽的「性格」：

#### 暴怒 — 连杀

计算连续击杀生物之间的时间间隔：

| 间隔     | 碎片 |
|--------|----|
| ≤ 1 秒  | +7 |
| ≤ 2 秒  | +5 |
| ≤ 5 秒  | +3 |
| ≤ 10 秒 | +1 |

#### 色欲 — 繁殖与变异

| 行为                              | 碎片  |
|---------------------------------|-----|
| 繁殖任意生物                          | +1  |
| 砸鸡蛋                             | +1  |
| 治疗僵尸村民                          | +10 |
| 闪电使猪→僵尸猪灵/苦力怕→闪电苦力怕/哞菇→变异/村民→女巫 | +5  |
| 僵尸在水中转化为溺尸                      | +5  |

#### 怠惰 — 静止与睡眠

| 行为            | 碎片 |
|---------------|----|
| 静止不动（不跑不跳）每分钟 | +3 |
| 完整睡一觉         | +5 |

> 怠惰特性：可反复睡觉（白天也可睡到晚上）。

#### 暴食 — 进食

| 行为   | 碎片 |
|------|----|
| 进食一次 | +1 |

> 暴食特性：可反复进食（满饱食度也可吃）。

#### 忧郁 — 受伤与目击

| 行为                   | 碎片 |
|----------------------|----|
| 每受到 1 点伤害            | +1 |
| 目击其他生物受伤（非玩家造成），每点伤害 | +1 |

#### 傲慢 — 成就与生产

| 行为                   | 碎片  |
|----------------------|-----|
| 每达成一个成就/进度           | +10 |
| 每累计制作 27 组（1728 个）物品 | +1  |
| 每烧炼/酿造/附魔一次          | +2  |

#### 嫉妒 — 攀比与目击

| 行为                 | 碎片 |
|--------------------|----|
| 目击装备等级高于自身的实体（每分钟） | +3 |
| 目击任何实体发动罪孽属性攻击（每次） | +1 |

> 装备等级：下界合金 > 钻石 > 金 > 铁 > 石头 > 木 > 无。附魔装备 > 无附魔装备。

### 碎片阈值

| 阈值   | 名称 | 行为                     |
|------|----|------------------------|
| 500  | 溢出 | HUD 显示"溢"，下次对应罪孽攻击自动触发 |
| 1000 | 即死 | HUD 显示"死"，攻击者被直接击杀     |

### 手动触发

按 `\` 键手动触发当前激活罪孽（消耗碎片）：

- 连按 1 次 → Lv.1（消耗 40 碎片）
- 连按 2 次 → Lv.2（消耗 70 碎片）
- 连按 3 次 → Lv.3（消耗 100 碎片）

**附魔减耗**: 武器持有对应罪孽附魔时，每级附魔减少 2 点消耗（最低 1）。公式：`max(1, baseCost - 2 × enchantLevel)`

按 `[` / `]` 键切换当前激活罪孽类型。

### 激活持续

| 等级   | 持续 tick | 持续秒 |
|------|---------|-----|
| Lv.1 | 80      | 4s  |
| Lv.2 | 140     | 7s  |
| Lv.3 | 200     | 10s |

---

## 非玩家生物罪孽触发

非玩家 LivingEntity 不使用碎片，每次攻击独立掷骰：

```
对 7 种罪孽各掷一次:
  触发率 = 5% + 附魔等级 × 10%
  等级   = 范围 [max(1, 附魔), min(3, 附魔+1)] 内随机
```

使用确定性种子（UUID + 坐标 + 时间），保证多端一致。

| 场景            | 暴怒触发率 | 等级范围     |
|---------------|-------|----------|
| 僵尸空手          | 5%    | 1 ~ 3    |
| 僵尸持有「暴怒 II」剑  | 25%   | 2 ~ 3    |
| 掠夺者持有「怠惰 IV」弩 | 45%   | 3（clamp） |

---

## 非玩家实体抗性衰减

非玩家实体每 2 个游戏天（48000 tick = 40 分钟）自动衰减一次：

1. 随机交换抗性值（30 次随机交换，因子 0.8~1.2）
2. 归一化到当前总乘积
3. 每 4 个完整周期（8 游戏天），总乘积降低 0.01（最低 0.01）

玩家实体不受此衰减影响（抗性由 GUI 手动管理）。

---

## 附魔系统

### 罪孽附魔 (7 种)

| 属性    | 值                    |
|-------|----------------------|
| 稀有度   | RARE                 |
| 最高等级  | 5                    |
| 适用槽位  | 主手                   |
| 附魔台权重 | 1 + (level - 1) × 10 |

### 物理抗性附魔 (3 种)

| 属性    | 值                             |
|-------|-------------------------------|
| 稀有度   | RARE                          |
| 最高等级  | 4                             |
| 适用槽位  | 头盔、胸甲、护腿、靴子                   |
| 附魔台权重 | 1 + (level - 1) × 8           |
| 减伤公式  | 每件护甲 (1 - 0.05 × level)，4 件乘算 |

---

## 按键操作

| 按键 | 功能                   |
|----|----------------------|
| U  | 打开抗性分配 GUI           |
| [  | 激活罪孽向左切换             |
| ]  | 激活罪孽向右切换             |
| \  | 触发罪孽攻击（连按 1~3 次选择等级） |

---

## 罪孽碎片 HUD

屏幕左上角渲染 7 个 32×32 罪孽图标，显示碎片数量：

| 数量      | 显示   | 颜色 |
|---------|------|----|
| 0       | "0"  | 灰  |
| 1~499   | 实际数字 | 白  |
| 500~999 | "溢"  | 橙  |
| ≥1000   | "死"  | 红  |

- 当前选中罪孽：金色四角边框高亮
- 激活中：图标右上角显示 L1/L2/L3
- 图标透明度随碎片数量从 50% 渐变到 100%

---

## 抗性分配 GUI

按 U 键打开，显示 3 物理 + 7 罪孽的抗性输入框（范围 0.00~5.00），越界输入框边框变红。

底部显示总乘积（≥1.0 绿色 OK，<1.0 红色警告）。点击 Apply 提交到服务端并自动归一化。

---

## 调试指令

权限等级 2（OP），根命令 `/attacktype`。

### 抗性管理

| 指令                              | 说明                   |
|---------------------------------|----------------------|
| `/attacktype get [实体]`          | 查看实体抗性 + 等级标签        |
| `/attacktype set <类型> <值> [实体]` | 设置抗性值（≥0.0，无上限）      |
| `/attacktype reset [实体]`        | 重置为随机抗性              |
| `/attacktype tick [实体]`         | 手动触发一次抗性衰减           |
| `/attacktype test`              | 生成 10 只极端抗性测试狗（互相敌对） |

### 碎片管理

| 指令                                        | 说明               |
|-------------------------------------------|------------------|
| `/attacktype fragment get [玩家]`           | 查看碎片数据 + 溢出/即死状态 |
| `/attacktype fragment add <罪孽> <数量> [玩家]` | 增加碎片             |
| `/attacktype fragment set <罪孽> <数量> [玩家]` | 设置碎片数            |

类型参数（大小写不敏感）：`slash` / `pierce` / `blunt` / `wrath` / `lust` / `sloth` / `gluttony` / `gloom` / `pride` /
`envy`

---

## 代码架构

```
src/
├── client/java/org/attack_type/
│   ├── client/
│   │   ├── Attack_typeClient.java          # 客户端入口: 按键注册 + HUD + 网络
│   │   └── Attack_typeDataGenerator.java   # 数据生成器入口（预留）
│   ├── fragment/
│   │   └── ClientFragmentCache.java        # 客户端碎片缓存（仅供 HUD 读取）
│   ├── gui/
│   │   ├── ResistanceScreen.java           # 抗性分配 GUI
│   │   └── SinFragmentHUD.java             # 碎片 HUD 渲染
│   └── network/
│       ├── ClientResistanceCache.java      # 客户端抗性缓存
│       └── NetworkHandlerClient.java       # 客户端网络包接收
├── main/java/org/attack_type/
│   ├── Attack_type.java                    # 模组主入口
│   ├── advancement/
│   │   └── ModAdvancements.java            # 成就/进度系统
│   ├── api/
│   │   ├── AttackType.java                # 物理攻击类型枚举
│   │   ├── AttackTypeMapper.java          # 攻击类型/罪孽判定核心逻辑
│   │   ├── ResistanceProfile.java         # 抗性数据模型 + NBT 序列化
│   │   └── SinType.java                   # 罪孽属性枚举
│   ├── command/
│   │   └── ResistanceCommand.java         # /attacktype 调试命令
│   ├── component/
│   │   └── ResistanceManager.java         # 全局抗性管理 + 周期衰减
│   ├── config/
│   │   └── ModConfig.java                 # 全局配置（热重载 + 预设）
│   ├── effect/
│   │   ├── BurstEffect.java               # 爆发效果（反伤 + 抗性调整 + 伤害转换）
│   │   ├── CostIncreaseEffect.java        # 消耗增加效果
│   │   ├── EffectCategory.java            # 效果分类枚举（强化/守护/提升/弱化/易损/降低）
│   │   ├── FragmentBoostEffect.java       # 碎片获取增加效果
│   │   ├── FragmentDrainEffect.java       # 碎片扣除效果
│   │   ├── IgnoreResistanceEffect.java    # 无视抗性效果
│   │   ├── ModPotions.java                # 药水注册 + 两段式酿造配方
│   │   ├── ModStatusEffects.java          # 状态效果注册中心（60 个效果）
│   │   ├── NoCostEffect.java              # 无消耗效果
│   │   └── SinCategoryEffect.java         # 罪孽/物理分类状态效果
│   ├── enchantment/
│   │   ├── ModEnchantments.java           # 附魔注册中心
│   │   ├── PhysicalResistanceEnchantment.java  # 物理抗性护甲附魔
│   │   └── SinEnchantment.java            # 罪孽武器附魔
│   ├── fragment/
│   │   ├── SinFragmentAcquisition.java   # 7种罪孽碎片获取系统
│   │   ├── SinFragmentData.java           # 碎片数据模型 + 消耗常量
│   │   └── SinFragmentManager.java        # 碎片管理（增删/触发/溢出/即死）
│   ├── mixin/
│   │   ├── MixinLivingEntity.java         # LivingEntity 伤害计算注入 + 粒子 + 效果集成
│   │   ├── MixinPlayerEntity.java         # 暴食：允许满饱食度进食
│   │   ├── MixinServerPlayerEntity.java   # 怠惰：随时睡觉 + 睡眠检测
│   │   ├── MixinLightningStrike.java      # 色欲：闪电击中变异检测
│   │   ├── MixinAnimalEntity.java         # 色欲：动物繁殖检测
│   │   ├── MixinZombieVillagerEntity.java # 色欲：僵尸村民治愈检测
│   │   ├── MixinZombieEntity.java         # 色欲：僵尸转溺尸检测
│   │   └── MixinPlayerAdvancementTracker.java # 傲慢：成就达成检测
│   └── network/
│       ├── ModPackets.java                # 网络包通道标识符
│       └── NetworkHandler.java            # 服务端网络包处理 + 推送
└── main/resources/
    ├── assets/attack_type/lang/
    │   ├── zh_cn.json                      # 简体中文
    │   └── en_us.json                      # English
    └── assets/attack_type/textures/gui/sin_fragment/
        ├── wrath.png ~ envy.png            # 7 个罪孽图标 (32×32)
```

---

## 网络通信

| 通道                  | 方向  | 内容                                |
|---------------------|-----|-----------------------------------|
| `resistance_sync`   | S→C | 抗性配置 NBT 全量同步                     |
| `resistance_update` | C→S | 玩家提交抗性修改（10×float + totalProduct） |
| `fragment_sync`     | S→C | 碎片数据 NBT 全量同步                     |
| `fragment_trigger`  | C→S | 手动触发罪孽（ordinal + level）           |

---

## 状态效果与药水系统

### 效果分类

模组新增 60 个状态效果（StatusEffect），分为三大类：

#### 罪孽/物理分类效果（48 种）

6 个分类 × (7 罪孽 + 1 物理) = 48 种：

| 类别 | 关键词 | 效果 | 公式 |
|------|--------|------|------|
| 强化 | `strengthen` | 造成该类型伤害 +N% | `1 + 0.3 × (amplifier + 1)` |
| 守护 | `guard` | 受到该类型伤害 -N% | `1 - 0.3 × (amplifier + 1)` |
| 提升 | `boost` | 结算伤害 +N | `2 × (amplifier + 1)` |
| 弱化 | `weaken` | 造成该类型伤害 -N% | `1 - 0.3 × (amplifier + 1)` |
| 易损 | `vulnerable` | 受到该类型伤害 +N% | `1 + 0.3 × (amplifier + 1)` |
| 降低 | `reduce` | 结算伤害 -N | `2 × (amplifier + 1)` |

#### 通用效果（5 种）

| 效果 | 关键词 | 说明 |
|------|--------|------|
| 碎片获取增加 | `fragment_boost` | 每次获取碎片 +N |
| 无消耗 | `no_cost` | 触发罪孽不消耗碎片 |
| 无视抗性 | `ignore_resistance` | N% 伤害无视物理/罪孽抗性 |
| 碎片扣除 | `fragment_drain` | 每 5s 扣除 N 碎片 |
| 消耗增加 | `cost_increase` | 触发罪孽消耗 +N% |

#### 爆发效果（7 种 × 5 级）

以"爆发的忧郁"为例：

| 效果 | 公式 |
|------|------|
| 罪孽抗性 | +0.3 × N |
| 物理抗性 | -0.5 × N |
| 反伤 | 受到攻击时对攻击者造成 3 × N 点对应罪孽伤害 |
| 伤害转换 | 所有受到的伤害改为对应罪孽属性 |

### 酿造系统

采用**两段式酿造**：

```
粗制药水 + 罪孽材料 → 罪孽基础药水 → + 分类材料 → 具体药水
```

| 罪孽 | 基础材料 | 分类材料 |
|------|---------|---------|
| 暴怒 | 烈焰棒 | 火焰弹(强化) / 铁锭(守护) / 海晶碎片(提升) / 毒马铃薯(弱化) / 线(易损) / 爆裂紫颂果(降低) |
| 色欲 | 玫瑰丛 | 同上 |
| 怠惰 | 羽毛 | 同上 |
| 暴食 | 腐肉 | 同上 |
| 忧郁 | 墨囊 | 同上 |
| 傲慢 | 金锭 | 同上 |
| 嫉妒 | 绿宝石 | 同上 |
| 爆发 | — | 回响碎片（加入罪孽基础药水） |

**升级与转换：**
- 萤石粉 → 提升 1 级（罪孽/物理/通用最高 Lv3，爆发最高 Lv5）
- 红石粉 → 延长时长（基础 3min → 延长 8min）
- 火药 → 喷溅型药水
- 龙息 → 滞留型药水

---

## 技术要点

- **ThreadLocal 缓存**: `MixinLivingEntity` 使用 `PENDING_PHYS_MULT` 和 `PENDING_SIN_DAMAGE` 两个 ThreadLocal 在 HEAD
  注入和 ModifyArg 注入间传递数据，确保同一 `damage()` 调用中抗性乘数和罪孽伤害配对一致
- **ConcurrentHashMap**: `ResistanceManager` 和 `SinFragmentManager` 使用线程安全 Map 管理实体/玩家数据
- **确定性随机**: 非玩家生物罪孽掷骰使用 `(UUID LSB ^ worldTime ^ 坐标)` 作为种子，保证多端一致
- **NBT 序列化**: `ResistanceProfile` 和 `SinFragmentData` 支持完整的 NBT 读写，用于持久化存储和网络同步
- **几何平均归一化**: `ResistanceProfile.normalize()` 使用 `(totalProduct / curProduct)^(1/10)` 等比缩放 10 个抗性值
- **罪孽粒子效果**: 每种罪孽攻击在目标实体周围生成 12 个 `DustParticleEffect` 粉尘粒子，颜色对应罪孽属性（红/橙/黄/草绿/天蓝/深蓝/紫），便于区分攻击类型
- **碎片获取系统**: `SinFragmentAcquisition` 通过 Fabric API 事件 + 8个 Mixin 注入实现 7 种罪孽的独立碎片获取逻辑
- **QoL 特性**: `MixinPlayerEntity` 允许满饱食度进食，`MixinServerPlayerEntity` 允许白天反复睡觉

---

## 构建

```bash
./gradlew build
```

产物: `build/libs/Attack_Type-1.0-SNAPSHOT.jar`

依赖:

- Minecraft 1.20.1
- Fabric Loader ≥ 0.14
- Fabric API

---

## 系统规则汇总

### 攻击类型判定规则

1. 弹射物仅 `ArrowEntity` / `TridentEntity`（投掷）→ 突刺；其余 → 打击
2. 近战武器（剑/斧/三叉戟）→ 斩击；空手/其他物品 → 打击
3. 非实体来源（摔落/火焰/中毒等）→ NONE

### 罪孽触发规则

4. 玩家：手动触发（消耗碎片）或溢出自动触发（≥500 碎片）
5. 非玩家：独立掷骰 7 种罪孽，触发率 5% + 附魔等级 × 10%，等级范围 [max(1, enchantLv), min(3, enchantLv+1)]
6. 碎片 ≥1000 时攻击者被即死（自反）

### 伤害计算规则

7. `applyDamage(amount × physMult) + sinDamage`
8. `physMult = 物理抗性 × Π(1 - 0.05 × armorEnchantLevel)`
9. `sinDamage = (sinLevel × 3 + 1) × 罪孽抗性`
10. 罪孽伤害在 `applyDamage()` 之后叠加，不受物理减伤

### 抗性管理规则

11. 所有抗性值钳制到 ≥0.01（无上限），保留 2 位小数
12. 归一化使用几何平均缩放，保持乘积 = totalProduct
13. 玩家抗性由 GUI 手动管理，不自动衰减
14. 非玩家实体每 48000 tick 随机化抗性，每 4 周期 totalProduct -0.01
15. 玩家首次创建时随机化抗性并归一化

### 碎片管理规则

16. 手动触发消耗 = max(1, baseCost - 2 × enchantLevel)
17. 溢出阈值 500，即死阈值 1000
18. 玩家断线自动清理碎片数据
19. 任意罪孽碎片首次达到 500，总积（totalProduct）-0.1，最低降至 0.1

### 附魔规则

20. 罪孽附魔：RARE/5 级/主手/任意物品可用
21. 物理抗性附魔：RARE/4 级/四护甲槽/每级 -5% 伤害

### 网络规则

22. 抗性修改必须经过服务端归一化后再同步回客户端
23. 碎片触发由服务端验证消耗，客户端仅发送请求
24. 玩家加入时自动同步抗性 + 碎片数据

---

## 代码审计结果

### 发现的问题

1. **`SinFragmentConfig` 未使用** — 该文件定义了与 `SinFragmentData` 重复的常量，但代码中实际使用 `SinFragmentData`
   的常量。建议删除或统一引用。

2. **`ResistanceProfile.getResistanceLabel()` 缺失** — 该方法的调用已存在于 `ResistanceCommand` 中，但方法定义缺失。**已修复
   **：添加了基于抗性值返回等级标签键的方法。

3. **`Attack_typeDataGenerator` 为空实现** — 数据生成器入口未实现任何数据生成逻辑，仅创建空 Pack。

### 已验证一致

- 语言文件（zh_cn.json / en_us.json）与枚举值完全对应
- 伤害公式在 README 与代码中一致
- 所有常量值与文档描述匹配
- 网络包序号与注册顺序一致
- ThreadLocal 缓存正确清理（HEAD 注入 set，ModifyArg 注入 remove）