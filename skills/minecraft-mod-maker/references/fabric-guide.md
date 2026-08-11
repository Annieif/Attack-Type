# Fabric Mod 开发完整指南

## 适用版本

覆盖 1.16.5 ~ 1.21.x 所有主流版本。API 差异详见 [version-migration.md](version-migration.md)。

## 项目搭建

### 使用模板生成器（推荐）

1. 访问 https://fabricmc.net/develop/template/
2. 填写 Mod ID、包名、MC 版本
3. 下载并解压，用 IDEA 打开

### 手动配置 build.gradle

```groovy
plugins {
    id 'fabric-loom' version '1.9-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}
```

### gradle.properties

```properties
minecraft_version=1.21.4
yarn_mappings=1.21.4+build.1
loader_version=0.16.10
fabric_version=0.109.0+1.21.4
mod_version=1.0.0
maven_group=com.example
archives_base_name=my-mod
```

### 各版本 Gradle/Loom/Java 要求

| MC 版本 | Java | Gradle | Loom | Fabric Loader |
|---------|------|--------|------|---------------|
| 1.16.5 | 8+ | 7.x | 0.10+ | 0.11+ |
| 1.17.1 | 16+ | 7.x | 0.10+ | 0.12+ |
| 1.18.2 | 17+ | 7.x | 0.12+ | 0.13+ |
| 1.19.2 | 17+ | 7.x | 1.0+ | 0.14+ |
| 1.19.4 | 17+ | 8.x | 1.1+ | 0.14+ |
| 1.20.1 | 17+ | 8.x | 1.3+ | 0.14+ |
| 1.20.4 | 17+ | 8.x | 1.5+ | 0.15+ |
| 1.20.5/6 | 21+ | 8.6+ | 1.6+ | 0.15+ |
| 1.21 | 21+ | 8.8+ | 1.7+ | 0.16+ |
| 1.21.4 | 21+ | 8.10+ | 1.9+ | 0.16+ |

## 项目结构

```
project/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── src/
│   ├── main/java/com/example/mymod/
│   │   ├── MyMod.java              # ModInitializer
│   │   ├── MyModClient.java        # ClientModInitializer
│   │   ├── item/ModItems.java
│   │   ├── block/ModBlocks.java
│   │   ├── entity/ModEntities.java
│   │   ├── screen/ModScreenHandlers.java
│   │   └── mixin/                  # Mixin 类
│   ├── main/resources/
│   │   ├── fabric.mod.json
│   │   ├── mymod.mixins.json
│   │   ├── assets/mymod/
│   │   │   ├── blockstates/
│   │   │   ├── models/block/
│   │   │   ├── models/item/
│   │   │   ├── textures/block/
│   │   │   ├── textures/item/
│   │   │   ├── sounds.json
│   │   │   └── lang/en_us.json
│   │   └── data/mymod/
│   │       ├── recipe/
│   │       ├── loot_table/
│   │       └── tags/
│   └── client/java/com/example/mymod/
│       └── MyModClient.java
```

## 主类与配置

### ModInitializer（1.21+）

```java
package com.example.mymod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyMod implements ModInitializer {
    public static final String MOD_ID = "mymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();
        ModEntities.initialize();
        // 其他初始化
    }
}
```

### fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "mymod",
  "version": "${version}",
  "name": "My Mod",
  "description": "A mod description",
  "authors": ["Your Name"],
  "contact": { "homepage": "https://example.com" },
  "license": "MIT",
  "icon": "assets/mymod/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": ["com.example.mymod.MyMod"],
    "client": ["com.example.mymod.MyModClient"],
    "fabric-datagen": ["com.example.mymod.MyModDataGenerator"]
  },
  "mixins": ["mymod.mixins.json"],
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "~1.21",
    "java": ">=21",
    "fabric-api": "*"
  }
}
```

## 物品

### 注册（1.21+）

```java
public class ModItems {
    public static final Item MY_ITEM = register("my_item",
        new Item(new Item.Settings()));

    public static Item register(String id, Item item) {
        return Registry.register(Registries.ITEM,
            Identifier.of(MyMod.MOD_ID, id), item);
    }

    public static void initialize() {}
}
```

### 注册（1.20.x）

```java
public static final Item MY_ITEM = Registry.register(
    Registry.ITEM,
    new Identifier(MyMod.MOD_ID, "my_item"),
    new Item(new FabricItemSettings()));
