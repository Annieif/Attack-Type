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