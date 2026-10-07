package com.gtnewhorizons.angelica.mixins.early.angelica;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On Android launchers {@code Minecraft.getSystemTime()} can come from the wall clock (the DBR mobile launcher read
 * {@code System.currentTimeMillis()} through lwjglx), which jumps whenever Android adjusts the time. The tick timer
 * and GUI animations of every mod are built on it, so there it is replaced by monotonic milliseconds with the same
 * epoch origin. Desktop keeps the vanilla clock.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft_MonotonicClock {

    @Unique
    private static final boolean angelica$ANDROID = System.getProperty("os.version", "").contains("Android")
        || System.getProperty("java.vendor", "").contains("Android");
    @Unique
    private static final long angelica$EPOCH_MILLIS = System.currentTimeMillis();
    @Unique
    private static final long angelica$EPOCH_NANOS = System.nanoTime();

    @Inject(method = "getSystemTime", at = @At("HEAD"), cancellable = true)
    private static void angelica$monotonicSystemTime(CallbackInfoReturnable<Long> cir) {
        if (angelica$ANDROID) {
            cir.setReturnValue(angelica$EPOCH_MILLIS + (System.nanoTime() - angelica$EPOCH_NANOS) / 1_000_000L);
        }
    }
}