```

### 物品设置详解

```java
new Item.Settings()
    .maxCount(64)                        // 最大堆叠数
    .maxDamage(500)                      // 耐久度
    .food(new FoodComponent.Builder()    // 食物属性
        .hunger(4).saturationModifier(0.3f)
        .build())
    .recipeRemainder(Items.BUCKET)       // 合成后剩余物品
    .fireproof()                         // 抗火
    .rarity(Rarity.EPIC)                 // 稀有度
```

## 方块

### 注册（1.21+）

```java
public class ModBlocks {
    public static final Block MY_BLOCK = register("my_block",
        new Block(AbstractBlock.Settings.create()
            .strength(4.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)),
        true);

    public static Block register(String id, Block block, boolean withItem) {
        Registry.register(Registries.BLOCK,
            Identifier.of(MyMod.MOD_ID, id), block);
        if (withItem) {
            ModItems.register(id, new BlockItem(block, new Item.Settings()));
        }
        return block;
    }
}
```

### 四个必需资源文件

**1. blockstates/`<name>`.json** — 方块状态 → 模型映射
```json
{ "variants": { "": { "model": "mymod:block/my_block" } } }
```

**2. models/block/`<name>`.json** — 方块模型
```json
{ "parent": "block/cube_all", "textures": { "all": "mymod:block/my_block" } }
```

**3. models/item/`<name>`.json** — 物品模型
```json
{ "parent": "mymod:block/my_block" }
```

**4. textures/block/`<name>`.png** — 纹理图片（16×16 或更大）

## 合成配方

### 有序合成
`data/mymod/recipe/my_recipe.json`：
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["XXX", "X X", "XXX"],
  "key": { "X": { "item": "minecraft:iron_ingot" } },
  "result": { "id": "mymod:my_item", "count": 1 }
}
```

### 无序合成
```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "mymod:my_item" },
    { "item": "minecraft:stick" }
  ],
  "result": { "id": "mymod:my_other_item", "count": 1 }
}
```

### 熔炼配方
```json
{
  "type": "minecraft:smelting",
  "ingredient": { "item": "mymod:my_ore" },
  "result": { "id": "mymod:my_ingot" },
  "experience": 0.7,
  "cookingtime": 200
}
```

## Mixin

Mixin 完整教程（含 @Inject、@ModifyVariable、@Redirect、@Accessor、@Invoker、@Shadow、@Unique、Access Widener、源码阅读、多版本兼容、调试技巧等）：**[references/mixin-guide.md](mixin-guide.md)**

### 快速示例

```java
@Mixin(SomeClass.class)
public class MixinSomeClass {
    @Inject(method = "someMethod", at = @At("HEAD"), cancellable = true)
    private void onSomeMethod(CallbackInfo ci) {
        // 修改逻辑
    }
}
```

`mixins.json`：
```json
{
  "required": true,
  "package": "com.example.mymod.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": ["MixinSomeClass"],
  "client": ["MixinTitleScreen"],
  "injectors": { "defaultRequire": 1 }
}
```

## 数据生成

### 入口点
```java
public class MyModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator gen) {
        FabricDataGenerator.Pack pack = gen.createPack();
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModLootTableProvider::new);
        pack.addProvider(ModBlockTagProvider::new);
        pack.addProvider(ModItemTagProvider::new);
        pack.addProvider(ModWorldGenProvider::new);
    }
}
```

### 配方生成
```java
public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
        super(output, registries);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.MY_ITEM)
            .pattern("XXX").pattern("X X").pattern("XXX")
            .input('X', Items.IRON_INGOT)
            .criterion("has_iron", conditionsFromItem(Items.IRON_INGOT))
            .offerTo(exporter);
    }
}
```

## 自定义实体

### 实体类型注册
```java
public class ModEntities {
    public static final EntityType<MyEntity> MY_ENTITY = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(MyMod.MOD_ID, "my_entity"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, MyEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
            .trackRangeChunks(8)
            .build()
    );

    public static void initialize() {}
}
```

### 实体类
```java
public class MyEntity extends LivingEntity {
    public MyEntity(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0);
    }
}
```

