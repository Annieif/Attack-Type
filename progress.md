# Attack Type Mod — 进度日志

## 会话 1: 2026-08-10

### 阶段1-7: 核心功能实现
- 创建 AttackType、SinType 枚举
- 实现 ResistanceProfile 抗性数据模型
- 实现 AttackTypeMapper 攻击类型/罪孽映射
- 实现 MixinLivingEntity 伤害计算注入
- 实现 7种罪孽附魔 + 3种物理抗性附魔
- 实现 ResistanceManager 实体抗性管理
- 实现 ResistanceScreen 玩家抗性分配 GUI
- 实现 NetworkHandler 网络同步

### 阶段8: 构建修复
- 修复 AttackType.java BOM 字符问题
- 修复 Registry.ENCHANTMENT → Registries.ENCHANTMENT
- 修复 DamageSource.isProjectile() → ProjectileEntity 检查
- 修复 GUI 代码源集问题（main → client）
- 修复 MatrixStack → DrawContext API 兼容性
- 修复网络代码源集拆分

### SKILL.md 审查改进
- 修复 fabric.mod.json 描述和作者信息
- 修复 maven_group 命名规范
- 添加 mixins.json refmap 配置
- 添加 gradle.properties loom.mixin.defaultRefmapName
- 修复关键 bug：注册 ServerTickEvents 触发实体抗性周期更新
- 添加玩家断线清理
- 移除 ResistanceProfile 无用的 isPlayer() 方法
- 添加附魔描述翻译
- GUI 标签国际化（物理抗性/罪孽抗性/总积）

### 构建结果
- BUILD SUCCESSFUL (2次)
- 产物: build/libs/Attack_Type-1.0-SNAPSHOT.jar

## 会话 2: 2026-08-10

### 阶段9: Git 初始化
- 初始化 git 仓库并创建 .gitignore
- 排除 .gradle/、build/ 等构建产物

### 阶段10: 攻击类型映射修复
- 修复弹射物分类：仅 ArrowEntity 和 TridentEntity 为 PIERCE
- 雪球、鸡蛋等其余弹射物归为 BLUNT

### 阶段11: README 规则完善
- 更新攻击类型来源表，明确弹射物分类规则
- 添加调试指令说明

### 阶段12: 调试指令
- 创建 ResistanceCommand.java
- 支持 /attacktype get/set/reset/tick
- 需要 OP 权限（level 2）
- 添加 ResistanceManager.getOrCreateProfile/resetProfile/syncToPlayer

### 构建结果
- BUILD SUCCESSFUL (3次)

## 会话 3: 2026-08-11

### 阶段23: 文本国际化 + 碎片指令
- zh_cn.json 与 en_us.json 全量翻译键 40+
- ResistanceScreen: 标题/Apply/脆弱/耐性/总积 → 翻译键
- ResistanceCommand: 错误提示、查询/设置/碎片反馈 → 翻译键
- HUD 溢/死 字符 → 翻译键
- Attack_typeClient showCycleMessage → 翻译键
- 碎片指令 `/attacktype fragment add/set/get` 确认可用
- 构建: **BUILD SUCCESSFUL**

### 阶段24: ResistanceScreen UI 修复（完成）
**问题清单（来自用户截图）：**
1. 双列布局导致右列标签（突刺）与左列输入框重叠 → 改单列
2. 输入框（TextFieldWidget）有默认背景阴影，选中光标移动时阴影错位 → 禁用背景 + 自绘简洁边框
3. 数值超过 clamp 上限 (5.0) 时无提示 → 添加红色 ">" 标记 + 红色边框
4. 中文标签 42px 宽度不够，标签、输入框、状态三列间距不合理

**修复步骤（已完成）：**
- 重写 init() 布局：每行一个 `[标签 (labelX)] → [输入框 (fieldX)] → [状态 (statusX)]`，三列居中对齐（totalHalf 公式统一）
- `tf.setDrawsBackground(false)` 删除默认输入框阴影/背景，新增 `drawFieldBorder()` 自绘 1px 简洁灰色边框 + 深色底色
- 越界检测：`raw < 0 或 raw > 5.0` → 边框变红 + 状态右侧追加红色 ">" / "<" 字符（ResistanceScreen.java:122-135, 160, 172-175）
- 标签统一用 64px 宽度（LABEL_W=64），标签与输入框间距 8px，输入框与状态间距 8px
- 构建验证：**BUILD SUCCESSFUL**

