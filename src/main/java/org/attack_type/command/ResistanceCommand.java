package org.attack_type.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.attack_type.api.AttackType;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.component.ResistanceManager;
import org.attack_type.fragment.SinFragmentData;
import org.attack_type.fragment.SinFragmentManager;
import org.attack_type.network.NetworkHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 攻击类型系统管理命令。
 * <p>
 * 注册 {@code /attacktype} 根命令（权限等级 2），包含以下子命令：
 * <ul>
 *   <li>{@code /attacktype get [target]} — 查看实体抗性</li>
 *   <li>{@code /attacktype set <type> <value> [target]} — 设置抗性值</li>
 *   <li>{@code /attacktype reset [target]} — 重置抗性为默认</li>
 *   <li>{@code /attacktype tick [target]} — 手动触发一次抗性衰减</li>
 *   <li>{@code /attacktype fragment get [target]} — 查看碎片数据</li>
 *   <li>{@code /attacktype fragment add <type> <amount> [target]} — 增加碎片</li>
 *   <li>{@code /attacktype fragment set <type> <amount> [target]} — 设置碎片</li>
 * </ul>
 */
public class ResistanceCommand {

    /**
     * 注册命令到调度器。
     *
     * @param dispatcher Minecraft 命令调度器
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("attacktype")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("get")
                        .executes(ctx -> getResistance(ctx, ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .executes(ctx -> getResistance(ctx, EntityArgumentType.getEntity(ctx, "target")))))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (AttackType type : AttackType.values()) {
                                        if (type != AttackType.NONE) builder.suggest(type.name().toLowerCase());
                                    }
                                    for (SinType type : SinType.values()) {
                                        builder.suggest(type.name().toLowerCase());
                                    }
                                    return builder.buildFuture();
                                })
                                .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ctx -> setResistance(ctx, ctx.getSource().getPlayerOrThrow()))
                                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                                .executes(ctx -> setResistance(ctx, EntityArgumentType.getEntity(ctx, "target")))))))
                .then(CommandManager.literal("reset")
                        .executes(ctx -> resetResistance(ctx, ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .executes(ctx -> resetResistance(ctx, EntityArgumentType.getEntity(ctx, "target")))))
                .then(CommandManager.literal("tick")
                        .executes(ctx -> tickResistance(ctx, ctx.getSource().getPlayerOrThrow()))
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .executes(ctx -> tickResistance(ctx, EntityArgumentType.getEntity(ctx, "target")))))
                .then(CommandManager.literal("fragment")
                        .then(buildFragmentGet())
                        .then(buildFragmentAdd())
                        .then(buildFragmentSet())
                )
                .then(CommandManager.literal("test")
                        .executes(ResistanceCommand::spawnTestDogs)
                )
        );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildFragmentGet() {
        return CommandManager.literal("get")
                .executes(ctx -> fragmentGet(ctx, ctx.getSource().getPlayerOrThrow()))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> fragmentGet(ctx, EntityArgumentType.getPlayer(ctx, "target"))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildFragmentAdd() {
        return CommandManager.literal("add")
                .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (SinType type : SinType.values()) {
                                builder.suggest(type.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg())
                                .executes(ctx -> fragmentAdd(ctx, ctx.getSource().getPlayerOrThrow()))
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .executes(ctx -> fragmentAdd(ctx, EntityArgumentType.getPlayer(ctx, "target"))))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildFragmentSet() {
        return CommandManager.literal("set")
                .then(CommandManager.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (SinType type : SinType.values()) {
                                builder.suggest(type.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg())
                                .executes(ctx -> fragmentSet(ctx, ctx.getSource().getPlayerOrThrow()))
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .executes(ctx -> fragmentSet(ctx, EntityArgumentType.getPlayer(ctx, "target"))))));
    }

    /**
     * 查看实体抗性信息。
     */
    private static int getResistance(CommandContext<ServerCommandSource> ctx, net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendError(Text.translatable("cmd.attack_type.err_not_living"));
            return 0;
        }

