# 客户端 Mod 开发教程

客户端 Mod 仅运行在客户端，不需要服务端安装。涵盖 HUD、显示增强、自动化辅助、优化类 Mod 等。

## 重要：客户端 Mod 与服务端 Mod 的区别

| 特性 | 客户端 Mod | 服务端 Mod |
|------|----------|----------|
| 安装位置 | 仅客户端 | 服务端 |
| 功能范围 | 渲染、本地数据、输入 | 游戏逻辑、数据持久化 |
| 网络影响 | 不发送额外数据包 | 可通过网络包与客户端通信 |
| 服务器兼容 | 纯客户端 Mod 服务器不感知 | 通常需要服务端也安装 |
| `fabric.mod.json` 环境 | `"client"` | `"main"` |

**纯客户端 Mod 声明（`fabric.mod.json`）：**
```json
{
  "entrypoints": {
    "client": ["com.example.mymod.client.MyModClient"]
  }
}
```

## 1. HUD 渲染（HudRenderCallback）

Fabric API 提供 `HudRenderCallback`，每帧调用，用于在屏幕上绘制自定义内容。

### 基础 HUD 渲染（1.21+）

```java
// 在 ClientModInitializer 中注册
HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.player == null) return;

    // 绘制半透明背景
    int x = 10, y = 10;
    int width = 120, height = 60;
    drawContext.fill(x, y, x + width, y + height, 0x80000000);

    // 绘制文字
    drawContext.drawText(client.textRenderer, "自定义 HUD", x + 5, y + 5, 0xFFFFFF, true);

    // 绘制玩家坐标
    BlockPos pos = client.player.getBlockPos();
    String coords = String.format("X: %d Y: %d Z: %d", pos.getX(), pos.getY(), pos.getZ());
    drawContext.drawText(client.textRenderer, coords, x + 5, y + 20, 0x00FF00, true);
});
```

### 绘制纹理

```java
// 在 HudRenderCallback 中
Identifier texture = Identifier.of("mymod", "textures/gui/hud_icon.png");
RenderSystem.setShaderTexture(0, texture);
drawContext.drawTexture(texture, x, y, 0, 0, 16, 16, 16, 16);
```

### 动态颜色

```java
double currentTime = Util.getMeasuringTimeMs() / 1000.0;
float lerpedAmount = Mth.abs(Mth.sin((float)currentTime / 2.0f));
int color = FastColor.ARGB32.lerp(lerpedAmount, 0xFFFF0000, 0xFF00FF00);
drawContext.fill(0, 0, 100, 100, 0, color);
```

### 条件渲染

```java
HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
    MinecraftClient client = MinecraftClient.getInstance();
    // 仅在持有特定物品时显示
    if (client.player != null && client.player.getMainHandStack().isOf(ModItems.MY_ITEM)) {
        drawContext.drawText(client.textRenderer, "已装备 My Item",
            10, 50, 0xFFFF00, true);
    }
    // 仅在 F3 调试界面关闭时显示
    if (!client.getDebugHud().shouldShowDebugHud()) {
        // 自定义 HUD 内容
    }
});
```

## 2. Mixin 注入 HUD

当需要更深度的 HUD 修改时，使用 Mixin 注入 `InGameHud`：

```java
@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, DeltaTracker tickCounter, CallbackInfo ci) {
        // 在 HUD 渲染完成后添加自定义内容
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        context.drawText(client.textRenderer, "Mixin HUD",
            10, 10, 0xFFAA00, true);
    }
}
```

## 3. 显示类 Mod

### 坐标显示

```java
// 在 HudRenderCallback 中
BlockPos pos = client.player.getBlockPos();
String facing = client.player.getHorizontalFacing().getName();
String text = String.format("§a%s §7| §eX:%d Y:%d Z:%d",
    facing, pos.getX(), pos.getY(), pos.getZ());
drawContext.drawText(client.textRenderer, text, 10, 10, 0xFFFFFF, true);
```

### FPS 显示

```java
// 在 HudRenderCallback 中
int fps = MinecraftClient.getInstance().getCurrentFps();
String fpsText = String.format("FPS: %d", fps);
int color = fps >= 60 ? 0x00FF00 : (fps >= 30 ? 0xFFFF00 : 0xFF0000);
drawContext.drawText(client.textRenderer, fpsText, 10, 10, color, true);
```

