package org.attack_type.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.attack_type.api.AttackType;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.component.ResistanceManager;
import org.attack_type.fragment.SinFragmentData;
import org.attack_type.fragment.SinFragmentManager;
import org.attack_type.network.NetworkHandler;

public class ResistanceCommand {

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
                                .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(0.0, 5.0))
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
        ));
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

    private static int getResistance(CommandContext<ServerCommandSource> ctx, net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendError(Text.literal("Target must be a living entity"));
            return 0;
        }

        ResistanceProfile profile = ResistanceManager.getOrCreateProfile(living);
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(entity.getName().getString()).append(" ===\n");
        sb.append("Total Product: ").append(String.format("%.2f", profile.getTotalProduct())).append("\n");
        sb.append("Physical:\n");
        for (AttackType type : AttackType.values()) {
            if (type == AttackType.NONE) continue;
            float v = profile.getPhysicalResistance(type);
            sb.append("  ").append(type.name()).append(": ").append(String.format("%.2f", v))
                    .append(" (").append(profile.getResistanceLabel(type)).append(")\n");
        }
        sb.append("Sin:\n");
        for (SinType type : SinType.values()) {
            float v = profile.getSinResistance(type);
            sb.append("  ").append(type.name()).append(": ").append(String.format("%.2f", v))
                    .append(" (").append(profile.getResistanceLabel(type)).append(")\n");
        }

        ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
        return 1;
    }

    private static int setResistance(CommandContext<ServerCommandSource> ctx, net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendError(Text.literal("Target must be a living entity"));
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
            ctx.getSource().sendError(Text.literal("Unknown type: " + typeName + ". Use slash/pierce/blunt or sin names."));
            return 0;
        }

        if (living instanceof ServerPlayerEntity player) {
            ResistanceManager.syncToPlayer(player);
        }

        ctx.getSource().sendFeedback(() -> Text.literal("Set " + typeName + " resistance to " + String.format("%.2f", value) + " for " + entity.getName().getString()), true);
        return 1;
    }

    private static int resetResistance(CommandContext<ServerCommandSource> ctx, net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendError(Text.literal("Target must be a living entity"));
            return 0;
        }

        ResistanceManager.resetProfile(living);
        if (living instanceof ServerPlayerEntity player) {
            ResistanceManager.syncToPlayer(player);
        }

        ctx.getSource().sendFeedback(() -> Text.literal("Reset resistance for " + entity.getName().getString()), true);
        return 1;
    }

    private static int tickResistance(CommandContext<ServerCommandSource> ctx, net.minecraft.entity.Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            ctx.getSource().sendError(Text.literal("Target must be a living entity"));
            return 0;
        }

        ResistanceManager.tickEntityResistance(living, living.getWorld().getTime());
        if (living instanceof ServerPlayerEntity player) {
            ResistanceManager.syncToPlayer(player);
        }

        ctx.getSource().sendFeedback(() -> Text.literal("Forced tick on " + entity.getName().getString()), true);
        return 1;
    }

    private static int fragmentGet(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        SinFragmentData data = SinFragmentManager.getData(target);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Fragments: ").append(target.getName().getString()).append(" ===\n");
        for (SinType type : SinType.values()) {
            int count = data.getFragments(type);
            sb.append("  ").append(type.name()).append(": ").append(count);
            if (count >= 500) sb.append(" [OVERFLOW]");
            if (count >= 1000) sb.append(" [KILL]");
            sb.append("\n");
        }
        sb.append("Active Sin: ").append(data.getActiveSinType().name())
                .append(" L").append(data.getActiveSinLevel()).append("\n");
        ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
        return 1;
    }

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
            ctx.getSource().sendError(Text.literal("Unknown sin type: " + typeName));
            return 0;
        }

        SinFragmentManager.addFragments(target, sinType, amount);
        NetworkHandler.sendFragmentSync(target);
        int newCount = SinFragmentManager.getFragmentCount(target, sinType);
        ctx.getSource().sendFeedback(() -> Text.literal("Added " + amount + " " + typeName + " fragments to " + target.getName().getString() + " (now: " + newCount + ")"), true);
        return 1;
    }

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
            ctx.getSource().sendError(Text.literal("Unknown sin type: " + typeName));
            return 0;
        }

        SinFragmentManager.getData(target).setFragments(sinType, amount);
        NetworkHandler.sendFragmentSync(target);
        ctx.getSource().sendFeedback(() -> Text.literal("Set " + typeName + " fragments to " + amount + " for " + target.getName().getString()), true);
        return 1;
    }
}