### 渲染器（客户端）
```java
@Environment(EnvType.CLIENT)
public class MyEntityRenderer extends MobEntityRenderer<MyEntity, MyEntityModel<MyEntity>> {
    public MyEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new MyEntityModel<>(ctx.getPart(ModModelLayers.MY_ENTITY)), 0.5f);
    }

    @Override
    public Identifier getTexture(MyEntity entity) {
        return Identifier.of(MyMod.MOD_ID, "textures/entity/my_entity.png");
    }
}
```

### 客户端注册
```java
// 在 ClientModInitializer 中
EntityRendererRegistry.register(ModEntities.MY_ENTITY, MyEntityRenderer::new);
EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MY_ENTITY, MyEntityModel::getTexturedModelData);
```

### 生成蛋
```java
public static final Item MY_ENTITY_SPAWN_EGG = ModItems.register("my_entity_spawn_egg",
    new SpawnEggItem(ModEntities.MY_ENTITY, 0xFFFFFF, 0x000000, new Item.Settings()));
```

## 网络通信

### 定义 Payload（1.21+）
```java
public record MyPayload(int value, String message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MyPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.of(MyMod.MOD_ID, "my_packet"));
    public static final StreamCodec<RegistryByteBuf, MyPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MyPayload::value,
            ByteBufCodecs.STRING_UTF8, MyPayload::message,
            MyPayload::new
        );

    @Override public Type<MyPayload> getType() { return TYPE; }
}
```

### 注册接收（ModInitializer）
```java
PayloadTypeRegistry.playS2C().register(MyPayload.TYPE, MyPayload.CODEC);
PayloadTypeRegistry.playC2S().register(MyPayload.TYPE, MyPayload.CODEC);

ServerPlayNetworking.registerGlobalReceiver(MyPayload.TYPE,
    (payload, context) -> {
        // 服务端处理客户端发来的包
        context.player().sendMessage(Text.literal("Received: " + payload.message()));
    });
```

### 发送
```java
// S2C: 发送给单个玩家
ServerPlayNetworking.send(player, new MyPayload(42, "hello"));

// C2S: 发送给服务端
ClientPlayNetworking.send(new MyPayload(42, "hello"));
```

## GUI / Screen

### ScreenHandler
```java
public class MyScreenHandler extends ScreenHandler {
    public MyScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.MY_SCREEN_HANDLER, syncId);
        // 添加玩家物品栏
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canUse(PlayerEntity player) { return true; }
}
```

### 注册 ScreenHandlerType
```java
public class ModScreenHandlers {
    public static final ScreenHandlerType<MyScreenHandler> MY_SCREEN_HANDLER =
        Registry.register(Registries.SCREEN_HANDLER,
            Identifier.of(MyMod.MOD_ID, "my_screen"),
            new SimpleScreenHandlerFactory<>(MyScreenHandler::new));
}
```

### 客户端 Screen
```java
public class MyScreen extends HandledScreen<MyScreenHandler> {
    public MyScreen(MyScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
    }

    @Override protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        // 绘制背景
    }
}
```

### 客户端注册
```java
// 在 ClientModInitializer 中
HandledScreens.register(ModScreenHandlers.MY_SCREEN_HANDLER, MyScreen::new);
```

### 打开 GUI
```java
player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
    (syncId, inv, p) -> new MyScreenHandler(syncId, inv),
    Text.literal("My Screen")
));
```

## 世界生成

### 矿物生成
```java
// 在 ModInitializer 中
BiomeModifications.addFeature(
    BiomeSelectors.foundInOverworld(),
    GenerationStep.Feature.UNDERGROUND_ORES,
    ModPlacedFeatures.MY_ORE_PLACED_KEY
);
```

### 地物注册（数据生成方式）
```java
public class ModPlacedFeatures {
    public static final RegistryKey<PlacedFeature> MY_ORE_PLACED_KEY =
        RegistryKey.of(RegistryKeys.PLACED_FEATURE,
            Identifier.of(MyMod.MOD_ID, "my_ore"));

    // 在 WorldGenProvider 中配置
    public static void bootstrap(Registerable<PlacedFeature> ctx) {
        var config = new OreFeatureConfig(
            RuleTest.BLOCK_STATES,
            ModBlocks.MY_ORE.getDefaultState(),
            8 // 矿脉大小
        );
        ctx.register(MY_ORE_PLACED_KEY,
            new PlacedFeature(
                ctx.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE)
                    .getOrThrow(ModConfiguredFeatures.MY_ORE_KEY),
                List.of(
                    CountPlacementModifier.of(20),           // 每区块矿脉数
                    SquarePlacementModifier.of(),             // 水平分布
                    HeightRangePlacementModifier.uniform(     // 高度范围
                        VerticalAnchor.absolute(-64),
                        VerticalAnchor.absolute(64))
                )
            ));
    }
}
```

