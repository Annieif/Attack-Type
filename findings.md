# Attack Type Mod — 研究发现

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
| 色欲 | LUST | 粉 |
| 怠惰 | SLOTH | 灰 |
| 暴食 | GLUTTONY | 橙 |
| 忧郁 | GLOOM | 蓝 |
| 傲慢 | PRIDE | 紫 |
| 嫉妒 | ENVY | 绿 |