### 耐久度/装备状态显示

```java
// 在 HudRenderCallback 中
PlayerEntity player = client.player;
if (player == null) return;

int y = 10;
for (ItemStack armor : player.getArmorItems()) {
    if (!armor.isEmpty() && armor.isDamageable()) {
        int maxDmg = armor.getMaxDamage();
        int dmg = armor.getDamage();
        int remaining = maxDmg - dmg;
        double percent = (double)remaining / maxDmg * 100;
        String text = String.format("%s: %.0f%%", armor.getName().getString(), percent);
        int color = percent > 50 ? 0x00FF00 : (percent > 25 ? 0xFFFF00 : 0xFF0000);
        drawContext.drawText(client.textRenderer, text, 10, y, color, true);
        y += 12;
    }
}
```

### 小地图基础

```java
// 小地图核心：在 HudRenderCallback 中绘制俯视图
// 完整实现需要：
// 1. 缓存区块数据（从 ChunkData 事件获取）
// 2. 将方块颜色映射到纹理
// 3. 根据玩家朝向旋转地图
// 4. 处理不同维度的颜色映射

// 简化示例：绘制固定色块地图
int mapX = 10, mapY = 10;
int mapSize = 100;
int scale = 4;
BlockPos playerPos = client.player.getBlockPos();

for (int dx = -mapSize/2; dx < mapSize/2; dx += scale) {
    for (int dz = -mapSize/2; dz < mapSize/2; dz += scale) {
        BlockPos checkPos = playerPos.add(dx, 0, dz);
        BlockState state = client.world.getBlockState(checkPos);
        int color = getMapColor(state); // 自定义颜色映射
        int screenX = mapX + (dx + mapSize/2) / scale;
        int screenY = mapY + (dz + mapSize/2) / scale;
        drawContext.fill(screenX, screenY, screenX + 1, screenY + 1, 0, color);
    }
}
```

### 方向指示器

```java
// 在 HudRenderCallback 中
Direction facing = client.player.getHorizontalFacing();
String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
float yaw = client.player.getYaw();
int dirIndex = Math.round(yaw / 45f) % 8;
if (dirIndex < 0) dirIndex += 8;

String compass = String.format("§e[ %s ]", directions[dirIndex]);
drawContext.drawText(client.textRenderer, compass,
    client.getWindow().getScaledWidth() / 2 - 10, 10, 0xFFFFFF, true);
```

## 4. 自动化辅助类 Mod

### 自动钓鱼

```java
// 在 ClientTickEvents.END_CLIENT_TICK 中
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (!autoFishEnabled) return;
    if (client.player == null || client.interactionManager == null) return;

    // 检测鱼漂（FishingBobberEntity）
    Entity hooked = client.player.fishHook;
    if (hooked instanceof FishingBobberEntity bobber) {
        // 鱼上钩时（bobber 下沉），收杆
        if (bobber.getVelocity().y < -0.1) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            // 等待一小段时间后重新抛竿
        }
    }
});
```

### 自动整理背包

```java
// 排序算法：按物品类型、数量、耐久度排序
public static void sortInventory(PlayerInventory inventory) {
    List<ItemStack> items = new ArrayList<>();
    for (int i = 9; i < 36; i++) { // 热键栏之外
        ItemStack stack = inventory.getStack(i);
        if (!stack.isEmpty()) {
            items.add(stack.copy());
            inventory.setStack(i, ItemStack.EMPTY);
        }
    }
    items.sort((a, b) -> {
        // 按物品注册名排序，同名按数量降序
        int cmp = Registries.ITEM.getId(a.getItem())
            .compareTo(Registries.ITEM.getId(b.getItem()));
        if (cmp != 0) return cmp;
        return Integer.compare(b.getCount(), a.getCount());
    });
    for (int i = 9; i < 36 && !items.isEmpty(); i++) {
        inventory.setStack(i, items.remove(0));
    }
}
```

### 一键合成（CraftingHelper）

```java
// 点击合成台时，自动计算最优合成路径
// 通过 Mixin 注入 HandledScreen 的 mouseClicked 方法
@Mixin(HandledScreen.class)
public class MixinHandledScreen {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // 检测 shift+点击合成结果槽，自动计算材料分配
    }
}
```

