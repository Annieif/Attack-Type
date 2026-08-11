# Mixin 完整教程

## 目录

1. [Mixin 是什么](#1-mixin-是什么)
2. [环境配置](#2-环境配置)
3. [Access Widener](#3-access-widener)
4. [源码阅读与反编译](#4-源码阅读与反编译)
5. [注入点大全](#5-注入点大全)
6. [@At 参数详解](#6-at-参数详解)
7. [@Inject 深入](#7-inject-深入)
8. [@ModifyVariable / @ModifyConstant](#8-modifyvariable--modifyconstant)
9. [@Redirect](#9-redirect)
10. [@Accessor / @Invoker](#10-accessor--invoker)
11. [@Shadow / @Unique / @Mutable](#11-shadow--unique--mutable)
12. [@Overwrite（不推荐）](#12-overwrite不推荐)
13. [Mixin 配置详解](#13-mixin-配置详解)
14. [Mixins 最佳实践](#14-mixins-最佳实践)
15. [多版本兼容策略](#15-多版本兼容策略)
16. [Mixin 调试技巧](#16-mixin-调试技巧)
17. [Mixin Extra 扩展](#17-mixin-extra-扩展)
18. [常见 Mixin 场景速查](#18-常见-mixin-场景速查)

---

## 1. Mixin 是什么

Mixin 是 SpongePowered 开发的字节码注入框架，允许在运行时修改 Minecraft 源代码。Minecraft Mod 开发中，Mixin 是修改原版行为的核心手段。

**核心概念：**
- 你的 Mixin 类会被编译成字节码
- 运行时，Mixin 框架将你的字节码「织入」目标类
- 你可以在目标方法执行前/后插入代码，或修改返回值、变量等

---

## 2. 环境配置

### build.gradle 依赖

```groovy
// Fabric Loom 内置 Mixin 支持，无需额外依赖
// 确保 gradle.properties 中有:
// loom.mixin.defaultRefmapName = mymod.refmap.json
```

### gradle.properties

```properties
loom.mixin.defaultRefmapName = mymod.refmap.json
```

### fabric.mod.json

```json
{
  "mixins": ["mymod.mixins.json"]
}
```

### mixins.json 配置

```json
{
  "required": true,
  "package": "com.example.mymod.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "MixinLivingEntity",
    "MixinServerPlayerEntity",
    "MixinAbstractBlock"
  ],
  "client": [
    "MixinTitleScreen",
    "MixinInGameHud"
  ],
  "server": [
    "MixinDedicatedServer"
  ],
  "injectors": {
    "defaultRequire": 1
  },
  "refmap": "mymod.refmap.json"
}
```

**字段说明：**
- `required`：是否必须加载，失败则崩溃
- `package`：Mixin 类所在的包
- `compatibilityLevel`：目标 Java 版本（JAVA_8、JAVA_17、JAVA_21）
- `mixins`：服务端和客户端都加载的 Mixin
- `client`：仅客户端加载的 Mixin
- `server`：仅服务端加载的 Mixin
- `refmap`：引用映射表，由 Loom 自动生成

### 包结构

```
src/main/java/com/example/mymod/mixin/
├── MixinLivingEntity.java
├── MixinServerPlayerEntity.java
├── MixinAbstractBlock.java
├── MixinTitleScreen.java          # 客户端 Mixin
└── accessor/
    ├── PlayerEntityAccessor.java
    └── LivingEntityAccessor.java
```

---

## 3. Access Widener

Access Widener 用于扩大类/方法/字段的访问权限（如 `private` → `public`），让你可以直接访问而不需要 Mixin。

### 创建 accesswidener 文件

`src/main/resources/mymod.accesswidener`：
```
accessWidener v2 named

# 扩大类的访问权限
accessible class net/minecraft/entity/player/PlayerEntity$HandcuffedC2SPacket

# 扩大方法的访问权限
accessible method net/minecraft/entity/LivingEntity getJumpVelocity ()F

# 扩大字段的访问权限
accessible field net/minecraft/entity/player/PlayerEntity hungerManager Lnet/minecraft/entity/player/HungerManager;

# 可变的字段（允许修改 final 字段）
mutable field net/minecraft/world/World random Ljava/util/Random;
```

### build.gradle 配置

```groovy
loom {
    accessWidenerPath = file("src/main/resources/mymod.accesswidener")
}
```

### fabric.mod.json

```json
{
  "accessWidener": "mymod.accesswidener"
}
```

**注意：** 命名使用 `named`（Yarn/Mojang 映射名）或 `intermediary`（混淆名）。推荐使用 `named`。

---

## 4. 源码阅读与反编译

### 通过 IDEA 反编译

1. 运行一次 `./gradlew genSources` 下载带映射的源码
2. 在 IDEA 中打开外部库 `minecraft`，可以看到所有 Minecraft 源码
3. 右键任意类 → `Find Usages` 查看调用链
4. 使用 `Ctrl+N` 搜索类，`Ctrl+Shift+N` 搜索文件

### 常用探索路径

```
# 物品系统
net.minecraft.item.Item          → 物品基类
net.minecraft.item.ItemStack     → 物品堆叠
net.minecraft.item.Items         → 原版物品注册表

# 方块系统
net.minecraft.block.AbstractBlock → 方块设置基类
net.minecraft.block.Block         → 方块基类
net.minecraft.block.Blocks        → 原版方块注册表

# 实体系统
net.minecraft.entity.Entity       → 实体基类
net.minecraft.entity.LivingEntity → 生物实体
net.minecraft.entity.player.PlayerEntity → 玩家

# 渲染系统
net.minecraft.client.render.entity.EntityRenderer → 实体渲染器
net.minecraft.client.gui.screen.Screen → GUI 屏幕

# 网络系统
net.minecraft.network.packet.Packet → 网络包基类
net.minecraft.server.network.ServerPlayNetworkHandler → 服务端网络处理
```

### 推荐：查看原版实现

当不确定如何实现某个功能时，找到原版类似功能的代码，直接参考其实现方式。例如：
- 想做新武器 → 看 `SwordItem`、`BowItem`、`TridentItem`
- 想做新方块实体 → 看 `FurnaceBlockEntity`、`ChestBlockEntity`
- 想做新实体 → 看 `ZombieEntity`、`SkeletonEntity`
- 想做新 GUI → 看 `AnvilScreen`、`CraftingScreen`

---

## 5. 注入点大全

| 注解 | 作用 | 使用场景 |
|------|------|---------|
| `@Inject` | 在方法执行前/后/返回时注入代码 | 最常用，添加行为 |
| `@ModifyVariable` | 修改方法中的局部变量 | 修改参数值 |
| `@ModifyConstant` | 修改方法中的字面常量 | 修改硬编码的数值 |
| `@ModifyArg` | 修改方法调用中的参数 | 修改传递给另一个方法的参数 |
| `@ModifyArgs` | 批量修改方法调用参数 | 一次性修改多个参数 |
| `@Redirect` | 重定向方法调用 | 替换方法调用 |
| `@Accessor` | 访问私有字段 | 读取/写入私有字段 |
| `@Invoker` | 调用私有方法 | 调用不可见的方法 |
| `@Shadow` | 遮蔽目标类中的字段/方法 | 在 Mixin 中直接访问 |
| `@Unique` | 在目标类中注入自定义字段 | 存储 Mixin 专属数据 |
| `@Mutable` | 允许修改 final 字段 | 配合 @Shadow 使用 |
| `@Overwrite` | 完全覆盖方法 | 不推荐，维护困难 |

---

## 6. @At 参数详解

`@At` 定义注入位置，是 Mixin 最核心的参数。

```java
@At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V")
```

| value | 说明 | 需要 target |
|-------|------|:---:|
| `HEAD` | 方法开头 | |
| `RETURN` | 方法返回前 | |
| `INVOKE` | 调用某个方法时 | ✅ |
| `INVOKE_ASSIGN` | 调用方法并赋值返回值时 | ✅ |
| `FIELD` | 访问某个字段时 | ✅ |
| `TAIL` | 方法末尾（return 之前） | |
| `NEW` | 创建新对象时 | ✅ |
| `CONSTANT` | 加载常量时 | ✅ |

### @At 的高级参数

```java
// 按顺序：第几个匹配的注入点
@At(value = "INVOKE", target = "L...;", ordinal = 0)

// 按操作码索引（精确控制）
@At(value = "INVOKE", target = "L...;", opcode = Opcodes.INVOKEVIRTUAL)

// 按参数类型（区分重载方法）
@At(value = "INVOKE", target = "L...;", args = "I")

// 切片：限定注入范围
@At(value = "INVOKE", target = "L...;", slice = @Slice(
    from = @At(value = "INVOKE", target = "L...;"),
    to = @At(value = "INVOKE", target = "L...;")
))

// 位移：偏移 N 条指令
@At(value = "INVOKE", target = "L...;", shift = At.Shift.AFTER, by = 2)
```

---

## 7. @Inject 深入

### 基本用法

```java
@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    // HEAD: 方法开头注入
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // 每次 tick 执行
    }

    // RETURN: 方法返回前注入
    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // 伤害处理完成后执行
    }

    // INVOKE: 在调用某个方法时注入
    @Inject(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/LivingEntity;tickStatusEffects()V"))
    private void onTickStatusEffects(CallbackInfo ci) {
        // 在状态效果 tick 时注入
    }
}
```

### CallbackInfo 类型

| 类型 | 使用场景 |
|------|---------|
| `CallbackInfo` | 无返回值方法 |
| `CallbackInfoReturnable<T>` | 有返回值方法，可修改返回值 |
| `Cancellable` | 可取消方法执行 |

### 取消方法执行

```java
@Inject(method = "addExperience", at = @At("HEAD"), cancellable = true)
private void onAddExperience(int experience, CallbackInfo ci) {
    if (experience > 1000) {
        ci.cancel(); // 取消本次经验添加
    }
}
```

### 修改返回值

```java
@Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true)
private void modifyMaxHealth(CallbackInfoReturnable<Float> cir) {
    // 获取原返回值
    float original = cir.getReturnValue();
    // 修改为原来的 2 倍
    cir.setReturnValue(original * 2.0f);
}
```

### 捕获局部变量（@Local）

```java
@Inject(method = "tick", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V"),
    locals = LocalCapture.CAPTURE_FAILHARD)
private void onSetVelocity(CallbackInfo ci, double d, double e, double f) {
    // 捕获方法中的局部变量 d, e, f
    // CAPTURE_FAILHARD: 捕获失败则崩溃
    // CAPTURE_FAILSOFT: 捕获失败则跳过
    // PRINT: 打印并继续
}
```

---

## 8. @ModifyVariable / @ModifyConstant

### @ModifyVariable — 修改局部变量

```java
@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    // 修改方法参数（ordinal=0 表示第一个匹配类型的参数）
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float modifyExhaustion(float exhaustion) {
        return exhaustion * 0.5f; // 消耗减半
    }

    // 修改局部变量（非参数）
    @ModifyVariable(method = "attack", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/player/PlayerEntity;addEnchantmentEffects(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;)V"),
        ordinal = 0)
    private float modifyDamage(float damage) {
        return damage * 1.5f; // 伤害加 50%
    }
}
```

### @ModifyConstant — 修改字面常量

```java
@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    // 修改硬编码的数值
    @ModifyConstant(method = "jump", constant = @Constant(floatValue = 0.42f))
    private float modifyJumpVelocity(float original) {
        return 0.6f; // 修改跳跃速度
    }

    // 修改硬编码的 int 值
    @ModifyConstant(method = "getXpToDrop", constant = @Constant(intValue = 5))
    private int modifyXpDrop(int original) {
        return 10; // 修改经验掉落
    }

    // 修改字符串常量
    @ModifyConstant(method = "getDeathMessage", constant = @Constant(stringValue = "was slain"))
    private String modifyDeathMessage(String original) {
        return "was obliterated";
    }
}
```

---

## 9. @Redirect

重定向方法调用，将目标方法调用替换为你的实现。

```java
@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    // 替换 isCreative() 的调用结果
    @Redirect(method = "canConsume", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/player/PlayerEntity;isCreative()Z"))
    private boolean redirectIsCreative(PlayerEntity player) {
        return false; // 让生存模式玩家也能无视饱食度吃食物
    }

    // 替换方块破坏速度计算
    @Redirect(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;getMiningSpeedMultiplier(Lnet/minecraft/block/BlockState;)F"))
    private float modifyMiningSpeed(ItemStack stack, BlockState state) {
        float original = stack.getMiningSpeedMultiplier(state);
        return original * 2.0f; // 双倍挖掘速度
    }
}
```

**注意：** `@Redirect` 会影响所有调用点，使用 `ordinal` 精确控制。

---

## 10. @Accessor / @Invoker

### @Accessor — 访问私有字段

```java
@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
    @Accessor("itemCooldownManager")
    ItemCooldownManager getItemCooldownManager();

    @Accessor("itemCooldownManager")
    void setItemCooldownManager(ItemCooldownManager manager);
}

// 使用
PlayerEntity player = ...;
ItemCooldownManager cooldown = ((PlayerEntityAccessor) player).getItemCooldownManager();
```

### @Invoker — 调用私有方法

```java
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Invoker("getJumpVelocity")
    float invokeGetJumpVelocity();

    @Invoker("damage")
    boolean invokeDamage(DamageSource source, float amount);
}

// 使用
LivingEntity entity = ...;
float jumpVel = ((LivingEntityInvoker) entity).invokeGetJumpVelocity();
```

---

## 11. @Shadow / @Unique / @Mutable

### @Shadow — 遮蔽目标类字段/方法

```java
@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    @Shadow
    private int experienceLevel; // 读取目标类的 private 字段

    @Shadow
    protected void updatePose() {} // 遮蔽目标类的方法（方法体为空）

    @Inject(method = "addExperience", at = @At("HEAD"))
    private void onAddExperience(int experience, CallbackInfo ci) {
        // 直接使用 @Shadow 遮蔽的字段
        if (this.experienceLevel >= 100) {
            ci.cancel();
        }
    }
}
```

### @Unique — 在目标类中注入新字段

```java
@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    @Unique
    private int myModCustomCounter = 0; // 注入到 PlayerEntity 中的新字段

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        myModCustomCounter++;
        if (myModCustomCounter > 100) {
            // 每 100 tick 做点什么
            myModCustomCounter = 0;
        }
    }
}
```

### @Mutable — 修改 final 字段

```java
@Mixin(Item.class)
public class MixinItem {
    @Shadow @Mutable @Final
    private int maxCount;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void modifyMaxCount(Item.Settings settings, CallbackInfo ci) {
        this.maxCount = 128; // 修改 final 字段
    }
}
```

---

## 12. @Overwrite（不推荐）

完全覆盖原方法。**不推荐使用**，因为多个 Mod 同时 Overwrite 同一方法会冲突。

```java
@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Overwrite
    public void jump() {
        // 完全替换原版 jump() 的实现
        // 不推荐：应使用 @Inject + @ModifyVariable
    }
}
```

**替代方案：**
- 用 `@Inject` + `@ModifyVariable` 代替
- 用 `@Redirect` 代替
- 用 `@Inject` + `cancellable = true` 代替

---

## 13. Mixin 配置详解

### refmap 是什么

refmap（引用映射表）记录 Mixin 中的字符串引用与实际混淆名的对应关系。由 Loom 在构建时自动生成。

```properties
# gradle.properties
loom.mixin.defaultRefmapName = mymod.refmap.json
```

### 优先级（priority）

```java
@Mixin(value = LivingEntity.class, priority = 1000)
```

- 默认优先级 1000
- 数值越大执行越晚
- 用于控制多个 Mixin 的执行顺序

### require（依赖）

```java
@Mixin(value = LivingEntity.class)
@Pseudo // 目标类不存在时不报错（可选依赖）
```

### 条件 Mixin（@Mixin 配置）

```java
// 只对特定类生效
@Mixin(targets = "net.minecraft.entity.passive.SheepEntity")

// 使用通配符
@Mixin(targets = "net.minecraft.block.AbstractBlock$*")
```

---

## 14. Mixins 最佳实践

### 1. 命名规范

```
Mixin + 目标类名 = MixinLivingEntity
Mixin + 目标类名 + 功能 = MixinPlayerEntityHunger
```

### 2. 方法命名

```java
// 前缀 + 目标方法名
@Inject(method = "tick", at = @At("HEAD"))
private void onTick(CallbackInfo ci) {} // 推荐

// 或者
@Inject(method = "tick", at = @At("HEAD"))
private void tick_HEAD(CallbackInfo ci) {} // 也可以
```

### 3. 保持方法 private

Mixin 方法应为 `private`，避免被意外调用。

### 4. 一个类一个 Mixin

每个 Mixin 类只针对一个目标类，职责单一。

### 5. 避免 @Overwrite

永远使用 `@Inject` + `@Redirect` + `@ModifyVariable` 代替。

### 6. 使用 .mixin 包

将所有 Mixin 类放在 `mixin` 包下，结构清晰。

### 7. 添加注释

```java
/**
 * 修改玩家跳跃高度，使其与速度属性挂钩
 * 目标方法: LivingEntity.jump()
 * 注入点: 修改 jumpVelocity 局部变量
 */
@ModifyVariable(method = "jump", at = @At("HEAD"), ordinal = 0, argsOnly = true)
private float modifyJumpVelocity(float jumpVelocity) {
    return (float) (jumpVelocity * this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
}
```

---

## 15. 多版本兼容策略

### 策略 1：版本分离 Mixin 类

```
mixin/
├── common/
│   └── MixinLivingEntity.java      # 所有版本通用
├── v1_20/
│   └── MixinPlayerEntity.java      # 1.20.x 专用
└── v1_21/
    └── MixinPlayerEntity.java      # 1.21.x 专用
```

### 策略 2：使用 @Mixin 条件

```java
// 只对特定版本生效（通过类名判断）
@Mixin(targets = {
    "net.minecraft.class_1309",  // intermediary 名
    "net.minecraft.entity.LivingEntity"  // named 名
})
```

### 策略 3：在 mixins.json 中按版本分离

```json
{
  "mixins": [
    "MixinLivingEntity",
    "MixinAbstractBlock"
  ],
  "client": [
    "MixinTitleScreen"
  ]
}
```

不同版本使用不同的 mixins.json 文件。

---

## 16. Mixin 调试技巧

### 1. 查看织入日志

在 `build.gradle` 中启用：
```groovy
loom {
    mixin {
        useLegacyMixinAp = false
    }
}
```

运行 `./gradlew build` 时会输出 Mixin 织入信息。

### 2. 添加调试日志

```java
@Inject(method = "tick", at = @At("HEAD"))
private void onTick(CallbackInfo ci) {
    MyMod.LOGGER.info("MixinLivingEntity.onTick injected successfully");
}
```

### 3. 检查目标方法签名

```bash
# 如果注入失败，检查目标方法是否被正确识别
./gradlew build --info | grep -i "mixin"
```

### 4. 常见错误与解决

| 错误 | 原因 | 解决 |
|------|------|------|
| `MixinTargetAlreadyLoadedException` | 目标类已加载后再织入 | 减少 Mixin 复杂度 |
| `InjectionError` | 注入点找不到 | 检查 `@At` 参数和目标方法签名 |
| `Cannot locate method` | 方法名在映射中不存在 | 检查映射名是否正确 |
| `Invalid member descriptor` | 方法签名不匹配 | 核对参数类型和返回值 |
| `Too many injections` | 注入点匹配过多 | 添加 `ordinal` 或 `slice` 限定 |

---

## 17. Mixin Extra 扩展

Mixin Extra 是 Fabric 社区提供的 Mixin 扩展库，提供更多注入点。

### build.gradle

```groovy
// 1.21+
modImplementation include("com.github.llamalad7.mixinextras:mixinextras-fabric:0.4.1")
```

### 常用注解

```java
// @WrapOperation — 比 @Redirect 更安全的重定向
@WrapOperation(method = "tick", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V"))
private void onSetVelocity(LivingEntity instance, double x, double y, double z,
        Operation<Void> original) {
    original.call(instance, x * 2, y, z * 2); // 修改后再调用原方法
}

// @ModifyExpressionValue — 修改表达式返回值
@ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/item/ItemStack;getMiningSpeedMultiplier(Lnet/minecraft/block/BlockState;)F"))
private float modifyMiningSpeed(float original) {
    return original * 2;
}

// @WrapWithCondition — 条件化方法调用
@WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/entity/Entity;setOnFireFromLava()V"))
private boolean shouldSetOnFireFromLava() {
    return false; // 永远不被岩浆点燃
}
```

---

## 18. 常见 Mixin 场景速查

### 场景：修改物品使用效果

```java
@Mixin(Item.class)
public class MixinItem {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(World world, PlayerEntity user, Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (user.isSneaking()) {
            cir.setReturnValue(TypedActionResult.fail(user.getStackInHand(hand)));
        }
    }
}
```

### 场景：阻止方块放置

```java
@Mixin(BlockItem.class)
public class MixinBlockItem {
    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
        at = @At("HEAD"), cancellable = true)
    private void onPlace(ItemPlacementContext ctx, CallbackInfoReturnable<ActionResult> cir) {
        if (ctx.getWorld().getDimension().ultrawarm()) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
```

### 场景：修改掉落物

```java
@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @ModifyVariable(method = "getLootSource", at = @At("STORE"), ordinal = 0)
    private int modifyXpDrop(int original) {
        return original * 3; // 三倍经验掉落
    }
}
```

### 场景：物品栏交互

```java
@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {
    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void onDrawBackground(DrawContext ctx, float delta, int mouseX, int mouseY,
            CallbackInfo ci) {
        // 添加自定义绘制
        ctx.drawText(textRenderer, Text.literal("My Mod!"), 10, 10, 0xFFFFFF, true);
    }
}
```

### 场景：修改聊天消息

```java
@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    @ModifyVariable(method = "onChatMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private SignedMessage modifyChatMessage(SignedMessage message) {
        // 修改聊天消息
        return message;
    }
}
```

### 场景：阻止生物生成

```java
@Mixin(SpawnHelper.class)
public class MixinSpawnHelper {
    @Inject(method = "spawnEntitiesInChunk", at = @At("HEAD"), cancellable = true)
    private static void onSpawn(SpawnGroup group, ServerWorld world, Chunk chunk,
            SpawnHelper.Checker checker, SpawnHelper.Runner runner, CallbackInfo ci) {
        if (group == SpawnGroup.MONSTER) {
            ci.cancel(); // 阻止所有怪物生成
        }
    }
}
```

### 场景：修改附魔台

```java
@Mixin(EnchantmentScreenHandler.class)
public class MixinEnchantmentScreenHandler {
    @ModifyConstant(method = "method_17411", constant = @Constant(intValue = 30))
    private int modifyMaxEnchantLevel(int original) {
        return 50; // 附魔等级上限从 30 改为 50
    }
}
```

### 场景：自定义配方类型

```java
@Mixin(RecipeManager.class)
public class MixinRecipeManager {
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApply(Map<Identifier, JsonElement> map, ResourceManager rm,
            Profiler profiler, CallbackInfo ci) {
        // 在配方加载时注入自定义逻辑
    }
}
```

---

## 参考资源

- Mixin 官方文档：https://github.com/SpongePowered/Mixin/wiki
- Mixin Extra：https://github.com/LlamaLad7/MixinExtras/wiki
- Fabric Wiki Mixin：https://wiki.fabricmc.net/tutorial:mixin
- Mixin Cheatsheet：https://github.com/2xsaiko/mixin-cheatsheet
- Access Widener 教程：https://fabricmc.net/wiki/tutorial:accesswideners