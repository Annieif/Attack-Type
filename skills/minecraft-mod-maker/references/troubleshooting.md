# 常见错误排查指南

## 目录

1. [未找到目标类/方法](#1-未找到目标类方法)
2. [纹理/模型不显示](#2-纹理模型不显示)
3. [配方不生效](#3-配方不生效)
4. [网络包收发失败](#4-网络包收发失败)
5. [GUI 打不开](#5-gui-打不开)
6. [实体不渲染](#6-实体不渲染)
7. [矿物不生成](#7-矿物不生成)
8. [Mixin 注入失败](#8-mixin-注入失败)
9. [NullPointerException 渲染](#9-nullpointerexception-渲染)
10. [服务端崩溃但客户端正常](#10-服务端崩溃但客户端正常)
11. [方块放置后消失](#11-方块放置后消失)
12. [构建失败](#12-构建失败)
13. [如何阅读崩溃日志](#13-如何阅读崩溃日志)

---

## 1. 未找到目标类/方法

**症状：** `NoSuchMethodError`、`NoClassDefFoundError`、`ClassNotFoundException`

**常见原因：**
- API 版本不匹配：1.20.x 和 1.21.x 的 API 名称不同
- 使用了不存在的映射名（Yarn vs Mojang 映射名不同）
- 缺少依赖项

**排查步骤：**
1. 确认 MC 版本，对照 [version-migration.md](version-migration.md) 检查 API
2. 检查 `build.gradle` 中的依赖版本
3. 检查 `fabric.mod.json` 或 `mods.toml` 中的依赖声明
4. 运行 `./gradlew dependencies` 查看实际解析的依赖

**典型案例：**
```
// 错误: Registry.ITEM 在 1.21+ 中已改名
Registry.register(Registry.ITEM, id, item);  // 1.20.x
// 正确:
Registry.register(Registries.ITEM, id, item); // 1.21+
```

---

## 2. 纹理/模型不显示

**症状：** 物品/方块显示为紫黑棋盘格，或显示为 3D 模型但没有纹理

**常见原因：**
- 资源文件路径不正确
- 方块缺少四个必需文件之一
- `blockstates` JSON 中的模型路径错误
- 纹理文件格式/尺寸问题

**排查步骤：**
1. 检查 `assets/<modid>/` 下是否存在以下文件：
   - `blockstates/<name>.json`
   - `models/block/<name>.json`
   - `models/item/<name>.json`
   - `textures/block/<name>.png`
2. 确认 JSON 中的 `modid` 与实际 modid 一致
3. 检查纹理文件是否为有效 PNG
4. 查看游戏日志中的 `Missing model` 或 `Missing texture` 警告

---

## 3. 配方不生效

**症状：** 合成台中无法合成，JEI 中不显示配方

**常见原因：**
- JSON 路径错误（必须在 `data/<modid>/recipe/` 下）
- JSON 格式错误（缺少 `type` 字段）
- 1.21+ 中 `result` 格式变化（`item` → `id`）

**排查步骤：**
1. 确认路径：`src/main/resources/data/<modid>/recipe/<name>.json`
2. 使用 JSON 验证工具检查格式
3. 运行 `/reload` 命令或重启游戏
4. 查看日志中的 `Failed to load recipe` 错误

**1.21+ 配方格式：**
```json
    "result": { "id": "mymod:my_item", "count": 1 }
```
不是 1.20.x 的 `"result": { "item": "mymod:my_item", "count": 1 }`

---

## 4. 网络包收发失败

**症状：** 客户端/服务端收不到数据包，`Unknown custom packet identifier` 错误

**常见原因：**
- Payload 未注册到 `PayloadTypeRegistry`
- `StreamCodec` 序列化/反序列化不匹配
- 客户端和服务端 Mod 版本不一致
- 1.20.2 之前的旧 API 与新 API 混用

**排查步骤：**
1. 确认 `PayloadTypeRegistry.playS2C().register()` 或 `playC2S().register()` 已调用
2. 确认 `StreamCodec` 的读写顺序一致
3. 检查 `Type` 的 Identifier 是否与服务端注册的一致
4. 在注册处添加日志确认是否执行

---

## 5. GUI 打不开

**症状：** 右键方块无反应，`openHandledScreen` 后无 GUI 显示

**常见原因：**
- 客户端 Screen 未注册（`HandledScreens.register()`）
- `ScreenHandlerType` 未注册
- `BlockEntity` 未正确实现 `MenuProvider`

**排查步骤：**
1. 确认 `ScreenHandlerType` 已注册到 `Registries.SCREEN_HANDLER`
2. 确认在 `ClientModInitializer` 中调用了 `HandledScreens.register()`
3. 确认 `BlockEntity` 实现了 `MenuProvider` 接口
4. 检查 `openHandledScreen` 是否在服务端调用

---

## 6. 实体不渲染

**症状：** 实体存在但不可见（碰撞箱和 AI 正常）

**常见原因：**
- 未注册 `EntityRenderer`
- 未注册 `EntityModelLayer`
- 渲染器类不在客户端源码集中

**排查步骤：**
1. 确认在 `ClientModInitializer` 中调用了 `EntityRendererRegistry.register()`
2. 确认 `EntityModelLayerRegistry.registerModelLayer()` 已调用
3. 确认渲染器类使用了 `@Environment(EnvType.CLIENT)` 注解
4. 确认纹理文件存在于 `assets/<modid>/textures/entity/`

---

## 7. 矿物不生成

**症状：** 世界中不生成自定义矿物

**常见原因：**
- 未调用 `BiomeModifications.addFeature()`
- `PlacedFeature` 的配置不正确
- 使用的 `BiomeSelector` 范围太小

**排查步骤：**
1. 确认 `BiomeModifications.addFeature()` 在 `ModInitializer` 中调用
2. 检查 `BiomeSelectors` 是否正确（如 `foundInOverworld()`）
3. 检查 `GenerationStep.Feature` 是否正确（如 `UNDERGROUND_ORES`）
4. 确认 `PlacedFeature` 的 `CountPlacementModifier` 不为 0

---

## 8. Mixin 注入失败

**症状：** 启动时崩溃，日志显示 `MixinTargetAlreadyLoadedException` 或 `InjectionError`

**常见原因：**
- 目标方法签名在不同 MC 版本中变化
- 混淆名在 Yarn 和 Mojang 映射中不同
- 注入点 `@At` 的参数不正确
- `mixin.json` 配置不正确

**排查步骤：**
1. 使用 `./gradlew build` 查看 Mixin 警告
2. 检查 `mixin.json` 中的 `package`、`compatibilityLevel`、`mixins` 列表
3. 确认 `fabric.mod.json` 中有 `"mixins": ["mymod.mixins.json"]`
4. 使用 IDE 的 "Find Usages" 确认目标方法签名
5. 对于不同 MC 版本，确认映射名是否正确

---

## 9. NullPointerException 渲染

**症状：** 加入世界时崩溃，日志指向 `render` 或 `tick` 方法

**常见原因：**
- 客户端代码在服务端执行（没有 `@Environment(EnvType.CLIENT)` 保护）
- 客户端类在服务端源码集中

**排查步骤：**
1. 确认所有客户端代码都在 `src/client/java/` 或使用 `@Environment(EnvType.CLIENT)` 注解
2. 确认 `fabric.mod.json` 中有 `client` 入口点
3. 检查静态初始化块中是否有客户端代码

---

## 10. 服务端崩溃但客户端正常

**症状：** 客户端运行正常，放到服务端后崩溃

**常见原因：**
- 客户端类在 `main` 入口点中加载
- 缺少 `@Environment(EnvType.CLIENT)` 注解
- 使用了仅客户端的方法

**排查步骤：**
1. 运行 `./gradlew runServer` 复现
2. 查看崩溃日志的堆栈跟踪
3. 将所有渲染、模型、屏幕相关代码移到 `client` 源码集

---

## 11. 方块放置后消失

**症状：** 放置方块后立即消失，或方块无法在物品栏中找到

**常见原因：**
- 缺少 `BlockItem`
- `BlockItem` 注册了但未关联到方块

**排查步骤：**
1. 确认每个方块都有对应的 `BlockItem` 注册
2. 确认 `ModItems.register()` 在 `ModBlocks.register()` 中调用

---

## 12. 构建失败

**症状：** `./gradlew build` 失败

**常见原因：**
- Java 版本不匹配（1.20.5+ 需要 Java 21）
- Gradle 版本过低
- 依赖下载失败（网络问题）
- `fabric.mod.json` 或 `mods.toml` 格式错误

**排查步骤：**
1. 运行 `java -version` 确认 Java 版本
2. 对照 [version-migration.md](version-migration.md) 中的 Java 要求表
3. 运行 `./gradlew build --info` 获取详细日志
4. 如果依赖下载失败，配置代理或更换镜像
5. 验证 JSON/TOML 配置文件格式

---

## 13. 如何阅读崩溃日志

崩溃日志包含以下关键部分：

```
---- Minecraft Crash Report ----
// 错误摘要
Time: 2024-01-01 12:00:00
Description: Unexpected error          ← 这里说明错误类型

java.lang.NullPointerException: ...    ← 堆栈跟踪，从这里开始向下读
    at com.example.mymod.MyItem.use(MyItem.java:42)  ← 你的代码位置
    at net.minecraft.item.Item.use(Item.java:123)    ← MC 内部代码
    ...

-- MOD mymod --
Details:
    Mod File: mymod-1.0.0.jar          ← 哪个 Mod 导致的
    Failure message: ...
```

**阅读技巧：**
1. 从 `Description` 了解错误类型
2. 在堆栈跟踪中找你的包名（如 `com.example.mymod`）
3. 第一个出现你的包名的行通常就是问题所在
4. 查看 `-- MOD --` 部分确认是哪个 Mod 导致
5. 如果涉及 Mixin，查看 `-- Mixin --` 部分