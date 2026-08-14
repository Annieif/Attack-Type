package org.attack_type.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Vec3d;
import org.attack_type.advancement.ModAdvancements;
import org.attack_type.api.AttackType;
import org.attack_type.api.AttackTypeMapper;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.component.ResistanceManager;
import org.attack_type.effect.*;
import org.attack_type.enchantment.ModEnchantments;
import org.attack_type.enchantment.PhysicalResistanceEnchantment;
import org.attack_type.fragment.SinFragmentAcquisition;
import org.attack_type.fragment.SinFragmentData;
import org.attack_type.fragment.SinFragmentManager;
import org.attack_type.network.NetworkHandler;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LivingEntity 伤害计算 Mixin 注入。
 * <p>
 * 通过三个注入点修改 Minecraft 原版伤害系统：
 * <ol>
 *   <li>{@code damage() HEAD} — 计算物理抗性乘数和罪孽附加伤害，存入 ThreadLocal</li>
 *   <li>{@code applyDamage()} INVOKE — 修改最终伤害值：{@code amount × physMult + sinDamage}</li>
 *   <li>{@code tick()} HEAD — 触发实体抗性周期衰减</li>
 * </ol>
 * 额外注入：NBT 读写持久化、死亡清理。
 *
 * <h3>伤害公式</h3>
 * <pre>
 * 最终伤害 = (物理伤害 × 物理抗性 × 护甲抗性附魔) × 原版护甲减伤 + (sinLevel × 3 + 1) × 罪孽抗性
 * </pre>
 * 其中 {@code applyDamage()} 修改的是传入原版护甲计算前的 amount，实际公式为：
 * <pre>
 * 最终伤害 = applyDamage(amount × physMult) + sinDamage
 * </pre>
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /** 待应用的物理抗性乘数（ThreadLocal，跨 inject 传递） */
    @Unique
    private static final ThreadLocal<Float> PENDING_PHYS_MULT = new ThreadLocal<>();

    /** 待应用的罪孽附加伤害（ThreadLocal，跨 inject 传递） */
    @Unique
    private static final ThreadLocal<Float> PENDING_SIN_DAMAGE = new ThreadLocal<>();

    /**
     * 伤害计算入口（HEAD 注入）。
     * <p>
     * 步骤：
     * <ol>
     *   <li>判定攻击类型（NONE → 跳过所有计算）</li>
     *   <li>获取目标实体的物理抗性</li>
     *   <li>叠加护甲物理抗性附魔</li>
     *   <li>检查攻击者是否应被即死（碎片 ≥ 1000）</li>
     *   <li>判定罪孽属性 + 等级，计算附加伤害</li>
     *   <li>将结果存入 ThreadLocal 供 {@link #addSinDamageToFinal(float)} 使用</li>
     * </ol>
     *
     * @param source 伤害来源
     * @param amount 原始伤害值
     * @param cir    Mixin 回调
     */
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamageHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        AttackType attackType = AttackTypeMapper.getAttackType(source);

        if (attackType == AttackType.NONE) {
            PENDING_PHYS_MULT.set(1.0f);
            PENDING_SIN_DAMAGE.set(0.0f);
            return;
        }

        ResistanceProfile profile = ResistanceManager.getProfile(self);
        float physResistance = profile.getPhysicalResistance(attackType);

        if (source.getSource() instanceof LivingEntity attacker) {
            physResistance *= getArmorResistance(self, attackType);

            if (attacker instanceof PlayerEntity player && AttackTypeMapper.shouldKillPlayer(attacker)) {
                player.kill();
                PENDING_SIN_DAMAGE.set(0.0f);
                PENDING_PHYS_MULT.set(physResistance);
                if (player instanceof ServerPlayerEntity sp) {
                    ModAdvancements.grant(sp, ModAdvancements.INSTANT_KILL);
                }
                return;
            }

            SinType sinType = AttackTypeMapper.getSinType(attacker);
            if (sinType != null) {
                int sinLevel = AttackTypeMapper.getSinLevel(attacker);
                float sinDamage = (sinLevel * 3.0f + 1.0f) * profile.getSinResistance(sinType);

                sinDamage = applySinEffects(attacker, self, sinType, sinDamage);
                physResistance = applyPhysEffects(attacker, self, sinType, physResistance);

                PENDING_SIN_DAMAGE.set(sinDamage);

                if (!self.getWorld().isClient) {
                    ServerWorld sw = (ServerWorld) self.getWorld();
                    spawnSinParticles(sw, self.getX(), self.getY() + self.getHeight() / 2, self.getZ(), sinType, sinLevel);
                    SinFragmentAcquisition.notifySinAttackWitnessed(self.getWorld(), self.getPos());

                    if (attacker instanceof ServerPlayerEntity sp) {
                        ModAdvancements.recordSinTrigger(sp, sinType);
                    }

                    sw.playSound(null, self.getX(), self.getY(), self.getZ(),
                            SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE,
                            0.3f + 0.1f * sinLevel, 0.8f + 0.2f * sinLevel);

                    self.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NAUSEA, 20 * sinLevel, 0,
                            false, false, true));

                    spawnDamageNumber(sw, self.getPos().add(0, self.getHeight() + 0.5, 0),
                            sinType, sinDamage);

                    applyBurstThorns(sw, attacker, self);
                }
            } else {
                PENDING_SIN_DAMAGE.set(0.0f);
            }
        } else {
            PENDING_SIN_DAMAGE.set(0.0f);
        }

        PENDING_PHYS_MULT.set(physResistance);
    }

    /**
     * 修改 applyDamage 的伤害参数（ModifyArg 注入）。
     * <p>
     * 将原始伤害乘以物理抗性乘数，再加上罪孽附加伤害。
     * 调用后清理所有 ThreadLocal 缓存。
     *
     * @param amount 原版计算的伤害值（已含护甲/保护等减伤）
     * @return 修改后的伤害值
     */
    @ModifyArg(method = "damage",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"),
            index = 1)
    private float addSinDamageToFinal(float amount) {
        Float mult = PENDING_PHYS_MULT.get();
        Float sinDamage = PENDING_SIN_DAMAGE.get();
        PENDING_SIN_DAMAGE.remove();
        PENDING_PHYS_MULT.remove();
        AttackTypeMapper.clearMobSinCache();
        return amount * (mult != null ? mult : 1.0f) + (sinDamage != null ? sinDamage : 0.0f);
    }

    /**
     * 计算护甲上的物理抗性附魔减伤。
     * <p>
     * 遍历 4 个护甲槽位，每个附魔等级提供 5% 减伤（乘算）：
     * 实际乘数 = ∏ (1 - 0.05 × level)。
     *
     * @param entity     目标实体
     * @param attackType 攻击类型
     * @return 护甲抗性乘数（≤ 1.0）
     */
    @Unique
    private float getArmorResistance(LivingEntity entity, AttackType attackType) {
        PhysicalResistanceEnchantment enchantment = ModEnchantments.getPhysicalEnchantment(attackType);
        if (enchantment == null) return 1.0f;

        float totalReduction = 1.0f;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack armor = entity.getEquippedStack(slot);
            int level = EnchantmentHelper.getLevel(enchantment, armor);
            if (level > 0) {
                totalReduction *= enchantment.getResistanceMultiplier(level);
            }
        }
        return totalReduction;
    }

    /**
     * 每 tick 触发实体抗性衰减（服务端）。
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;

        ResistanceManager.tickEntityResistance(self, self.getWorld().getTime());
    }

    /**
     * 持久化抗性和碎片数据到 NBT。
     */
    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity) {
            ResistanceProfile profile = ResistanceManager.getProfile(self);
            nbt.put("attack_type_resistance", profile.writeNbt(new NbtCompound()));
            SinFragmentData data = SinFragmentManager.getData((PlayerEntity) self);
            nbt.put("attack_type_fragments", data.writeNbt(new NbtCompound()));
        }
    }

    /**
     * 从 NBT 恢复抗性和碎片数据。
     */
    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof PlayerEntity) {
            if (nbt.contains("attack_type_resistance")) {
                ResistanceProfile profile = ResistanceProfile.readNbt(nbt.getCompound("attack_type_resistance"));
                ResistanceManager.setProfile(self, profile);
            }
            if (nbt.contains("attack_type_fragments")) {
                SinFragmentData data = new SinFragmentData();
                data.readNbt(nbt.getCompound("attack_type_fragments"));
                SinFragmentManager.setData(self.getUuid(), data);
            }
            if (self instanceof ServerPlayerEntity sp) {
                NetworkHandler.sendResistanceSync(sp);
                NetworkHandler.sendFragmentSync(sp);
            }
        }
    }

    /**
     * 实体死亡时清理抗性数据。
     */
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ResistanceManager.removeProfile(self.getUuid());
    }

    /**
     * 罪孽攻击粒子效果。
     * <p>
     * 每种罪孽使用不同颜色的 DustParticleEffect（RGB 粉尘粒子）：
     * <ul>
     *   <li>暴怒 WRATH — 红色 (1.0, 0.2, 0.2)</li>
     *   <li>色欲 LUST — 橙色 (1.0, 0.5, 0.0)</li>
     *   <li>怠惰 SLOTH — 黄色 (1.0, 0.9, 0.1)</li>
     *   <li>暴食 GLUTTONY — 草绿 (0.2, 0.8, 0.2)</li>
     *   <li>忧郁 GLOOM — 天蓝 (0.2, 0.7, 1.0)</li>
     *   <li>傲慢 PRIDE — 深蓝 (0.1, 0.2, 0.8)</li>
     *   <li>嫉妒 ENVY — 紫色 (0.6, 0.2, 1.0)</li>
     * </ul>
     * 粒子数量与大小随等级递增：
     * <ul>
     *   <li>Lv.1 — 8 粒子, size 0.6</li>
     *   <li>Lv.2 — 16 粒子, size 1.2</li>
     *   <li>Lv.3 — 24 粒子, size 1.8</li>
     * </ul>
     *
     * @param world    服务端世界
     * @param x        目标 X 坐标
     * @param y        目标 Y 坐标（身体中心）
     * @param z        目标 Z 坐标
     * @param sinType  罪孽类型
     * @param sinLevel 罪孽等级（1/2/3）
     */
    private static float applySinEffects(LivingEntity attacker, LivingEntity target, SinType sinType, float baseDamage) {
        float damage = baseDamage;

        SinCategoryEffect strengthen = ModStatusEffects.getSinEffect(sinType, EffectCategory.STRENGTHEN);
        if (strengthen != null) {
            StatusEffectInstance inst = attacker.getStatusEffect(strengthen);
            if (inst != null) damage *= (1.0f + 0.3f * (inst.getAmplifier() + 1));
        }

        SinCategoryEffect weaken = ModStatusEffects.getSinEffect(sinType, EffectCategory.WEAKEN);
        if (weaken != null) {
            StatusEffectInstance inst = attacker.getStatusEffect(weaken);
            if (inst != null) damage *= (1.0f - 0.3f * (inst.getAmplifier() + 1));
        }

        SinCategoryEffect guard = ModStatusEffects.getSinEffect(sinType, EffectCategory.GUARD);
        if (guard != null) {
            StatusEffectInstance inst = target.getStatusEffect(guard);
            if (inst != null) damage *= (1.0f - 0.3f * (inst.getAmplifier() + 1));
        }

        SinCategoryEffect vulnerable = ModStatusEffects.getSinEffect(sinType, EffectCategory.VULNERABLE);
        if (vulnerable != null) {
            StatusEffectInstance inst = target.getStatusEffect(vulnerable);
            if (inst != null) damage *= (1.0f + 0.3f * (inst.getAmplifier() + 1));
        }

        SinCategoryEffect boost = ModStatusEffects.getSinEffect(sinType, EffectCategory.BOOST);
        if (boost != null) {
            StatusEffectInstance inst = attacker.getStatusEffect(boost);
            if (inst != null) damage += 2.0f * (inst.getAmplifier() + 1);
        }

        SinCategoryEffect reduce = ModStatusEffects.getSinEffect(sinType, EffectCategory.REDUCE);
        if (reduce != null) {
            StatusEffectInstance inst = target.getStatusEffect(reduce);
            if (inst != null) damage -= 2.0f * (inst.getAmplifier() + 1);
        }

        return Math.max(0.0f, damage);
    }

    @Unique
    private static float applyPhysEffects(LivingEntity attacker, LivingEntity target, SinType sinType, float physResistance) {
        if (sinType != null) {
            BurstEffect burst = ModStatusEffects.getBurstEffect(sinType);
            if (burst != null) {
                StatusEffectInstance inst = target.getStatusEffect(burst);
                if (inst != null) {
                    physResistance *= (1.0f - 0.5f * (inst.getAmplifier() + 1));
                }
            }
        }

        StatusEffectInstance ignoreInst = attacker.getStatusEffect(ModStatusEffects.IGNORE_RESISTANCE);
        if (ignoreInst != null) {
            float ignoreRatio = 0.25f * (ignoreInst.getAmplifier() + 1);
            physResistance = physResistance * (1.0f - ignoreRatio) + ignoreRatio;
        }

        return Math.max(0.0f, physResistance);
    }

    @Unique
    private static void applyBurstThorns(ServerWorld world, LivingEntity attacker, LivingEntity target) {
        for (SinType sin : SinType.values()) {
            BurstEffect burst = ModStatusEffects.getBurstEffect(sin);
            if (burst == null) continue;
            StatusEffectInstance inst = target.getStatusEffect(burst);
            if (inst == null) continue;

            int amplifier = inst.getAmplifier();
            float thornsDamage = 3.0f * (amplifier + 1);

            attacker.damage(world.getDamageSources().magic(), thornsDamage);

            DustParticleEffect particle = new DustParticleEffect(
                    new Vector3f((sin.getColor() >> 16) & 0xFF, (sin.getColor() >> 8) & 0xFF, sin.getColor() & 0xFF),
                    1.5f);
            for (int i = 0; i < 10; i++) {
                world.spawnParticles(particle,
                        attacker.getX() + (world.random.nextDouble() - 0.5) * 0.5,
                        attacker.getY() + attacker.getHeight() / 2 + (world.random.nextDouble() - 0.5) * 0.5,
                        attacker.getZ() + (world.random.nextDouble() - 0.5) * 0.5,
                        1, 0, 0, 0, 0);
            }
        }
    }

    @Unique
    private static void spawnSinParticles(ServerWorld world, double x, double y, double z, SinType sinType, int level) {
        Vector3f color = switch (sinType) {
            case WRATH -> new Vector3f(1.0f, 0.2f, 0.2f);     // 红
            case LUST -> new Vector3f(1.0f, 0.5f, 0.0f);       // 橙
            case SLOTH -> new Vector3f(1.0f, 0.9f, 0.1f);      // 黄
            case GLUTTONY -> new Vector3f(0.2f, 0.8f, 0.2f);   // 草绿
            case GLOOM -> new Vector3f(0.2f, 0.7f, 1.0f);      // 天蓝
            case PRIDE -> new Vector3f(0.1f, 0.2f, 0.8f);      // 深蓝
            case ENVY -> new Vector3f(0.6f, 0.2f, 1.0f);       // 紫
        };
        int particleCount = 4 + level * 4;
        float particleSize = 0.3f + level * 0.3f;
        float spread = 0.5f + level * 0.2f;
        DustParticleEffect effect = new DustParticleEffect(color, particleSize);
        for (int i = 0; i < particleCount; i++) {
            double ox = x + (world.random.nextDouble() - 0.5) * spread;
            double oy = y + (world.random.nextDouble() - 0.5) * spread;
            double oz = z + (world.random.nextDouble() - 0.5) * spread;
            world.spawnParticles(effect, ox, oy, oz, 1, 0, 0, 0, 0);
        }
    }

    /**
     * 生成浮动伤害数字。
     * <p>
     * 使用 {@link AreaEffectCloudEntity} + 自定义名称在目标头顶显示罪孽伤害数值，
     * 文字颜色与罪孽类型对应，1 秒后自动消失。
     */
    @Unique
    private static void spawnDamageNumber(ServerWorld world, Vec3d pos, SinType sinType, float damage) {
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, pos.x, pos.y, pos.z);
        int colorRgb = switch (sinType) {
            case WRATH -> 0xFF3333;
            case LUST -> 0xFF8000;
            case SLOTH -> 0xFFE600;
            case GLUTTONY -> 0x33CC33;
            case GLOOM -> 0x33B3FF;
            case PRIDE -> 0x1A33CC;
            case ENVY -> 0x9933FF;
        };
        cloud.setCustomName(Text.literal(String.format("%.1f", damage))
                .styled(style -> style.withColor(TextColor.fromRgb(colorRgb))));
        cloud.setCustomNameVisible(true);
        cloud.setDuration(20);
        cloud.setRadius(0.0f);
        cloud.setNoGravity(true);
        world.spawnEntity(cloud);
    }
}