**Git 提交待执行（用户要求 use git）。**

- Commit `91ababf`: i18n all strings + fix ResistanceScreen UI (single column, no shadow, >5 overflow indicator) — 9 files changed, +257 -104

### 阶段25: 其他生物罪孽随机触发（完成）
- AttackTypeMapper.getSinType() / getSinLevel() 非玩家分支 → rollMobSin()
- 7 种罪孽各独立掷骰：基础 5%，每级对应附魔 +10% 触发率
- 等级 1~3 随机：无附魔均等 1-3；有附魔时 minLevel=enchantLevel(cap3), maxLevel=enchantLevel+1
- ThreadLocal MOB_SIN_CACHE 保证同一攻击 getSinType 与 getSinLevel 一致；MixinLivingEntity.addSinDamageToFinal 后 clearMobSinCache
- 构建: **BUILD SUCCESSFUL**
- Commit `78ce81b`: Mob sin trigger: non-player mobs get 5% base random sin 1-3 — 3 files changed, +92 -27

### 阶段26: README 更新与 Git 提交（完成）
- README 重写：新增玩家罪孽碎片系统（HUD/阈值500溢/1000死/[ ] \键位）、其他生物随机罪孽（5%+10%/lv）、完整键位表、碎片指令、fragment 包与网络包、单列 UI 修复、i18n 说明
- 构建: **BUILD SUCCESSFUL**
- Commit `8f39e33`: Update README (fragment system, mob sin rules, key map, commands, i18n, UI, architecture) — 3 files changed, +189 -248

### 阶段27: 项目全面审查与优化（完成）
- 全面审查23个Java文件 + 配置文件 + README
- 修复6个问题：
  1. 删除 zh_cn.json / en_us.json 中无效条目 `attack_type.attack_type.fierce`（无对应枚举）
  2. 修正 README 手动触发消耗：100/200/300 → 40/70/100
  3. 修正 README 附魔减耗：10% → 每级 -2
  4. 修复 HUD L1/L2/L3 标签不消失：添加 `worldTime < expiry` 过期检查
  5. 修复 ResistanceScreen 溢出提示被 TextFieldWidget 覆盖：移到 super.render() 之后
  6. 标记 SinFragmentConfig 为预留代码（未被任何代码引用）
- 一致性验证：枚举↔语言文件✓ 伤害公式✓ 网络包✓ 附魔等级✓ 配置文件✓
- 构建: **BUILD SUCCESSFUL**

## 会话 3: 2026-08-11

### 阶段28: 函数级注释 + 代码审计 + 规则汇总 + README重写（完成）

**函数级注释（23个Java文件）：**
- 为所有类、方法、字段添加统一风格的 Javadoc 注释
- 核心 API 文件（AttackType、SinType、ResistanceProfile、AttackTypeMapper）：详细说明枚举值、判定规则、算法流程
- Mixin 注入文件（MixinLivingEntity）：说明 ThreadLocal 缓存机制、注入点职责、伤害公式
- 附魔文件（SinEnchantment、PhysicalResistanceEnchantment、ModEnchantments）：说明附魔属性、触发机制
- 碎片系统（SinFragmentData、SinFragmentManager、SinFragmentConfig、ClientFragmentCache）：说明常量、阈值、触发逻辑
- 抗性管理（ResistanceManager）：说明衰减算法、ConcurrentHashMap 线程安全
- 网络通信（ModPackets、NetworkHandler、NetworkHandlerClient、ClientResistanceCache）：说明包方向、数据格式
- 客户端（Attack_typeClient、Attack_typeDataGenerator）：说明按键绑定、触发窗口机制
- 命令（ResistanceCommand）：说明所有子命令及参数
- GUI（ResistanceScreen、SinFragmentHUD）：说明布局、渲染逻辑、状态显示

**代码审计：**
- 发现 `ResistanceProfile.getResistanceLabel()` 缺失 → **已修复**：添加了基于抗性值返回等级标签键的方法（3个重载）
- 发现 `SinFragmentConfig` 与 `SinFragmentData` 常量重复 → 待清理
- 发现 `Attack_typeDataGenerator` 为空实现 → 已标记
- 验证一致性：语言文件 ↔ 枚举值 ✓，伤害公式 ↔ 代码实现 ✓，常量值 ↔ 文档描述 ✓

