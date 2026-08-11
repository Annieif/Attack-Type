# Forge / NeoForge Mod 开发指南

## Forge vs NeoForge

| 属性 | Forge | NeoForge |
|------|-------|----------|
| 维护方 | MinecraftForge 团队 | NeoForged 团队 |
| 最新支持 | 1.21+ | 1.20.1+ |
| 包名 | `net.minecraftforge` | `net.neoforged` |
| 事件总线 | `IEventBus` | `IEventBus`（类似） |
| 配置文件 | `META-INF/mods.toml` | `META-INF/neoforge.mods.toml` |
| 注册系统 | `DeferredRegister` | `DeferredRegister` |
| Gradle 插件 | `net.minecraftforge.gradle` | `net.neoforged.gradle` |

**注意：** 1.21+ 起 NeoForge 是推荐方向，API 与 Forge 高度相似但包名不同。以下代码以 NeoForge 1.21+ 为主，关键差异处标注。

## 项目搭建

### build.gradle（NeoForge 1.21+）

```groovy
plugins {
    id 'java-library'
    id 'net.neoforged.gradle.userdev' version '7.0.+'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
}
```

### gradle.properties

```properties
minecraft_version=1.21.4
neo_version=21.4.0
mod_version=1.0.0
mod_id=mymod
```

### build.gradle（Forge 1.20.1）

```groovy
plugins {
    id 'net.minecraftforge.gradle' version '6.0.+'
}

minecraft {
    mappings channel: 'official', version: '1.20.1'
}

dependencies {
    minecraft "net.minecraftforge:forge:1.20.1-47.3.0"
}
```

## 主类

### NeoForge 1.21+
```java
@Mod(MyMod.MOD_ID)
public class MyMod {
    public static final String MOD_ID = "mymod";

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(Registries.ITEM, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(Registries.BLOCK, MOD_ID);

    public static final RegistryObject<Item> MY_ITEM =
        ITEMS.register("my_item", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Block> MY_BLOCK =
        BLOCKS.register("my_block", () -> new Block(BlockBehaviour.Properties.of()));

    public MyMod(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
    }
}
```

### Forge 1.20.1
```java
@Mod(MyMod.MOD_ID)
public class MyMod {
    public static final String MOD_ID = "mymod";

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final RegistryObject<Item> MY_ITEM =
        ITEMS.register("my_item", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Block> MY_BLOCK =
        BLOCKS.register("my_block", () -> new Block(BlockBehaviour.Properties.of()));

    public MyMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        BLOCKS.register(bus);
    }
}
```

### Forge 1.16.5
```java
@Mod(MyMod.MOD_ID)
public class MyMod {
    public static final String MOD_ID = "mymod";

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final RegistryObject<Item> MY_ITEM =
        ITEMS.register("my_item", () -> new Item(new Item.Properties().tab(ModItemGroup.MY_GROUP)));
    public static final RegistryObject<Block> MY_BLOCK =
        BLOCKS.register("my_block", () -> new Block(AbstractBlock.Properties.of(Material.STONE)));

    public MyMod() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
```

## 配置文件

### NeoForge（META-INF/neoforge.mods.toml）
```toml
modLoader = "javafml"
loaderVersion = "[4,)"

[[mods]]
modId = "mymod"
version = "1.0.0"
displayName = "My Mod"
description = "A mod description"
authors = "Your Name"

[[dependencies.mymod]]
modId = "neoforge"
type = "required"
versionRange = "[21.4,)"
ordering = "NONE"
side = "BOTH"
```

### Forge（META-INF/mods.toml）
```toml
modLoader = "javafml"
loaderVersion = "[47,)"

[[mods]]
modId = "mymod"
version = "1.0.0"
displayName = "My Mod"
description = "A mod description"

[[dependencies.mymod]]
modId = "forge"
type = "required"
versionRange = "[47.3,)"
ordering = "NONE"
side = "BOTH"
```

## 物品

### NeoForge 1.21+
```java
public static final RegistryObject<Item> MY_ITEM = ITEMS.register("my_item",
    () -> new Item(new Item.Properties()));

public static final RegistryObject<BlockItem> MY_BLOCK_ITEM = ITEMS.register("my_block",
    () -> new BlockItem(MY_BLOCK.get(), new Item.Properties()));
```