        ResistanceProfile profile = ResistanceManager.getOrCreateProfile(living);
        StringBuilder sb = new StringBuilder();
        sb.append(t("cmd.attack_type.get_title", entity.getName().getString())).append("\n");
        sb.append(t("cmd.attack_type.total_product", profile.getTotalProduct())).append("\n");
        sb.append(t("cmd.attack_type.physical_title")).append("\n");
        for (AttackType type : AttackType.values()) {
            if (type == AttackType.NONE) continue;
            float v = profile.getPhysicalResistance(type);
            sb.append(t("cmd.attack_type.resistance_row", t("attack_type.attack_type." + type.name().toLowerCase()), v, profile.getResistanceLabel(type))).append("\n");
        }
        sb.append(t("cmd.attack_type.sin_title")).append("\n");
        for (SinType type : SinType.values()) {
            float v = profile.getSinResistance(type);
            sb.append(t("cmd.attack_type.resistance_row", t("sin.attack_type." + type.name().toLowerCase()), v, profile.getResistanceLabel(type))).append("\n");
        }

        ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
        return 1;
    }

    /**
     * 设置实体抗性值。
     */
    private static int setResistance(CommandContext<ServerCommandSource> ctx, net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendError(Text.translatable("cmd.attack_type.err_not_living"));
            return 0;
        }

        String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
        double value = DoubleArgumentType.getDouble(ctx, "value");

        ResistanceProfile profile = ResistanceManager.getOrCreateProfile(living);

        boolean found = false;
        for (AttackType type : AttackType.values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                profile.setPhysicalResistance(type, (float) value);
                found = true;
                break;
            }
        }
        if (!found) {
            for (SinType type : SinType.values()) {
                if (type.name().equalsIgnoreCase(typeName)) {
                    profile.setSinResistance(type, (float) value);
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            ctx.getSource().sendError(Text.translatable("cmd.attack_type.err_unknown_type", typeName));
            return 0;
        }

        if (living instanceof ServerPlayerEntity player) {
            ResistanceManager.syncToPlayer(player);
        }

        String displayName = resolveTypeName(typeName);
        ctx.getSource().sendFeedback(() -> Text.translatable("cmd.attack_type.set_ok", displayName, value, entity.getName().getString()), true);
        return 1;
    }

    /**
     * 重置实体抗性为默认值。
     */
    private static int resetResistance(CommandContext<ServerCommandSource> ctx, net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendError(Text.translatable("cmd.attack_type.err_not_living"));
            return 0;
        }

        ResistanceManager.resetProfile(living);
        if (living instanceof ServerPlayerEntity player) {
            ResistanceManager.syncToPlayer(player);
        }

        ctx.getSource().sendFeedback(() -> Text.translatable("cmd.attack_type.reset_ok", entity.getName().getString()), true);
        return 1;
    }

    /**
     * 手动触发一次实体抗性衰减。
     */
    private static int tickResistance(CommandContext<ServerCommandSource> ctx, net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendError(Text.translatable("cmd.attack_type.err_not_living"));
            return 0;
        }

        ResistanceManager.tickEntityResistance(living, living.getWorld().getTime());
        if (living instanceof ServerPlayerEntity player) {
            ResistanceManager.syncToPlayer(player);
        }

        ctx.getSource().sendFeedback(() -> Text.translatable("cmd.attack_type.tick_ok", entity.getName().getString()), true);
        return 1;
    }

    /**
     * 查看玩家碎片数据。
     */
    private static int fragmentGet(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        SinFragmentData data = SinFragmentManager.getData(target);
        StringBuilder sb = new StringBuilder();
        sb.append(t("cmd.attack_type.frag_title", target.getName().getString())).append("\n");
        for (SinType type : SinType.values()) {
            int count = data.getFragments(type);
            String row = t("cmd.attack_type.frag_row", t("sin.attack_type." + type.name().toLowerCase()), count).getString();
            sb.append(row);
            if (count >= 500) sb.append(t("cmd.attack_type.frag_overflow"));
            if (count >= 1000) sb.append(t("cmd.attack_type.frag_kill"));
            sb.append("\n");
        }
        String sinName = t("sin.attack_type." + data.getActiveSinType().name().toLowerCase()).getString();
        sb.append(t("cmd.attack_type.frag_active", sinName, data.getActiveSinLevel())).append("\n");
        ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
        return 1;
    }

    /**
     * 增加玩家碎片。
     */
    private static int fragmentAdd(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
        int amount = (int) DoubleArgumentType.getDouble(ctx, "amount");

        SinType sinType = null;
        for (SinType type : SinType.values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                sinType = type;
                break;
            }
        }
        if (sinType == null) {
            ctx.getSource().sendError(Text.translatable("cmd.attack_type.err_unknown_sin", typeName));
            return 0;
        }

        SinFragmentManager.addFragments(target, sinType, amount);
        NetworkHandler.sendFragmentSync(target);
        int newCount = SinFragmentManager.getFragmentCount(target, sinType);
        String sinName = t("sin.attack_type." + sinType.name().toLowerCase()).getString();
        ctx.getSource().sendFeedback(() -> Text.translatable("cmd.attack_type.frag_add_ok", amount, sinName, target.getName().getString(), newCount), true);
        return 1;
    }

    /**
     * 设置玩家碎片数量。
     */
    private static int fragmentSet(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
        int amount = (int) DoubleArgumentType.getDouble(ctx, "amount");

        SinType sinType = null;
        for (SinType type : SinType.values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                sinType = type;
                break;
            }
        }
        if (sinType == null) {
            ctx.getSource().sendError(Text.translatable("cmd.attack_type.err_unknown_sin", typeName));
            return 0;
        }

