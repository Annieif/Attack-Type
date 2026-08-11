package org.attack_type.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.attack_type.api.AttackType;
import org.attack_type.api.AttackTypeMapper;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.component.ResistanceManager;
import org.attack_type.enchantment.ModEnchantments;
import org.attack_type.enchantment.PhysicalResistanceEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Unique
    private static final ThreadLocal<Float> PENDING_PHYS_MULT = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<Float> PENDING_SIN_DAMAGE = new ThreadLocal<>();

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
            SinType sinType = AttackTypeMapper.getSinType(attacker);
            if (sinType != null) {
                int sinLevel = EnchantmentHelper.getLevel(
                        ModEnchantments.getSinEnchantment(sinType),
                        attacker.getMainHandStack());
                float sinDamage = (sinLevel * 3.0f + 1.0f) * profile.getSinResistance(sinType);
                PENDING_SIN_DAMAGE.set(sinDamage);
            } else {
                PENDING_SIN_DAMAGE.set(0.0f);
            }
        } else {
            PENDING_SIN_DAMAGE.set(0.0f);
        }

        PENDING_PHYS_MULT.set(physResistance);
    }

    @ModifyArg(method = "damage",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;applyArmorToDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"),
            index = 1)
    private float modifyArmorDamageArg(float amount) {
        Float mult = PENDING_PHYS_MULT.get();
        return amount * (mult != null ? mult : 1.0f);
    }

    @ModifyArg(method = "damage",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/entity/damage/DamageSource;F)V"),
            index = 1)
    private float addSinDamageToFinal(float amount) {
        Float sinDamage = PENDING_SIN_DAMAGE.get();
        PENDING_SIN_DAMAGE.remove();
        PENDING_PHYS_MULT.remove();
        return amount + (sinDamage != null ? sinDamage : 0.0f);
    }

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

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;

        ResistanceManager.tickEntityResistance(self, self.getWorld().getTime());
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ResistanceProfile profile = ResistanceManager.getProfile(self);
        nbt.put("attack_type_resistance", profile.writeNbt(new NbtCompound()));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (nbt.contains("attack_type_resistance")) {
            ResistanceProfile profile = ResistanceProfile.readNbt(nbt.getCompound("attack_type_resistance"));
            ResistanceManager.setProfile(self, profile);
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        ResistanceManager.removeProfile(self.getUuid());
    }
}