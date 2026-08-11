package org.attack_type.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.server.world.ServerWorld;
import org.attack_type.fragment.SinFragmentAcquisition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 僵尸转化 Mixin。
 * <p>
 * 检测僵尸转化为溺尸的事件，通知 {@link SinFragmentAcquisition#onZombieConvertToDrowned} 处理色欲碎片。
 * 每次转化 +5 色欲碎片。
 */
@Mixin(ZombieEntity.class)
public class MixinZombieEntity {

    /**
     * 僵尸转化为溺尸时触发色欲碎片。
     */
    @Inject(method = "convertTo", at = @At("RETURN"))
    private void onConvertTo(EntityType<?> entityType, CallbackInfo ci) {
        ZombieEntity self = (ZombieEntity) (Object) this;
        if (entityType == EntityType.DROWNED && self.getWorld() instanceof ServerWorld world) {
            SinFragmentAcquisition.onZombieConvertToDrowned(world, self.getPos());
        }
    }
}