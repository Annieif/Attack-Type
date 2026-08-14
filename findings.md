# Attack Type Mod — 研究发现

## 性能优化发现（阶段42）

### EnumMap 查找表替代嵌套 switch

**问题：** ModPotions 中 `getSinEffect()` 使用 42 分支嵌套 switch（6 个分类 × 7 种罪孽），每次查找 O(1) 但有大量代码冗余。

**解决方案：** 在 ModStatusEffects 中引入 3 个 EnumMap：
- `SIN_EFFECTS`: `Map<SinType, Map<EffectCategory, SinCategoryEffect>>` — 罪孽分类效果
- `PHYS_EFFECTS`: `Map<EffectCategory, SinCategoryEffect>` — 物理分类效果
- `BURST_EFFECTS`: `Map<SinType, BurstEffect>` — 爆发效果

`regSin()`/`regPhys()`/`regBurst()` 在注册时自动填充查找表，`getSinEffect()`/`getPhysEffect()`/`getBurstEffect()` 提供 O(1) 查找。

### 药水注册代码去重

**问题：** `registerSinPotions`/`registerPhysPotions`/`registerGenericPotions` 三个方法中 Lv1/Lv2/Lv3/Long + 酿造配方代码重复。

**解决方案：** 抽取 `registerPotionLevels()` 方法统一处理等级提升和配方注册。

### 监听器合并

**问题：** SinFragmentAcquisition 中 Gloom 碎片获取使用 2 个独立的 `ALLOW_DAMAGE` 监听器（玩家受伤 + 目击受伤）。

**解决方案：** 合并为 1 个监听器，通过 `instanceof ServerPlayerEntity` 判断分支。

### Tick 降频

**问题：** Pride 傲慢碎片获取每 tick 扫描所有玩家的所有物品合成统计（`Registries.ITEM` 全量遍历），O(n × items) 每 tick 每玩家。

**解决方案：** 添加 `PRIDE_TICK_COUNTER`，每 100 tick（5 秒）扫描一次，降低 99% 计算量。

## 项目结构

- 加载器: Fabric 1.20.1 (Java 17)
- Mod ID: `attack_type`
- 包名: `org.attack_type`
- 使用 split source sets (client/main)
- 已有 `AttackType` 枚举: SLASH, PIERCE, BLUNT, NONE
- 已有 data generator 入口点

## 1.20.1 API 要点

- 注册: `Registry.register(Registry.ITEM, new Identifier(...), ...)`
- 不使用 `Identifier.of()`（1.21+ API）
- 不使用 `Registries.ITEM`（1.21+ API）
- 不使用 DataComponent 系统（1.21+ API）
- `FabricItemSettings` 可用

## 攻击类型映射

| 攻击类型 | 来源 |
|---------|------|
| 斩击 (SLASH) | 剑、斧、三叉戟近战 |
| 突刺 (PIERCE) | 箭矢、投掷三叉戟 |
| 打击 (BLUNT) | 空手、其他物品、雪球、鸡蛋 |
| 无 (NONE) | 摔落、窒息、指令、烫伤、中毒等非物理 |

## 罪孽属性

| 罪孽 | 英文名 | 颜色 |
|------|--------|------|
| 暴怒 | WRATH | 红 |
| 色欲 | LUST | 橙 |
| 怠惰 | SLOTH | 黄 |
| 暴食 | GLUTTONY | 草绿 |
| 忧郁 | GLOOM | 天蓝 |
| 傲慢 | PRIDE | 深蓝 |
| 嫉妒 | ENVY | 紫 |

## 罪孽属性数据采集（阶段29）

### 数据来源分布

| 数据项 | 来源文件 | 说明 |
|--------|---------|------|
| 枚举定义 | `SinType.java` | 7 个枚举值，含翻译键方法 |
| 颜色名称 | `SinType.java` Javadoc | 注释中注明颜色（已更新为图标色） |
| 图标纹理 | `SinFragmentHUD.java` | 7 个 `Identifier` 常量，指向 `textures/gui/sin_fragment/` |
| 碎片消耗 | `SinFragmentData.java` | COST_LEVEL_1=40, COST_LEVEL_2=70, COST_LEVEL_3=100 |
| 碎片阈值 | `SinFragmentData.java` | OVERFLOW=500, KILL=1000 |
| 激活持续 | `SinFragmentData.java` | DURATION_L1=80, L2=140, L3=200 ticks |
| 配置常量(重复) | `SinFragmentConfig.java` | 与 SinFragmentData 重复的常量值 |
| 碎片获取量 | `SinFragmentConfig.java` | MOB_KILL=5, PLAYER_KILL=20, DAMAGE=1 |
| 附魔属性 | `SinEnchantment.java` | RARE, maxLevel=5, MAINHAND, 权重 1+(level-1)×10 |
| 附魔注册 | `ModEnchantments.java` | 按 SinType 枚举注册 7 个附魔 |
| 非玩家触发率 | `AttackTypeMapper.java` | 基础 5% + 附魔等级 × 10% |
| 附魔减耗 | `SinFragmentData.java` | max(1, baseCost - 2 × enchantLevel) |
| 翻译键 | `zh_cn.json` / `en_us.json` | sin.attack_type.<name>, enchantment.attack_type.<name> |
| HUD 颜色 | `SinFragmentHUD.java` | 金角 0xFFFFD866, 溢出 0x99DD5500, 即死 0xBBDD0000, L标签 0xFFEE77 |

### 颜色变更记录

| 罪孽 | 旧颜色 | 新颜色 | 原因 |
|------|--------|--------|------|
| 暴怒 | 红 | 红 | 不变 |
| 色欲 | 粉 | 橙 | 对齐图标 |
| 怠惰 | 灰 | 黄 | 对齐图标 |
| 暴食 | 橙 | 草绿 | 对齐图标 |
| 忧郁 | 蓝 | 天蓝 | 对齐图标 |
| 傲慢 | 紫 | 深蓝 | 对齐图标 |
| 嫉妒 | 绿 | 紫 | 对齐图标 |