package org.attack_type.mixin;

import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import org.attack_type.fragment.SinFragmentAcquisition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 动物繁殖 Mixin。
 * <p>
 * 检测动物繁殖成功事件，通知 {@link SinFragmentAcquisition#onAnimalBreed} 处理色欲碎片。
 * 每次繁殖成功 +1 色欲碎片。
 */
@Mixin(AnimalEntity.class)
public class MixinAnimalEntity {

    /**
     * 繁殖成功时触发色欲碎片。
     */
    @Inject(method = "breed", at = @At("HEAD"))
    private void onBreed(ServerWorld world, AnimalEntity other, CallbackInfo ci) {
        AnimalEntity self = (AnimalEntity) (Object) this;
        SinFragmentAcquisition.onAnimalBreed(world, self.getPos());
    }
}