### 自动奔跑/自动行走

```java
// 在 ClientTickEvents 中
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (autoWalkEnabled && client.player != null) {
        KeyBinding forwardKey = client.options.forwardKey;
        forwardKey.setPressed(true);
    }
});
```

## 5. 外挂客户端（⚠️ 封禁风险警告）

> ⚠️ **严重警告**：以下内容仅供学习研究，了解反作弊原理。在多人服务器使用外挂客户端违反大多数服务器规则，会导致封禁（Ban/IP-Ban）。此类 Mod 不应用于破坏他人游戏体验。

### 常见外挂功能及其原理

| 功能 | 原理 | 检测方法 |
|------|------|---------|
| X-ray（透视） | 修改方块渲染，跳过非目标方块的面 | 矿脉模式异常（挖掘路径直线指向矿石） |
| KillAura（杀戮光环） | 自动攻击范围内实体 | 攻击角度异常、同时攻击多个实体、点击频率异常 |
| Fly（飞行） | 修改 `PlayerEntity.abilities.allowFlying` 或取消重力 | 移动轨迹异常、速度超限 |
| Speed（加速） | 修改移动速度或注入额外移动包 | 速度检测、移动模式分析 |
| NoFall（无摔落伤害） | 取消 `onLanding` 或修改 `fallDistance` | 从高处跳下无伤害 |
| Scaffold（自动搭路） | 快速在脚下放置方块 | 放置速度异常、角度异常 |
| ESP（透视实体） | 通过 Mixin 渲染实体轮廓或发光效果 | 纯客户端，难以检测 |
| AutoClicker（自动点击） | 模拟点击事件 | 点击间隔分布异常（过于均匀） |
| Reach（攻击距离） | 修改攻击距离检测 | 攻击距离超过 3 格 |
| NoClip（穿墙） | 修改 `noClip` 属性 | 穿越方块移动 |

### 反作弊系统原理

```java
// 服务器端检测示例（简化，展示反作弊如何工作）
public class AntiCheatListener implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // 1. 速度检测
        double distance = from.distance(to);
        if (distance > 0.6 && !player.isFlying()) { // 正常走路速度上限
            event.setCancelled(true);
            player.sendMessage("§c检测到异常移动！");
        }

        // 2. 飞行检测
        if (!player.isOnGround() && !player.isFlying() && !player.isInWater()) {
            // 玩家在空中但不在飞行/游泳状态
            int airTicks = getAirTicks(player);
            if (airTicks > 20) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            // 3. 攻击距离检测
            double distance = attacker.getLocation()
                .distance(event.getEntity().getLocation());
            if (distance > 3.5) {
                event.setCancelled(true);
                attacker.sendMessage("§c检测到异常攻击距离！");
            }
        }
    }
}
```

### 客户端检测规避手段（服务器视角）

服务器可以通过以下方式检测外挂客户端：
- **Client Brand 检测**：客户端发送的品牌名（"vanilla"、"fabric"等）
- **Plugin Channel 注册**：纯原版客户端不应注册额外 Plugin Channel
- **Mod 列表扫描**：部分 Mod 暴露自己（如 MOD-HACK DETECTED 插件）
- **行为建模**：AI 分析玩家行为模式（如 GrimAC）
- **屏幕截图审查**：管理员手动审查（ScreenShare）

## 6. 著名客户端 Mod 架构分析

### Litematica（投影 Mod）

Litematica 是 Minecraft 最著名的建筑蓝图 Mod，允许玩家加载 Schematic 文件并在世界中显示半透明投影。

**核心架构：**

```
Litematica/
├── 渲染引擎
│   ├── SchematicRenderer — 主渲染器
│   ├── ChunkRendererSchematicVBO — 分块 VBO 渲染
│   └── OverlayRenderer — 覆盖层渲染
├── 放置系统
│   ├── SchematicPlacement — 投影放置（位置/旋转/镜像）
│   ├── SubRegionPlacement — 子区域管理
│   └── SchematicPlacementManager — 全局放置管理器
├── 数据层
│   ├── SchematicFormat — 文件格式解析（.litematic/.schematic）
│   └── MaterialList — 材料清单
└── 粘贴系统
    ├── TaskPasteSchematicPerChunk — 逐块粘贴
    └── SchematicPlacingUtils — 放置工具
```

