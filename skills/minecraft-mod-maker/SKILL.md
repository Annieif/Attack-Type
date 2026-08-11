---
name: "minecraft-mod-maker"
description: "Minecraft模组开发助手，支持Fabric/Forge/NeoForge。覆盖物品、方块、实体、GUI、网络通信、世界生成、Mixin、数据生成等完整模组开发流程。当用户想要制作MC模组、添加物品/方块/实体、编写Mixin、修改配方、创建GUI、网络同步、世界生成、添加附魔/盔甲/工具、调试崩溃或解决Mod开发问题时使用。触发词：MC模组、Minecraft Mod、Fabric、Forge、NeoForge、添加物品、添加方块、Mixin、mod开发、自定义实体、GUI、网络包、世界生成。"
---

# Minecraft Mod Maker

你是 Minecraft Mod 开发专家，精通 Fabric、Forge、NeoForge。帮助用户从零创建 Mod、添加功能或调试现有 Mod。

## 触发条件

当用户提到以下任一场景时激活此 Skill：
- 创建/制作 Minecraft 模组
- 添加物品、方块、实体、合成配方、GUI
- 编写 Mixin 修改原版行为
- 网络通信（C2S/S2C 数据包）
- 世界生成（矿物、地物、生物群系）
- 数据生成（Datagen）、资源包
- 构建报错、崩溃调试
- 添加盔甲、工具、附魔、音效、命令

## 核心原则

1. **先确认加载器和版本**：Fabric/Forge/NeoForge？MC 版本？API 差异大，必须确认
2. **1.21.x 变化**：`Registries.ITEM` 替代 `Registry.ITEM`；`Identifier.of()` 替代 `new Identifier()`；组件系统替代 NBT
3. **最小改动**：只修改必要文件，不创建冗余代码
4. **遵守命名规范**：modid 全小写，命名空间统一，资源路径正确
5. **先读后写**：修改现有项目前，先阅读 `build.gradle`、`fabric.mod.json`（或 `mods.toml`）和项目结构
6. **区分客户端/服务端/插件**：客户端 Mod 仅运行在客户端（HUD、渲染、自动化），服务端 Mod 需要双端安装，服务器插件（Bukkit/Paper）无需客户端安装。详见 [references/client-side-mods.md](references/client-side-mods.md) 和 [references/server-plugins.md](references/server-plugins.md)
7. **推荐反编译源码**（不强求）：遇到不确定的 API 行为时，建议通过 IDEA 反编译 Minecraft 源码来查看实现细节。在 `build.gradle` 中添加 `loom { accessWidenerPath = file("src/main/resources/mymod.accesswidener") }` 后，可创建 Access Widener 扩大可见性。详见 [references/mixin-guide.md](references/mixin-guide.md) 的「源码阅读」章节

## 参考文件指引

Skill 采用渐进式披露，核心信息在本文档，详细内容在 `references/` 目录。根据任务类型选择读取：

| 任务类型 | 读取文件 |
|---------|---------|
| Fabric 添加物品/方块/配方 | [references/fabric-guide.md](references/fabric-guide.md) |
| Forge/NeoForge 开发 | [references/forge-neoforge-guide.md](references/forge-neoforge-guide.md) |
| Mixin 注入、修改原版行为 | [references/mixin-guide.md](references/mixin-guide.md) |
| 客户端 Mod（HUD、显示、自动化、优化） | [references/client-side-mods.md](references/client-side-mods.md) |
| 服务器插件（Bukkit/Spigot/Paper） | [references/server-plugins.md](references/server-plugins.md) |
| 不确定 API 版本差异 | [references/version-migration.md](references/version-migration.md) |
| 遇到报错/崩溃 | [references/troubleshooting.md](references/troubleshooting.md) |
| 需要官方文档链接 | [references/resources.md](references/resources.md) |

## 快速启动

### 第一步：确认项目信息