**规则汇总（24条）：**
- 攻击类型判定规则（3条）
- 罪孽触发规则（3条）
- 伤害计算规则（4条）
- 抗性管理规则（5条）
- 碎片管理规则（4条）
- 附魔规则（2条）
- 网络规则（3条）

**README 重写：**
- 全新结构：核心概念 → 伤害计算 → 碎片系统 → 生物触发 → 抗性衰减 → 附魔 → 按键 → HUD → GUI → 指令 → 架构 → 网络 → 技术要点 → 构建 → 规则汇总 → 审计结果
- 修正伤害公式表述（明确 applyDamage 包裹关系）
- 添加碎片获取途径表
- 添加激活持续时间表
- 添加完整规则汇总
- 添加代码审计结果章节

## 会话 4: 2026-08-11

### 阶段29: 罪孽颜色修正 + 配置化数据采集 + 生成配置文件（完成）

**颜色修正：**
- 更新 README.md 罪孽属性颜色表，对齐图标实际颜色：
  - 暴怒(红) / 色欲(橙) / 怠惰(黄) / 暴食(草绿) / 忧郁(天蓝) / 傲慢(深蓝) / 嫉妒(紫)
- 同步更新 SinType.java Javadoc 注释中的颜色描述

**数据采集（跨 10 个源文件）：**
- SinType.java — 枚举定义 + 颜色注释 + 翻译键方法
- SinFragmentData.java — 碎片消耗(40/70/100)、阈值(500/1000)、持续(80/140/200 ticks)
- SinFragmentConfig.java — 碎片获取量(5/20/1/1)
- SinFragmentHUD.java — 7 个图标纹理 ID + HUD 渲染颜色常量
- SinFragmentManager.java — 附魔减耗公式 max(1, base-2*level)
- SinEnchantment.java — 附魔属性(RARE/L5/MAINHAND/权重)
- ModEnchantments.java — 7 个罪孽附魔注册
- AttackTypeMapper.java — 非玩家掷骰率(5%+10%/lv, L1~3)
- zh_cn.json / en_us.json — 翻译键全覆盖

**生成配置文件：**
- 创建 `src/main/resources/assets/attack_type/sin_types.json`
- 包含 7 个罪孽的完整属性（名称/颜色/图标/附魔参数）
- 碎片系统参数（消耗/阈值/持续/获取量/减耗）
- 非玩家生物触发参数（概率/等级范围/种子公式）
- HUD 渲染参数（颜色/透明度/位置/尺寸）
- 附魔系统默认参数（sin + physicalResistance）
- 伤害公式参考

## 会话 6: 2026-08-11

### 阶段34: 6项Bug修复（完成）

**修复内容：**

| Bug | 文件 | 修复 |
|-----|------|------|
| GUI 抗性描述不实时更新 | ResistanceScreen.java | 使用用户输入值(raw)替代存储值(v)，应用 getResistanceLabel() |
| 退出重进数据丢失 | MixinLivingEntity.java, SinFragmentManager.java | NBT 读写碎片数据 + 恢复后自动同步 + 新增 setData() 方法 |
| 忧郁碎片过快 | SinFragmentAcquisition.java | `Math.ceil(amount)` → `Math.ceil(amount / 10.0)` |
| 溢出未持续触发Lv.3 | AttackTypeMapper.java | 阈值改为≥100、等级改为Lv.3、消耗100碎片 |
| 总积衰减未应用 | SinFragmentManager.java | `setTotalProduct` 后调用 `normalize()` |
| 碎片溢出变负数 | SinFragmentData.java | `long` 中间值，钳制 [0, 1000000] |

**构建结果：**
- BUILD SUCCESSFUL

### 长期计划制定（完成）
- 更新 task_plan.md：阶段34完成 + 短期TODO(S1-S4) + 长期发展计划(35-42)
- 长期计划覆盖：配置系统、多人适配、视觉增强、成就系统、内容扩展、Mod兼容、发布准备、性能优化

## 会话 5: 2026-08-11

### 阶段30: 罪孽碎片获取系统 + 粒子效果 + QoL（完成）

**实现内容：**