### Forge 1.20.1
```java
public static final RegistryObject<Item> MY_ITEM = ITEMS.register("my_item",
    () -> new Item(new Item.Properties()));
```

## 方块

### NeoForge 1.21+
```java
public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register("my_block",
    () -> new Block(BlockBehaviour.Properties.of()
        .strength(4.0f)
        .requiresCorrectToolForDrops()
        .sound(SoundType.STONE)));
```

### Forge 1.20.1
```java
public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register("my_block",
    () -> new Block(BlockBehaviour.Properties.of(Material.STONE)
        .strength(4.0f)
        .requiresCorrectToolForDrops()
        .sound(SoundType.STONE)));
```

### Forge 1.16.5
```java
public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register("my_block",
    () -> new Block(AbstractBlock.Properties.of(Material.STONE)
        .strength(4.0f)
        .harvestTool(ToolType.PICKAXE)
        .harvestLevel(1)
        .sound(SoundType.STONE)));
```

## 方块实体

```java
public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
    DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

public static final RegistryObject<BlockEntityType<MyBlockEntity>> MY_BLOCK_ENTITY =
    BLOCK_ENTITIES.register("my_block_entity",
        () -> BlockEntityType.Builder.of(
            MyBlockEntity::new, MY_BLOCK.get()).build(null));

// BlockEntity 类
public class MyBlockEntity extends BlockEntity {
    public MyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_BLOCK_ENTITY.get(), pos, state);
    }
}
```

## 事件系统

```java
// 在构造函数中注册事件
public MyMod(IEventBus bus) {
    // 注册
    bus.addListener(this::commonSetup);
    // 服务端事件
    MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
}

private void commonSetup(FMLCommonSetupEvent event) {
    // 通用初始化
}

private void onPlayerTick(TickEvent.PlayerTickEvent event) {
    // 玩家 tick 事件
}
```

## 实体

```java
public static final DeferredRegister<EntityType<?>> ENTITIES =
    DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

public static final RegistryObject<EntityType<MyEntity>> MY_ENTITY =
    ENTITIES.register("my_entity",
        () -> EntityType.Builder.of(MyEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.8f)
            .build("my_entity"));

// 渲染器注册（客户端）
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MY_ENTITY.get(), MyEntityRenderer::new);
    }
}
```

## 网络通信

```java
// 定义 Payload
public record MyPayload(int value) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MyPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MyMod.MOD_ID, "my_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MyPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.VAR_INT, MyPayload::value, MyPayload::new);

    @Override public Type<MyPayload> type() { return TYPE; }
}

// 注册
// 在 commonSetup 中
NeoForge.EVENT_BUS.addListener(RegisterPayloadHandlersEvent.class, event -> {
    PayloadRegistrar registrar = event.registrar(MOD_ID);
    registrar.playToServer(MyPayload.TYPE, MyPayload.CODEC, (payload, ctx) -> {
        // 处理客户端发来的包
    });
    registrar.playToClient(MyPayload.TYPE, MyPayload.CODEC, (payload, ctx) -> {
        // 处理服务端发来的包（客户端侧）
    });
});

// 发送
// To client
PacketDistributor.sendToPlayer(player, new MyPayload(42));
// To all
PacketDistributor.sendToAllPlayers(new MyPayload(42));
```

## GUI / Screen

```java
// Menu 类
public class MyMenu extends AbstractContainerMenu {
    public MyMenu(int containerId, Inventory inv) {
        super(ModMenus.MY_MENU.get(), containerId);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}

// 注册 MenuType
public static final DeferredRegister<MenuType<?>> MENUS =
    DeferredRegister.create(Registries.MENU, MOD_ID);

public static final RegistryObject<MenuType<MyMenu>> MY_MENU =
    MENUS.register("my_menu", () -> new MenuType<>(MyMenu::new, FeatureFlags.DEFAULT_FLAGS));

// 客户端 Screen
public class MyScreen extends AbstractContainerScreen<MyMenu> {
    public MyScreen(MyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}

// 客户端注册
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public static class ClientModEvents {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MY_MENU.get(), MyScreen::new);
    }
}
```

## 构建与测试

```bash
./gradlew build          # 构建
./gradlew runClient      # 测试客户端
./gradlew runServer      # 测试服务端
```

## 参考资源

- NeoForge Docs：https://docs.neoforged.net
- Forge Docs：https://docs.minecraftforge.net
- Forge 社区 Wiki：https://forge.gemwire.uk