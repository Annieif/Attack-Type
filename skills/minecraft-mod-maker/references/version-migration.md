# Minecraft Mod 开发版本差异速查

## 版本总览

| MC 版本 | 主要变化 | 发布日期 |
|---------|---------|---------|
| 1.16.5 | 下界更新，稳定版基线 | 2021-01 |
| 1.17.1 | Java 16 要求，高度扩展 | 2021-07 |
| 1.18.2 | Java 17 要求，世界高度-64~320 | 2022-02 |
| 1.19.2 | 深暗/远古城市，聊天系统变化 | 2022-08 |
| 1.19.4 | 数据包版本升级，Registry 重构准备 | 2023-03 |
| 1.20.1 | 樱花/考古，新 Fabric API 结构 | 2023-06 |
| 1.20.2 | 网络 API 重构，配置阶段 | 2023-09 |
| 1.20.4 | 稳定版 | 2023-12 |
| 1.20.5/6 | **Java 21**，组件系统，物品堆叠重构 | 2024-04 |
| 1.21 | 试炼更新，Registry 重命名，新附魔系统 | 2024-06 |
| 1.21.4 | 冬季小更新，API 稳定 | 2024-12 |

## 关键 API 变化速查表

### Registry 注册

| 版本 | Fabric | Forge/NeoForge |
|------|--------|----------------|
| 1.16.5 | `Registry.ITEM` | `ForgeRegistries.ITEMS` |
| 1.17.1 | `Registry.ITEM` | `ForgeRegistries.ITEMS` |
| 1.18.2 | `Registry.ITEM` | `ForgeRegistries.ITEMS` |
| 1.19.2 | `Registry.ITEM` | `ForgeRegistries.ITEMS` |
| 1.19.4 | `Registry.ITEM` | `ForgeRegistries.ITEMS` |
| 1.20.1 | `Registry.ITEM` | `ForgeRegistries.ITEMS` |
| 1.20.4 | `Registry.ITEM` | `ForgeRegistries.ITEMS` |
| 1.20.5/6 | `Registry.ITEM`（仍可用） | `ForgeRegistries.ITEMS` |
| 1.21+ | `Registries.ITEM` | `Registries.ITEM`（NeoForge） |

### Identifier 创建

| 版本 | 方法 |
|------|------|
| 1.16.5 ~ 1.20.6 | `new Identifier(modid, path)` |
| 1.21+ | `Identifier.of(modid, path)` 或 `Identifier.of(modid, path)` |

### 物品堆叠 / NBT

| 版本 | 方式 |
|------|------|
| 1.16.5 ~ 1.20.4 | `ItemStack` 带 `NbtCompound`，`getOrCreateNbt()` |
| 1.20.5+ | 移除 NBT 标签，改用 `DataComponentType` 组件系统 |
| 1.21+ | 完全组件化：`stack.set(DataComponentTypes.XXX, value)` |

**迁移示例：**
```java
// 1.20.4 及之前
stack.getOrCreateNbt().putInt("energy", 100);
int energy = stack.getOrCreateNbt().getInt("energy");

// 1.20.5+
stack.set(ModComponents.ENERGY, 100);
int energy = stack.getOrDefault(ModComponents.ENERGY, 0);
```

### 盔甲注册

| 版本 | 方式 |
|------|------|
| 1.16.5 ~ 1.20.4 | 实现 `ArmorMaterial` 接口 |
| 1.21+ | `ArmorMaterials.register()` 静态方法 |

### 工具 / 材料

| 版本 | 方式 |
|------|------|
| 1.16.5 ~ 1.20.4 | 实现 `ToolMaterial` 接口 |
| 1.21+ | 构造 `new ToolMaterial(incorrectBlocksTag, durability, speed, damage, enchantability, repairItem)` |

### 附魔