## 盔甲与工具

### 盔甲材料（1.21+）
```java
public static final RegistryEntry<ArmorMaterial> MY_ARMOR_MATERIAL =
    ArmorMaterials.register(
        Identifier.of(MyMod.MOD_ID, "my_armor"),
        Map.of(
            ArmorItem.Type.HELMET, 3,
            ArmorItem.Type.CHESTPLATE, 8,
            ArmorItem.Type.LEGGINGS, 6,
            ArmorItem.Type.BOOTS, 3
        ),
        15,                          // 附魔能力
        SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
        () -> Ingredient.ofItems(Items.DIAMOND),
        List.of(new ArmorMaterial.Layer(Identifier.of(MyMod.MOD_ID, "my_armor"))),
        2.0f, 0.0f                   // 韧性、击退抗性
    );

// 盔甲物品
public static final Item MY_HELMET = register("my_helmet",
    new ArmorItem(MY_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings()));
```

### 工具材料（1.21+）
```java
public static final ToolMaterial MY_TOOL_MATERIAL = new ToolMaterial(
    BlockTags.INCORRECT_FOR_DIAMOND_TOOL,  // 可挖掘的方块
    1561,       // 耐久度
    8.0f,       // 挖掘速度
    3.0f,       // 攻击伤害
    10,         // 附魔能力
    Registries.ITEM.getEntry(Items.DIAMOND) // 修复材料
);

// 工具物品
public static final Item MY_SWORD = register("my_sword",
    new SwordItem(MY_TOOL_MATERIAL, new Item.Settings()
        .attributeModifiers(SwordItem.createAttributeModifiers(MY_TOOL_MATERIAL, 3, -2.4f))));
```

## 自定义命令

```java
// 在 ModInitializer 中
CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
    dispatcher.register(CommandManager.literal("mycommand")
        .requires(src -> src.hasPermissionLevel(2))
        .then(CommandManager.argument("target", EntityArgumentType.player())
            .executes(ctx -> {
                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                ctx.getSource().sendFeedback(
                    () -> Text.literal("Executed on " + target.getName().getString()),
                    false);
                return 1;
            }))
        .executes(ctx -> {
            ctx.getSource().sendFeedback(() -> Text.literal("Hello!"), false);
            return 1;
        })
    );
});
```

## 自定义音效

### 注册
```java
public class ModSounds {
    public static final SoundEvent MY_SOUND = Registry.register(
        Registries.SOUND_EVENT,
        Identifier.of(MyMod.MOD_ID, "my_sound"),
        SoundEvent.of(Identifier.of(MyMod.MOD_ID, "my_sound"))
    );

    public static void initialize() {}
}
```

### sounds.json
`assets/mymod/sounds.json`：
```json
{
  "my_sound": {
    "subtitle": "subtitles.mymod.my_sound",
    "sounds": ["mymod:my_sound"]
  }
}
```

音频文件放在 `assets/mymod/sounds/my_sound.ogg`

### 播放
```java
world.playSound(null, pos, ModSounds.MY_SOUND, SoundCategory.BLOCKS, 1.0f, 1.0f);
```

## 构建与测试

```bash
./gradlew build          # 构建，产物在 build/libs/
./gradlew runClient      # 测试客户端
./gradlew runServer      # 测试服务端
./gradlew runDatagen     # 运行数据生成
```

## 更多模板

### BlockEntity（方块实体）

```java
public class MyBlockEntity extends BlockEntity implements ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(9, ItemStack.EMPTY);

    public MyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_BLOCK_ENTITY, pos, state);
    }

    @Override public DefaultedList<ItemStack> getItems() { return inventory; }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.writeNbt(nbt, inventory, registryLookup);
        super.writeNbt(nbt, registryLookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
    }

    public static void tick(World world, BlockPos pos, BlockState state, MyBlockEntity be) {
        // 每 tick 执行的逻辑
    }
}

// 注册 BlockEntityType
public static final BlockEntityType<MyBlockEntity> MY_BLOCK_ENTITY =
    Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MyMod.MOD_ID, "my_block_entity"),
        BlockEntityType.Builder.create(MyBlockEntity::new, ModBlocks.MY_BLOCK).build());

// 方块注册（关联 BlockEntity）
public static final Block MY_BLOCK = register("my_block",
    new BlockWithEntity(AbstractBlock.Settings.create().strength(4.0f)) {
        @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            return new MyBlockEntity(pos, state);
        }
        @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return createCodec(settings -> new MyBlockWithEntity(settings)); }
    }, true);
```