**关键技术点：**
1. **Ghost Block 渲染**：使用半透明着色器渲染 Schematic 方块，不阻挡原版方块
2. **分块渲染**：与原版类似的分块系统，支持多线程渲染
3. **变换系统**：支持位置偏移、旋转（0/90/180/270）、镜像、区域选择
4. **材料列表**：自动统计所需方块及数量

**实现类似功能的起点：**
```java
// 自定义 WorldRenderer Mixin 注入额外渲染
@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(/* ... */) {
        // 在正常世界渲染后，渲染半透明投影
        renderSchematicOverlay();
    }
}
```

### Baritone（寻路/自动化 Mod）

Baritone 是 Minecraft 最强的 AI 寻路 Mod，支持自动行走、采矿、建造等。

**6 层架构：**

```
Layer 1: 世界数据层
├── BlockStateInterface — 方块状态缓存
├── WorldProvider — 世界数据提供者
├── CachedWorld — 缓存世界数据
└── FasterWorldScanner — 快速区块扫描

Layer 2: 寻路引擎
├── AStarPathFinder — A* 寻路实现
├── PathExecutor — 路径执行器
├── IMovement — 移动状态机
└── MovementHelper — 移动辅助

Layer 3: 行为层
├── PathingBehavior — 寻路行为
├── LookBehavior — 视角控制
├── InventoryBehavior — 背包管理
└── InputOverrideHandler — 输入覆盖

Layer 4: 高级流程层
├── MineProcess — 自动采矿
├── FollowProcess — 跟随实体
├── BuildProcess — 建造
└── FarmProcess — 自动农场

Layer 5: 命令层
├── CommandManager — 命令注册
└── 各类命令（#mine, #follow, #build, #stop...）

Layer 6: API 层
└── IBaritone — 外部 API 接口
```

**A* 寻路核心算法简化：**
```java
public class SimplePathFinder {
    // 1. 定义成本函数
    public double movementCost(BlockPos from, BlockPos to) {
        // 基础移动成本（走/跑/跳/游泳）
        // 方块破坏成本（如果需要破方块）
        // 方块放置成本（如果需要搭方块）
        // 危险成本（摔落伤害、岩浆、怪物）
    }

    // 2. 启发式函数（估算剩余距离）
    public double heuristic(BlockPos current, BlockPos goal) {
        return Math.sqrt(current.getSquaredDistance(goal));
    }

    // 3. A* 搜索
    public List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();

        openSet.add(new Node(start, 0, heuristic(start, goal)));

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current.pos.equals(goal)) {
                return reconstructPath(cameFrom, current.pos);
            }
            closedSet.add(current.pos);

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (closedSet.contains(neighbor)) continue;
                double tentativeG = current.g + movementCost(current.pos, neighbor);
                // 更新最优路径
            }
        }
        return null; // 无路径
    }
}
```

## 7. 优化类 Mod

优化 Mod 是 Minecraft 生态中最重要的客户端 Mod 类别之一，专注于提升帧率、减少卡顿、降低内存占用。

### 核心优化 Mod 列表

| Mod | 作用 | 原理 | 平台 |
|-----|------|------|------|
| **Sodium** | 渲染优化 | 重写渲染引擎，优化 GPU 缓冲区，视锥剔除 | Fabric/NeoForge |
| **Lithium** | 逻辑优化 | 优化实体 AI、物理、方块 tick，不改行为 | Fabric |
| **Phosphor** | 光照优化 | 优化光照引擎，减少光照更新开销 | Fabric（1.20 前） |
| **Starlight** | 光照优化 | 完全重写光照引擎 | Fabric（1.20+替代） |
| **FerriteCore** | 内存优化 | 压缩方块状态存储，减少内存占用 | Fabric/Forge |
| **ImmediatelyFast** | 即时渲染优化 | 优化 Immediate Mode 渲染 | Fabric |
| **Entity Culling** | 实体剔除 | 跳过不可见实体的渲染 | Fabric/Forge |
| **Smooth Boot** | 启动优化 | 多线程处理启动任务 | Fabric/Forge |
| **LazyDFU** | 启动优化 | 延迟 DataFixerUpper 初始化 | Fabric/Forge |
| **Krypton** | 网络优化 | 优化网络管道 | Fabric |
| **Memory Leak Fix** | 内存修复 | 修复原版内存泄漏 | Fabric/Forge |
| **Debugify** | Bug 修复 | 修复原版已知 Bug | Fabric/Forge |
| **DashLoader** | 缓存优化 | 缓存游戏资源，加速启动 | Fabric |

