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