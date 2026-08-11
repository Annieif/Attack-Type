package org.attack_type.mixin;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家暴食特性 Mixin。
 * <p>
 * 允许满饱食度的玩家继续进食，配合暴食罪孽碎片获取。
 * 注入 {@link PlayerEntity#canConsume(boolean)} 方法，始终返回 true。
 */
@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    /**
     * 始终允许进食，忽略饱食度限制。
     */
    @Inject(method = "canConsume", at = @At("HEAD"), cancellable = true)
    private void alwaysAllowEating(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}