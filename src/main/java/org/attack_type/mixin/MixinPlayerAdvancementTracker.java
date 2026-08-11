package org.attack_type.mixin;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.attack_type.fragment.SinFragmentAcquisition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 成就/进度达成 Mixin。
 * <p>
 * 检测玩家达成成就/进度事件，通知 {@link SinFragmentAcquisition#onAdvancement} 处理傲慢碎片。
 * 每个成就/进度 +10 傲慢碎片。
 */
@Mixin(PlayerAdvancementTracker.class)
public class MixinPlayerAdvancementTracker {

    @Shadow
    private ServerPlayerEntity owner;

    /**
     * 成就/进度达成时触发傲慢碎片。
     */
    @Inject(method = "grantCriterion", at = @At("HEAD"))
    private void onGrantCriterion(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (owner != null) {
            SinFragmentAcquisition.onAdvancement(owner);
        }
    }
}