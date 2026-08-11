package org.attack_type.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.attack_type.fragment.SinFragmentAcquisition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 怠惰特性 Mixin。
 * <p>
 * 允许玩家在任何时间反复睡觉，不受白天/夜晚限制。
 * 注入 {@link ServerPlayerEntity#trySleep(BlockPos)} 方法，绕过时间检查。
 * 注入 {@link ServerPlayerEntity#wakeUp(boolean, boolean)} 方法，检测睡眠完成。
 */
@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {

    /**
     * 修改 trySleep 方法，允许白天睡觉和反复睡觉。
     */
    @Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
    private void allowAlwaysSleep(BlockPos pos, CallbackInfoReturnable<net.minecraft.entity.player.PlayerEntity.SleepFailureReason> cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (!self.getWorld().isClient) {
            self.sleep(pos);
            self.setSpawnPoint(self.getWorld().getRegistryKey(), pos, 0.0f, false, true);
            cir.setReturnValue(null);
        }
    }

    /**
     * 检测玩家从睡眠中醒来，触发怠惰碎片 +5。
     */
    @Inject(method = "wakeUp", at = @At("RETURN"))
    private void onWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (!self.getWorld().isClient) {
            SinFragmentAcquisition.onPlayerWakeUp(self);
        }
    }
}