| 版本 | 方式 |
|------|------|
| 1.16.5 ~ 1.20.6 | 继承 `Enchantment` 类，Rarity 枚举 |
| 1.21+ | 组件化 `EnchantmentEffect`，`EnchantmentEntityEffect` |

### 网络通信

| 版本 | 方式 |
|------|------|
| 1.16.5 ~ 1.20.1 | `PacketByteBuf` + `Identifier` 手动序列化 |
| 1.20.2+ | `CustomPacketPayload` 接口 |
| 1.20.5+ | `StreamCodec` + `RegistryByteBuf` |
| 1.21+ | 稳定：`PayloadTypeRegistry` + `StreamCodec` |

**迁移示例：**
```java
// 1.20.1 及之前
public class MyPacket {
    public static Identifier ID = new Identifier("mymod", "my_packet");
    public static void write(PacketByteBuf buf, int value) { buf.writeInt(value); }
}

// 1.21+
public record MyPayload(int value) implements CustomPacketPayload {
    public static final Type<MyPayload> TYPE = new Type<>(Identifier.of("mymod", "my_packet"));
    public static final StreamCodec<RegistryByteBuf, MyPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, MyPayload::value, MyPayload::new);
}
```

### 方块属性

| 版本 | 方式 |
|------|------|
| 1.16.5 | `AbstractBlock.Properties.of(Material.STONE).harvestTool(ToolType.PICKAXE).harvestLevel(1)` |
| 1.17.1 ~ 1.20.4 | `AbstractBlock.Properties.of(Material.STONE).requiresCorrectToolForDrops()` |
| 1.21+ | `BlockBehaviour.Properties.of().requiresCorrectToolForDrops()`（无 Material） |

### 食物

| 版本 | 方式 |
|------|------|
| 1.16.5 ~ 1.20.4 | `new FoodComponent.Builder().hunger(4).build()` |
| 1.21+ | `new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build()` |

### Forge/NeoForge 主类

| 版本 | 构造方式 |
|------|---------|
| 1.16.5 ~ 1.20.4 | `public MyMod() { bus = FMLJavaModLoadingContext.get().getModEventBus(); }` |
| 1.21+ NeoForge | `public MyMod(IEventBus bus) { }`（构造函数注入） |
| 1.21+ Forge | `FMLJavaModLoadingContext.get().getModEventBus()` 已弃用 |

### 配置文件

| 版本 | 路径 |
|------|------|
| 1.16.5 ~ 1.20.4 | `META-INF/mods.toml` |
| 1.21+ NeoForge | `META-INF/neoforge.mods.toml` |
| 1.21+ Forge | `META-INF/mods.toml` |

## 映射系统变化

| 版本 | 默认映射 | 说明 |
|------|---------|------|
| 1.16.5 ~ 1.21.4 | Yarn / Mojang | Fabric 默认 Yarn，Forge 默认 MCP |
| 1.21.5+ | Mojang 官方 | Mojang 开始发布去混淆构建，Yarn 逐步淘汰 |

迁移命令（Fabric Loom 1.13+）：
```bash
./gradlew migrateMappings --mappings "net.minecraft:mappings:1.21.5"
```

## Java 版本要求

| MC 版本 | 最低 Java |
|---------|----------|
| 1.16.5 | Java 8 |
| 1.17.x | Java 16 |
| 1.18.x ~ 1.20.4 | Java 17 |
| 1.20.5+ | Java 21 |

## 快速判断版本

如果用户没有明确版本，依据以下线索推断：
- 提到 "Mojang mappings"、"Identifier.of" → 1.21+
- 提到 "new Identifier"、"Registry.ITEM" → 1.20.x 或更早
- 提到 "DeferredRegister"、"ForgeRegistries" → Forge/NeoForge
- 提到 "fabric.mod.json"、"FabricEntityTypeBuilder" → Fabric
- 提到 "NBT"、"getOrCreateNbt" → 1.20.4 或更早
- 提到 "DataComponent"、"components" → 1.20.5+