### 基础优化 Mod 开发示例

```java
// 1. 实体渲染剔除（Entity Culling 原理）
@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void onRenderEntity(Entity entity, /* ... */, CallbackInfo ci) {
        // 如果实体不在视野内（Frustum），跳过渲染
        if (!isInFrustum(entity)) {
            ci.cancel();
        }
    }

    private boolean isInFrustum(Entity entity) {
        // 检查实体是否在相机视锥体内
        Frustum frustum = MinecraftClient.getInstance().worldRenderer.getFrustum();
        if (frustum != null) {
            return frustum.isVisible(entity.getBoundingBox());
        }
        return true;
    }
}

// 2. 粒子效果限制
@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @ModifyVariable(method = "addParticle", at = @At("HEAD"), ordinal = 0)
    private int limitParticles(int original) {
        // 限制每帧粒子数量
        return Math.min(original, 1000);
    }
}

// 3. 延迟初始化（LazyDFU 原理）
// 将 DataFixerUpper 的初始化延迟到首次使用时
// 而非游戏启动时全部加载
```

### 客户端环境键（Fabric 中声明仅客户端 Mod）

```json
// fabric.mod.json 中
{
  "environment": "client",
  "entrypoints": {
    "client": ["com.example.optimization.OptimizationMod"]
  }
}
```

## 8. 客户端输入处理

### 按键绑定

```java
public class ModKeyBindings {
    public static final KeyBinding TOGGLE_HUD = new KeyBinding(
        "key.mymod.toggle_hud",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        "category.mymod.general"
    );

    public static void initialize() {
        KeyBindingHelper.registerKeyBinding(TOGGLE_HUD);
    }
}

// 在 ClientTickEvents 中处理按键
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    while (ModKeyBindings.TOGGLE_HUD.wasPressed()) {
        showHud = !showHud;
        client.player.sendMessage(Text.literal("HUD: " + (showHud ? "ON" : "OFF")), true);
    }
});
```

### 鼠标事件

```java
// 使用 Mixin 处理鼠标点击
@Mixin(Mouse.class)
public class MixinMouse {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            // 中键点击的自定义逻辑
        }
    }
}
```

## 9. 客户端数据存储

客户端 Mod 可以使用本地文件存储配置：

```java
// 使用 Gson 保存/加载配置
public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir().resolve("mymod.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean showHud = true;
    public int hudColor = 0xFFFFFF;
    public int hudX = 10;
    public int hudY = 10;

    public static ModConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return GSON.fromJson(Files.readString(CONFIG_PATH), ModConfig.class);
            }
        } catch (IOException e) { /* 使用默认值 */ }
        return new ModConfig();
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) { e.printStackTrace(); }
    }
}
```

## 10. 客户端 Mod 调式技巧

1. **使用 `/log` 命令**：`LOGGER.info()` 输出到日志
2. **使用聊天栏输出**：`client.player.sendMessage(Text.literal("debug info"), true)`
3. **使用 Mixin 调试**：在 Mixin 方法中添加 `LOGGER.info("Mixin invoked!")`
4. **使用 F3 调试菜单**：通过 Mixin 向 `DebugHud` 添加自定义信息
5. **热重载**：使用 RelaxedFabricLoader 或类似工具避免反复重启

## 参考资源

- Fabric Docs HUD Rendering：https://docs.fabricmc.net/develop/rendering/hud
- Fabric Wiki：https://wiki.fabricmc.net
- Litematica 源码：https://github.com/maruohon/litematica
- Baritone 源码：https://github.com/cabaletta/baritone
- Sodium 源码：https://github.com/CaffeineMC/sodium
- Lithium 源码：https://github.com/CaffeineMC/lithium