SinFragmentManager.getData(target).setFragments(sinType, amount);
        NetworkHandler.sendFragmentSync(target);
        String sinName = t("sin.attack_type." + sinType.name().toLowerCase()).getString();
        ctx.getSource().sendFeedback(() -> Text.translatable("cmd.attack_type.frag_set_ok", sinName, target.getName().getString(), amount), true);
        return 1;
    }

    /**
     * 获取类型名称的国际化显示文本。
     */
    private static String resolveTypeName(String typeName) {
        for (AttackType type : AttackType.values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                return t("attack_type.attack_type." + type.name().toLowerCase()).getString();
            }
        }
        for (SinType type : SinType.values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                return t("sin.attack_type." + type.name().toLowerCase()).getString();
            }
        }
        return typeName;
    }

    /**
     * 生成测试狗（/attacktype test）。
     * <p>
     * 在玩家周围生成 10 只具有极端抗性值的测试狗，互相敌对，便于实验。
     * <ul>
     *   <li>#0: 全抗性 0.0（免疫）</li>
     *   <li>#1: 斩击 0.0，其他正常</li>
     *   <li>#2: 突刺 0.0，其他正常</li>
     *   <li>#3: 打击 0.0，其他正常</li>
     *   <li>#4: 暴怒罪孽 0.0</li>
     *   <li>#5: 全抗性 50.0（致命）</li>
     *   <li>#6: 全抗性 0.0, 总积 0.1</li>
     *   <li>#7: 斩击 100.0, 其他 0.01</li>
     *   <li>#8: 突刺 100.0, 其他 0.01</li>
     *   <li>#9: 打击 100.0, 其他 0.01</li>
     * </ul>
     */
    private static int spawnTestDogs(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        BlockPos pos = new BlockPos((int)source.getPosition().x, (int)source.getPosition().y, (int)source.getPosition().z);
        ServerWorld world = source.getWorld();

        List<WolfEntity> dogs = new ArrayList<>();
        String[] names = {
            "All 0.0",
            "Slash 0.0",
            "Pierce 0.0",
            "Blunt 0.0",
            "Wrath 0.0",
            "All 50.0",
            "All 0.0 prod=0.1",
            "Slash 100",
            "Pierce 100",
            "Blunt 100"
        };

        for (int i = 0; i < 10; i++) {
            WolfEntity wolf = new WolfEntity(net.minecraft.entity.EntityType.WOLF, world);
            BlockPos spawnPos = pos.add(i * 2, 0, 0);
            wolf.refreshPositionAndAngles(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
            wolf.setCustomName(Text.literal(names[i]));
            wolf.setCustomNameVisible(true);
            wolf.setPersistent();
            wolf.setHealth(wolf.getMaxHealth());

            ResistanceProfile profile = ResistanceManager.getOrCreateProfile(wolf);
            applyTestProfile(profile, i);
            world.spawnEntity(wolf);
            dogs.add(wolf);
        }

        for (WolfEntity dog : dogs) {
            for (WolfEntity other : dogs) {
                if (dog != other) {
                    dog.setAngryAt(other.getUuid());
                    dog.setAngerTime(Integer.MAX_VALUE);
                }
            }
        }

        source.sendFeedback(() -> Text.translatable("cmd.attack_type.test_spawned", dogs.size()), true);
        return 1;
    }

    /**
     * 为测试狗应用极端抗性配置。
     *
     * @param profile 抗性配置
     * @param index   测试狗编号（0-9）
     */
    private static void applyTestProfile(ResistanceProfile profile, int index) {
        switch (index) {
            case 0:
                for (AttackType at : AttackType.values()) {
                    if (at != AttackType.NONE) profile.setPhysicalResistance(at, 0.0f);
                }
                for (SinType st : SinType.values()) {
                    profile.setSinResistance(st, 0.0f);
                }
                break;
            case 1:
                profile.setPhysicalResistance(AttackType.SLASH, 0.0f);
                break;
            case 2:
                profile.setPhysicalResistance(AttackType.PIERCE, 0.0f);
                break;
            case 3:
                profile.setPhysicalResistance(AttackType.BLUNT, 0.0f);
                break;
            case 4:
                profile.setSinResistance(SinType.WRATH, 0.0f);
                break;
            case 5:
                for (AttackType at : AttackType.values()) {
                    if (at != AttackType.NONE) profile.setPhysicalResistance(at, 50.0f);
                }
                for (SinType st : SinType.values()) {
                    profile.setSinResistance(st, 50.0f);
                }
                break;
            case 6:
                for (AttackType at : AttackType.values()) {
                    if (at != AttackType.NONE) profile.setPhysicalResistance(at, 0.0f);
                }
                for (SinType st : SinType.values()) {
                    profile.setSinResistance(st, 0.0f);
                }
                profile.setTotalProduct(0.1f);
                break;
            case 7:
                profile.setPhysicalResistance(AttackType.SLASH, 100.0f);
                profile.setPhysicalResistance(AttackType.PIERCE, 0.01f);
                profile.setPhysicalResistance(AttackType.BLUNT, 0.01f);
                for (SinType st : SinType.values()) {
                    profile.setSinResistance(st, 0.01f);
                }
                break;
            case 8:
                profile.setPhysicalResistance(AttackType.PIERCE, 100.0f);
                profile.setPhysicalResistance(AttackType.SLASH, 0.01f);
                profile.setPhysicalResistance(AttackType.BLUNT, 0.01f);
                for (SinType st : SinType.values()) {
                    profile.setSinResistance(st, 0.01f);
                }
                break;
            case 9:
                profile.setPhysicalResistance(AttackType.BLUNT, 100.0f);
                profile.setPhysicalResistance(AttackType.SLASH, 0.01f);
                profile.setPhysicalResistance(AttackType.PIERCE, 0.01f);
                for (SinType st : SinType.values()) {
                    profile.setSinResistance(st, 0.01f);
                }
                break;
        }
    }

    /**
     * 翻译键辅助方法。
     */
    private static net.minecraft.text.MutableText t(String key, Object... args) {
        return Text.translatable(key, args);
    }
}