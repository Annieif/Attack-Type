package org.attack_type.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import org.attack_type.fragment.SinFragmentAcquisition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 闪电击中实体 Mixin。
 * <p>
 * 检测实体被闪电击中事件，通知 {@link SinFragmentAcquisition#onLightningStrike} 处理色欲碎片。
 * 变异的实体：猪、苦力怕、哞菇、村民。
 * <p>
 * 注意：{@code onStruckByLightning} 定义在 {@link Entity} 中，因此 Mixin 目标为 Entity 而非 LivingEntity。
 */
@Mixin(Entity.class)
public class MixinLightningStrike {

    /**
     * 实体被闪电击中时触发色欲碎片检测。
     */
    @Inject(method = "onStruckByLightning", at = @At("HEAD"))
    private void onLightningStrike(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        SinFragmentAcquisition.onLightningStrike(world, self);
    }
}