### 自定义创造模式物品栏

```java
public class ModItemGroups {
    public static final ItemGroup MY_GROUP = Registry.register(
        Registries.ITEM_GROUP,
        Identifier.of(MyMod.MOD_ID, "my_group"),
        FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.MY_ITEM))
            .displayName(Text.translatable("itemGroup.mymod.my_group"))
            .entries((context, entries) -> {
                entries.add(ModItems.MY_ITEM);
                entries.add(ModBlocks.MY_BLOCK);
                // 添加所有物品
            })
            .build()
    );

    public static void initialize() {}
}
```

### 语言文件（en_us.json）

`assets/mymod/lang/en_us.json`：
```json
{
  "item.mymod.my_item": "My Item",
  "block.mymod.my_block": "My Block",
  "itemGroup.mymod.my_group": "My Mod",
  "entity.mymod.my_entity": "My Entity",
  "subtitles.mymod.my_sound": "My Sound plays"
}
```

### 战利品表

`data/mymod/loot_table/blocks/my_block.json`：
```json
{
  "type": "minecraft:block",
  "pools": [{
    "rolls": 1,
    "entries": [{ "type": "minecraft:item", "name": "mymod:my_block" }],
    "conditions": [{ "condition": "minecraft:survives_explosion" }]
  }]
}
```

### 矿物辞典标签

`data/mymod/tags/item/my_tag.json`：
```json
{
  "replace": false,
  "values": ["mymod:my_item", "minecraft:diamond"]
}
```

### 燃料注册（熔炉燃料）

```java
// 在 ModInitializer 中
FuelRegistry.INSTANCE.add(ModItems.MY_ITEM, 200); // 200 ticks = 10 秒燃烧
```

### 可堆肥物品

```java
// 在 ModInitializer 中
CompostingChanceRegistry.INSTANCE.add(ModItems.MY_ITEM, 0.65f); // 65% 概率增加堆肥层
```

### 可燃烧方块

```java
// 在 ModInitializer 中
FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.MY_BLOCK, 30, 20);
// 30 = 火势蔓延速度, 20 = 可燃性
```

### 可剥皮原木

```java
// 在 ModInitializer 中
StrippableBlockRegistry.register(ModBlocks.MY_LOG, ModBlocks.MY_STRIPPED_LOG);
```

### 锄头耕地

```java
// 在 ModInitializer 中
// 方式1：方块类中实现
public class MyBlock extends Block {
    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
            BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            // 转换为耕地
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
}

// 方式2：Mixin 注入 HoeItem（详见 mixin-guide.md）
```

### 自定义附魔

```java
// 1.21+ 附魔效果
public class ModEnchantmentEffects {
    public static final EnchantmentEntityEffect LIGHTNING_STRIKER =
        Registry.register(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.of(MyMod.MOD_ID, "lightning_striker"),
            new EnchantmentEntityEffectType<>(LightningStrikerEnchantment.CODEC));

    // 附魔定义
    public static final RegistryEntry<Enchantment> LIGHTNING_STRIKER_ENCHANT =
        register("lightning_striker", new Enchantment(
            Text.literal("Lightning Striker"),
            new Enchantment.Properties(
                Registries.ITEM.getEntry(Items.DIAMOND_SWORD),
                1, 1,
                Enchantment.constantCost(10),
                Enchantment.constantCost(25),
                4,
                EquipmentSlot.MAINHAND
            ),
            Enchantment.leveledEffect(LIGHTNING_STRIKER, LightningStrikerEnchantment::new),
            Enchantment.constantCost(10),
            Enchantment.constantCost(25),
            4,
            EquipmentSlot.MAINHAND
        ));
}
```

### 自定义药水效果

