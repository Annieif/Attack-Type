package org.attack_type.mixin;

import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.attack_type.fragment.SinFragmentAcquisition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 僵尸村民治愈 Mixin。
 * <p>
 * 检测僵尸村民被治愈为村民的事件，通知 {@link SinFragmentAcquisition#onZombieVillagerCured} 处理色欲碎片。
 * 每次治愈 +10 色欲碎片。
 */
@Mixin(ZombieVillagerEntity.class)
public class MixinZombieVillagerEntity {

    /**
     * 僵尸村民治愈完成时触发色欲碎片。
     */
    @Inject(method = "finishConversion", at = @At("HEAD"))
    private void onFinishConversion(ServerWorld world, CallbackInfo ci) {
        ZombieVillagerEntity self = (ZombieVillagerEntity) (Object) this;
        SinFragmentAcquisition.onZombieVillagerCured(world, self.getPos());
    }
}