1. **SinFragmentAcquisition.java** — 7种罪孽碎片获取系统
   - 暴怒：`ServerLivingEntityEvents.AFTER_DEATH` 追踪连杀间隔（≤1s +7, ≤2s +5, ≤5s +3, ≤10s +1）
   - 色欲：`UseItemCallback` 鸡蛋+1；`MixinAnimalEntity.breed()` 繁殖+1；`MixinZombieVillagerEntity.finishConversion()` 治愈+10；`MixinLightningStrike.onStruckByLightning()` 闪电变异+10；`MixinZombieEntity.convertTo(DROWNED)` 溺尸+5
   - 怠惰：`ServerTickEvents.END_SERVER_TICK` AFK检测（不跑不跳+在地面，每分钟+1）；`MixinServerPlayerEntity.wakeUp()` 睡眠完成+5
   - 暴食：`UseItemCallback` 进食+1；`MixinPlayerEntity.canConsume()` 允许满饱食度进食
   - 忧郁：`ALLOW_DAMAGE` 玩家受伤（每点+1）+ 目击非玩家造成的生物受伤（每点+1）
   - 傲慢：`MixinPlayerAdvancementTracker.grantCriterion()` 成就+10；`ServerTickEvents` 累计制作27组+1；`onProduction()` 烧炼/酿造/附魔+2
   - 嫉妒：`ServerTickEvents` 每分钟比较装备等级（下界合金6>钻石5>金4>铁3>石头2>木1>无0，附魔+10）；`notifySinAttackWitnessed()` 目击罪孽攻击+1

2. **Mixin 注入（8个文件）：**
   - `MixinLightningStrike` — 目标 `Entity.class`，注入 `onStruckByLightning`
   - `MixinPlayerEntity` — 目标 `PlayerEntity.class`，注入 `canConsume` 始终返回 true
   - `MixinServerPlayerEntity` — 目标 `ServerPlayerEntity.class`，注入 `trySleep`（绕过时间检查）+ `wakeUp`（睡眠完成检测）
   - `MixinAnimalEntity` — 目标 `AnimalEntity.class`，注入 `breed`
   - `MixinZombieVillagerEntity` — 目标 `ZombieVillagerEntity.class`，注入 `finishConversion`
   - `MixinZombieEntity` — 目标 `ZombieEntity.class`，注入 `convertTo`
   - `MixinPlayerAdvancementTracker` — 目标 `PlayerAdvancementTracker.class`，注入 `grantCriterion`
   - `MixinLivingEntity` — 添加 `spawnSinParticles()` 粒子效果 + `notifySinAttackWitnessed()` 调用

3. **粒子效果：** 7种罪孽各使用不同颜色的 `DustParticleEffect`（红/橙/黄/草绿/天蓝/深蓝/紫），在目标实体周围生成12个粒子

**编译错误修复：**
- `ServerLivingEntityEvents.AFTER_DAMAGE` 不存在 → 改用 `ALLOW_DAMAGE`
- `player.jumping` 是 protected → 改用 `player.isOnGround()`
- `MixinLightningStrike` 目标 `LivingEntity.class` → 改为 `Entity.class`（`onStruckByLightning` 定义在 Entity 中）
- `Stats.CRAFTED` 不可迭代 → 改用 `Registries.ITEM` 遍历
- `SwordItem` 引用冗余 → 移除（`SwordItem extends ToolItem`，已被 ToolItem 捕获）
- `AFTER_RESPAWN` 用于睡眠检测不正确 → 改用 `MixinServerPlayerEntity.wakeUp()` 注入
- 清理未使用的 import（ServerPlayerEvents, LightningEntity, StatusEffects, ZombieEntity, ZombieVillagerEntity, Stat, StatType, Identifier）

**构建结果：** BUILD SUCCESSFUL（2次验证）

**待完成：** 30j — sin_types.json 配置更新（加入新碎片获取参数）

### 30j: sin_types.json 配置更新（完成）
- 新增 `fragmentAcquisition` 章节：7种罪孽独立碎片获取规则（含来源、数量、条件）
- 新增 `particleEffects` 章节：粒子类型、颜色 RGB 值、数量、扩散半径
- 新增 `qolFeatures` 章节：反复进食/反复睡觉开关及描述
- 构建: **BUILD SUCCESSFUL**

**阶段30 全部完成！**