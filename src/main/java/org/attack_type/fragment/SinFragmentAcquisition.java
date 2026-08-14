package org.attack_type.fragment;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.attack_type.api.SinType;
import org.attack_type.config.ModConfig;
import org.attack_type.network.NetworkHandler;

import java.util.*;

/**
 * 罪孽碎片获取系统。
 * <p>
 * 为 7 种罪孽属性提供独立的碎片获取方式，每种罪孽体现不同的「性格」：
 * <ul>
 *   <li>{@link SinType#WRATH 暴怒} — 连杀追踪</li>
 *   <li>{@link SinType#LUST 色欲} — 繁殖与变异</li>
 *   <li>{@link SinType#SLOTH 怠惰} — 静止与睡眠</li>
 *   <li>{@link SinType#GLUTTONY 暴食} — 进食</li>
 *   <li>{@link SinType#GLOOM 忧郁} — 受伤与目击</li>
 *   <li>{@link SinType#PRIDE 傲慢} — 成就与生产</li>
 *   <li>{@link SinType#ENVY 嫉妒} — 攀比与目击</li>
 * </ul>
 */
public class SinFragmentAcquisition {

    /** 上次击杀时间（WRATH） */
    private static final Map<UUID, Long> LAST_KILL_TIME = new HashMap<>();
    /** 玩家上一帧位置（SLOTH AFK检测） */
    private static final Map<UUID, Vec3d> LAST_POS = new HashMap<>();
    /** AFK 累计 tick（SLOTH） */
    private static final Map<UUID, Integer> AFK_TICKS = new HashMap<>();
    /** 累计制作物品数（PRIDE） */
    private static final Map<UUID, Integer> CRAFTED_COUNT = new HashMap<>();
    /** 嫉妒攀比检测计时器（ENVY） */
    private static final Map<UUID, Integer> ENVY_TIMER = new HashMap<>();
    /** 鸡蛋上次使用时间（LUST 冷却） */
    private static final Map<UUID, Long> LAST_EGG_TIME = new HashMap<>();

    /**
     * 注册所有碎片获取事件监听器。在模组初始化时调用。
     */
    public static void register() {
        registerWrath();
        registerLust();
        registerSloth();
        registerGluttony();
        registerGloom();
        registerPride();
        registerEnvy();
    }

    // ==================== WRATH 暴怒 — 连杀 ====================