```java
public class ModStatusEffects {
    public static final StatusEffect MY_EFFECT = Registry.register(
        Registries.STATUS_EFFECT,
        Identifier.of(MyMod.MOD_ID, "my_effect"),
        new MyStatusEffect(StatusEffectCategory.BENEFICIAL, 0xAA00FF)
    );

    // 效果类
    public static class MyStatusEffect extends StatusEffect {
        protected MyStatusEffect(StatusEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
            // 每 tick 效果
            entity.heal(1.0f);
            return true;
        }

        @Override
        public boolean canApplyUpdateEffect(int duration, int amplifier) {
            return duration % 20 == 0; // 每秒执行
        }
    }
}
```

### 粒子效果

```java
// 注册粒子类型
public static final SimpleParticleType MY_PARTICLE = Registry.register(
    Registries.PARTICLE_TYPE,
    Identifier.of(MyMod.MOD_ID, "my_particle"),
    new SimpleParticleType(false)
);

// 粒子工厂（客户端）
@Environment(EnvType.CLIENT)
public class MyParticle extends SpriteBillboardParticle {
    protected MyParticle(ClientWorld world, double x, double y, double z,
            double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        this.maxAge = 40; // 存在 40 tick
        this.scale = 0.5f;
    }

    @Override public ParticleTextureSheet getType() { return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT; }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprite;
        public Factory(SpriteProvider sprite) { this.sprite = sprite; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world,
                double x, double y, double z, double vx, double vy, double vz) {
            MyParticle particle = new MyParticle(world, x, y, z, vx, vy, vz);
            particle.setSprite(this.sprite);
            return particle;
        }
    }
}

// 在 ClientModInitializer 中注册
ParticleFactoryRegistry.getInstance().register(ModParticles.MY_PARTICLE, MyParticle.Factory::new);

// 生成粒子
world.addParticle(ModParticles.MY_PARTICLE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0, 0);
```

### 盔甲纹饰材料（1.20+）

```java
public static final RegistryEntry<ArmorTrimMaterial> MY_TRIM_MATERIAL =
    Registry.registerReference(Registries.TRIM_MATERIAL,
        Identifier.of(MyMod.MOD_ID, "my_trim"),
        new ArmorTrimMaterial(
            "mymod_my_trim",
            Registries.ITEM.getEntry(ModItems.MY_ITEM),
            Map.of(),
            Text.translatable("trim_material.mymod.my_trim")
        ));
```

### 自定义村民交易

```java
// 在 ModInitializer 中
TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 1, factories -> {
    factories.add((entity, random) -> new TradeOffer(
        new ItemStack(Items.EMERALD, 3),
        new ItemStack(ModItems.MY_ITEM, 2),
        12, 5, 0.05f
    ));
});
```

### 战利品表注入

```java
// 在 ModInitializer 中
LootTableEvents.MODIFY.register((key, tableBuilder, source) -> {
    if (source.isBuiltin() && Registries.BLOCK.getId(
            Blocks.GRASS_BLOCK).equals(key.getValue())) {
        tableBuilder.pool(LootPool.builder()
            .rolls(ConstantLootNumberProvider.create(1))
            .with(ItemEntry.builder(ModItems.MY_ITEM))
            .conditionally(RandomChanceLootCondition.builder(0.1f))); // 10% 概率
    }
});
```

### 自定义进度

`data/mymod/advancement/my_advancement.json`：
```json
{
  "display": {
    "title": { "translate": "advancements.mymod.my_advancement.title" },
    "description": { "translate": "advancements.mymod.my_advancement.description" },
    "icon": { "id": "mymod:my_item" },
    "background": "minecraft:textures/gui/advancements/backgrounds/adventure.png",
    "frame": "task",
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": false
  },
  "criteria": {
    "got_my_item": {
      "trigger": "minecraft:inventory_changed",
      "conditions": { "items": [{ "items": "mymod:my_item" }] }
    }
  }
}
```

## 参考资源

- Fabric Wiki：https://wiki.fabricmc.net
- Fabric Docs：https://docs.fabricmc.net
- 模板生成器：https://fabricmc.net/develop/template/
- 示例 Mod：https://github.com/FabricMC/fabric-example-mod
- 网络通信：https://docs.fabricmc.net/develop/networking
- 自定义实体：https://fabricmc.net/wiki/tutorial:entity
- Mixin 完整教程：**[mixin-guide.md](mixin-guide.md)**