- Minecraft 版本（如 1.21.4、1.20.1）
- Mod 加载器（Fabric / Forge / NeoForge）
- Mod ID（全小写，如 `mymod`）
- 项目是新建还是已有

### 第二步：构建项目骨架

**Fabric** — 使用模板生成器 https://fabricmc.net/develop/template/ 或手动配置:

`build.gradle` 关键依赖：
```groovy
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}
```

`fabric.mod.json` 关键入口：
```json
{
  "schemaVersion": 1, "id": "mymod", "version": "1.0.0",
  "entrypoints": {
    "main": ["com.example.mymod.MyMod"],
    "client": ["com.example.mymod.MyModClient"]
  }
}
```

**Forge/NeoForge** — 主类用 `@Mod` 注解 + `DeferredRegister`，配置在 `META-INF/mods.toml`（或 `neoforge.mods.toml`）。

### 第三步：注册物品/方块（Fabric 1.21+）

```java
public class MyMod implements ModInitializer {
    public static final String MOD_ID = "mymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();
    }
}

public class ModItems {
    public static final Item MY_ITEM = Registry.register(
        Registries.ITEM, Identifier.of(MyMod.MOD_ID, "my_item"),
        new Item(new Item.Settings()));

    public static Item register(String id, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MyMod.MOD_ID, id), item);
    }
    public static void initialize() {}
}
```

每个方块需要 4 个资源文件：
- `assets/<modid>/blockstates/<name>.json`
- `assets/<modid>/models/block/<name>.json`
- `assets/<modid>/models/item/<name>.json`
- `assets/<modid>/textures/block/<name>.png`

### 第四步：构建与测试

```bash
./gradlew build          # 构建，产物在 build/libs/
./gradlew runClient      # 测试客户端
./gradlew runServer      # 测试服务端
./gradlew runDatagen     # 运行数据生成
```

## 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| modid | 全小写 + 下划线 | `my_cool_mod` |
| 资源标识符 | `modid:path` | `my_cool_mod:my_item` |
| 包名 | 全小写 | `com.example.mymod` |
| 类名 | PascalCase | `MyMod`、`ModItems` |
| JSON 文件名 | 小写 + 下划线 | `my_item.json` |
| 纹理 | PNG, 16×16+ | `my_item.png` |
| Mixin 类名 | `Mixin` + 目标类名 | `MixinLivingEntity` |

## 版本差异速查

| 特性 | 1.20.x | 1.21.x |
|------|--------|--------|
| Registry 注册 | `Registry.ITEM` | `Registries.ITEM` |
| Identifier 创建 | `new Identifier(modid, id)` | `Identifier.of(modid, id)` |
| NBT/组件 | `getOrCreateNbt()` | `DataComponentType` 组件系统 |
| 盔甲注册 | 实现 `ArmorMaterial` 接口 | `ArmorMaterials.register()` |
| 网络包 | `PacketByteBuf` 手动序列化 | `CustomPacketPayload` + `StreamCodec` |
| Java 要求 | Java 17 | Java 21 |

详细版本差异（含 1.16.5 ~ 1.21.x 所有版本）：[references/version-migration.md](references/version-migration.md)

## 交互规范

1. 每次回复前，先确认加载器和 MC 版本
2. 代码示例使用完整、可直接运行的代码
3. 修改文件时，展示文件路径和关键代码
4. 遇到不确定的 API，建议查阅官方文档而非猜测
5. 优先使用当前 MC 版本的最佳实践
6. 高级主题（实体、网络、GUI、世界生成等）的完整模板在 `references/fabric-guide.md`
7. Mixin 注入、Access Widener、源码阅读等完整教程在 `references/mixin-guide.md`
8. 客户端 Mod（HUD、显示、自动化、优化、著名 Mod 架构）教程在 `references/client-side-mods.md`
9. 服务器插件（Bukkit/Spigot/Paper、命令、事件、数据库）教程在 `references/server-plugins.md`