    private static void registerWrath() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getWorld().isClient) return;
            if (source.getAttacker() instanceof ServerPlayerEntity player) {
                long now = entity.getWorld().getTime();
                Long last = LAST_KILL_TIME.get(player.getUuid());
                if (last != null) {
                    long diff = now - last;
                    int bonus = 0;
                    int[] intervals = ModConfig.WRATH_KILL_CHAIN_INTERVALS;
                    int[] bonuses = ModConfig.WRATH_KILL_CHAIN_BONUSES;
                    for (int i = 0; i < intervals.length; i++) {
                        if (diff <= intervals[i] && bonus == 0) {
                            bonus = bonuses[i];
                        }
                    }
                    if (bonus > 0) {
                        SinFragmentManager.addFragments(player, SinType.WRATH, bonus);
                        NetworkHandler.sendFragmentSync(player);
                    }
                }
                LAST_KILL_TIME.put(player.getUuid(), now);
            }
        });
    }

    // ==================== LUST 色欲 — 繁殖与变异 ====================

    private static void registerLust() {
        // 繁殖由 MixinAnimalEntity 处理
        // 闪电变异由 MixinLightningStrike 处理
        // 僵尸村民治愈由 MixinZombieVillagerEntity 处理
        // 僵尸转溺尸由 MixinZombieEntity 处理

        // 使用物品：鸡蛋（5秒冷却）
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() == Items.EGG) {
                long now = world.getTime();
                Long last = LAST_EGG_TIME.get(player.getUuid());
                if (last == null || now - last >= ModConfig.LUST_EGG_COOLDOWN_TICKS) {
                    LAST_EGG_TIME.put(player.getUuid(), now);
                    SinFragmentManager.addFragments((ServerPlayerEntity) player, SinType.LUST, ModConfig.LUST_EGG_AMOUNT);
                    NetworkHandler.sendFragmentSync((ServerPlayerEntity) player);
                }
            }
            return TypedActionResult.pass(stack);
        });
    }

    private static void findNearbyPlayerAndAddLustFragment(World world, Vec3d pos, int amount) {
        Box box = new Box(pos.x - 16, pos.y - 16, pos.z - 16, pos.x + 16, pos.y + 16, pos.z + 16);
        for (PlayerEntity player : world.getPlayers()) {
            if (player.getPos().distanceTo(pos) <= 16) {
                SinFragmentManager.addFragments((ServerPlayerEntity) player, SinType.LUST, amount);
                NetworkHandler.sendFragmentSync((ServerPlayerEntity) player);
            }
        }
    }

    // ==================== SLOTH 怠惰 — 静止与睡眠 ====================

    private static void registerSloth() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                Vec3d pos = player.getPos();
                Vec3d last = LAST_POS.get(uuid);

                boolean moved = false;
                if (last != null) {
                    moved = Math.abs(pos.x - last.x) > 0.001
                            || Math.abs(pos.y - last.y) > 0.001
                            || Math.abs(pos.z - last.z) > 0.001;
                }
                LAST_POS.put(uuid, pos);

                int ticks = AFK_TICKS.getOrDefault(uuid, 0);
                if (!moved && !player.isSprinting() && player.isOnGround()) {
                    ticks++;
                    if (ticks >= ModConfig.SLOTH_AFK_INTERVAL_TICKS) {
                        SinFragmentManager.addFragments(player, SinType.SLOTH, ModConfig.SLOTH_AFK_AMOUNT);
                        NetworkHandler.sendFragmentSync(player);
                        AFK_TICKS.put(uuid, 0);
                    } else {
                        AFK_TICKS.put(uuid, ticks);
                    }
                } else {
                    AFK_TICKS.put(uuid, 0);
                }
            }
        });

        // 睡眠完成由 MixinServerPlayerEntity.wakeUp() 注入处理
    }

    // ==================== GLUTTONY 暴食 — 进食 ====================

    private static void registerGluttony() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isFood()) {
                SinFragmentManager.addFragments((ServerPlayerEntity) player, SinType.GLUTTONY, ModConfig.GLUTTONY_PER_FOOD);
                NetworkHandler.sendFragmentSync((ServerPlayerEntity) player);
            }
            return TypedActionResult.pass(stack);
        });
    }

    // ==================== GLOOM 忧郁 — 受伤与目击 ====================

    private static void registerGloom() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.getWorld().isClient) return true;

            if (entity instanceof ServerPlayerEntity player) {
                int gloomFrag = (int) Math.ceil(amount / (double) ModConfig.GLOOM_DAMAGE_PER_FRAGMENT);
                if (gloomFrag > 0) {
                    SinFragmentManager.addFragments(player, SinType.GLOOM, gloomFrag * ModConfig.GLOOM_SELF_AMOUNT);
                    NetworkHandler.sendFragmentSync(player);
                }
                return true;
            }

            if (source.getAttacker() instanceof PlayerEntity) return true;

            int gloomFrag = (int) Math.ceil(amount / (double) ModConfig.GLOOM_DAMAGE_PER_FRAGMENT);
            if (gloomFrag <= 0) return true;

            Box box = entity.getBoundingBox().expand(32);
            for (PlayerEntity player : entity.getWorld().getPlayers()) {
                if (player.getPos().distanceTo(entity.getPos()) <= 32) {
                    SinFragmentManager.addFragments((ServerPlayerEntity) player, SinType.GLOOM, gloomFrag * ModConfig.GLOOM_WITNESS_AMOUNT);
                    NetworkHandler.sendFragmentSync((ServerPlayerEntity) player);
                }
            }
            return true;
        });
    }

    // ==================== PRIDE 傲慢 — 成就与生产 ====================

    private static final Map<UUID, Integer> PRIDE_TICK_COUNTER = new HashMap<>();

    private static void registerPride() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                int counter = PRIDE_TICK_COUNTER.getOrDefault(uuid, 0) + 1;
                if (counter < 100) {
                    PRIDE_TICK_COUNTER.put(uuid, counter);
                    continue;
                }
                PRIDE_TICK_COUNTER.put(uuid, 0);

                int sum = 0;
                for (net.minecraft.item.Item item : net.minecraft.registry.Registries.ITEM) {
                    sum += player.getStatHandler().getStat(Stats.CRAFTED.getOrCreateStat(item));
                }
                int prev = CRAFTED_COUNT.getOrDefault(uuid, 0);
                if (sum - prev >= ModConfig.PRIDE_CRAFT_THRESHOLD) {
                    int multiples = (sum - prev) / ModConfig.PRIDE_CRAFT_THRESHOLD;
                    SinFragmentManager.addFragments(player, SinType.PRIDE, multiples);
                    NetworkHandler.sendFragmentSync(player);
                    CRAFTED_COUNT.put(uuid, prev + multiples * ModConfig.PRIDE_CRAFT_THRESHOLD);
                } else if (prev == 0 && sum > 0) {
                    CRAFTED_COUNT.put(uuid, sum);
                }
            }
        });
    }

    // ==================== ENVY 嫉妒 — 攀比与目击 ====================

    /** 装备材质等级 */
    private static int getMaterialTier(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) {
            return getArmorMaterialTier(armor.getMaterial());
        }
        if (item instanceof ToolItem tool) {
            return getToolMaterialTier(tool.getMaterial());
        }
        return getItemTier(item);
    }

    private static int getArmorMaterialTier(ArmorMaterial mat) {
        String name = mat.getName().toLowerCase();
        if (name.contains("netherite")) return 6;
        if (name.contains("diamond")) return 5;
        if (name.contains("gold") || name.contains("golden")) return 4;
        if (name.contains("iron")) return 3;
        if (name.contains("stone")) return 2;
        if (name.contains("wood") || name.contains("leather")) return 1;
        return 0;
    }

    private static int getToolMaterialTier(ToolMaterial mat) {
        String name = mat.toString().toLowerCase();
        if (name.contains("netherite")) return 6;
        if (name.contains("diamond")) return 5;
        if (name.contains("gold")) return 4;
        if (name.contains("iron")) return 3;
        if (name.contains("stone")) return 2;
        if (name.contains("wood")) return 1;
        return 0;
    }

    private static int getItemTier(Item item) {
        String name = item.toString().toLowerCase();
        if (name.contains("netherite")) return 6;
        if (name.contains("diamond")) return 5;
        if (name.contains("gold")) return 4;
        if (name.contains("iron")) return 3;
        if (name.contains("stone")) return 2;
        if (name.contains("wood") || name.contains("leather")) return 1;
        return 0;
    }

    /** 检查装备是否有附魔 */
    private static boolean isEnchanted(ItemStack stack) {
        return stack.hasEnchantments();
    }

    /** 获取玩家最佳装备等级 */
    private static int getPlayerBestTier(PlayerEntity player) {
        int best = 0;
        for (ItemStack armor : player.getArmorItems()) {
            // 物品等级 + 附魔加成
            int tier = getMaterialTier(armor);
            if (isEnchanted(armor)) tier += 10;
            if (tier > best) best = tier;
        }
        ItemStack mainHand = player.getMainHandStack();
        int weaponTier = getMaterialTier(mainHand);
        if (isEnchanted(mainHand)) weaponTier += 10;
        if (weaponTier > best) best = weaponTier;
        return best;
    }

    private static void registerEnvy() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                int timer = ENVY_TIMER.getOrDefault(uuid, 0);
                timer++;
                if (timer >= ModConfig.ENVY_INTERVAL_TICKS) {
                    ENVY_TIMER.put(uuid, 0);
                    int myTier = getPlayerBestTier(player);
                    Box box = player.getBoundingBox().expand(16);
                    for (Entity entity : player.getWorld().getOtherEntities(player, box, e -> e instanceof LivingEntity)) {
                        if (entity instanceof PlayerEntity otherPlayer) {
                            int otherTier = getPlayerBestTier(otherPlayer);
                            if (otherTier > myTier) {
                                SinFragmentManager.addFragments(player, SinType.ENVY, ModConfig.ENVY_COMPARE_AMOUNT);
                                NetworkHandler.sendFragmentSync(player);
                                break;
                            }
                        }
                    }
                } else {
                    ENVY_TIMER.put(uuid, timer);
                }
            }
        });
    }

    /**
     * 通知附近玩家目击了罪孽攻击（ENVY +1）。
     * 由 MixinLivingEntity 在罪孽伤害计算时调用。
     *
     * @param world   当前世界
     * @param pos     攻击发生位置
     */
    public static void notifySinAttackWitnessed(World world, Vec3d pos) {
        if (world.isClient) return;
        Box box = new Box(pos.x - 16, pos.y - 16, pos.z - 16, pos.x + 16, pos.y + 16, pos.z + 16);
        for (PlayerEntity player : world.getPlayers()) {
            if (player.getPos().distanceTo(pos) <= 16) {
                SinFragmentManager.addFragments((ServerPlayerEntity) player, SinType.ENVY, ModConfig.ENVY_WITNESS_AMOUNT);
                NetworkHandler.sendFragmentSync((ServerPlayerEntity) player);
            }
        }
    }

    /**
     * 处理闪电变异导致的色欲碎片。
     * 由 Mixin 在实体被闪电击中时调用。
     *
     * @param world  当前世界
     * @param victim 被闪电击中的实体
     */
    public static void onLightningStrike(World world, Entity victim) {
        if (world.isClient) return;
        boolean isTransformable = victim instanceof PigEntity
                || victim instanceof CreeperEntity
                || victim instanceof MooshroomEntity
                || victim instanceof VillagerEntity;
        if (isTransformable) {
            findNearbyPlayerAndAddLustFragment(world, victim.getPos(), ModConfig.LUST_LIGHTNING_AMOUNT);
        }
    }

    /**
     * 处理僵尸村民治愈（LUST +10）。
     */
    public static void onZombieVillagerCured(World world, Vec3d pos) {
        if (world.isClient) return;
        findNearbyPlayerAndAddLustFragment(world, pos, ModConfig.LUST_CURE_AMOUNT);
    }

    /**
     * 处理僵尸转溺尸（LUST +5）。
     */
    public static void onZombieConvertToDrowned(World world, Vec3d pos) {
        if (world.isClient) return;
        findNearbyPlayerAndAddLustFragment(world, pos, ModConfig.LUST_DROWN_AMOUNT);
    }

    /**
     * 处理动物繁殖成功（LUST +1）。
     */
    public static void onAnimalBreed(World world, Vec3d pos) {
        if (world.isClient) return;
        findNearbyPlayerAndAddLustFragment(world, pos, ModConfig.LUST_BREED_AMOUNT);
    }

    /**
     * 处理成就/进度达成（PRIDE +10）。
     */
    public static void onAdvancement(ServerPlayerEntity player) {
        SinFragmentManager.addFragments(player, SinType.PRIDE, ModConfig.PRIDE_ACHIEVEMENT_AMOUNT);
        NetworkHandler.sendFragmentSync(player);
    }

    /**
     * 处理烧炼/酿造/附魔（PRIDE +2）。
     */
    public static void onProduction(ServerPlayerEntity player) {
        SinFragmentManager.addFragments(player, SinType.PRIDE, ModConfig.PRIDE_PRODUCTION_AMOUNT);
        NetworkHandler.sendFragmentSync(player);
    }

    /**
     * 处理玩家从睡眠中醒来（SLOTH +5）。
     * 由 MixinServerPlayerEntity.wakeUp() 注入调用。
     */
    public static void onPlayerWakeUp(ServerPlayerEntity player) {
        SinFragmentManager.addFragments(player, SinType.SLOTH, ModConfig.SLOTH_SLEEP_AMOUNT);
        NetworkHandler.sendFragmentSync(player);
    }
}