package com.gtnewhorizons.angelica.mixins.early.angelica;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On Android {@code Minecraft.getSystemTime()} is not trustworthy: the launcher's clock and even
 * {@code System.nanoTime()} of the bundled JVM can read tens of seconds ahead for a single call and then come back.
 * The tick timer and the GUI animations of every mod are built on it, so there it is replaced by a steady clock built
 * from clamped steps: steps that go backwards or jump more than {@link #angelica$MAX_STEP_MS} are dropped. Same epoch
 * origin as the vanilla value. Desktop keeps the vanilla clock.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft_MonotonicClock {

    @Unique
    private static final boolean angelica$ANDROID = System.getProperty("os.version", "").contains("Android")
        || System.getProperty("java.vendor", "").contains("Android");
    @Unique
    private static final long angelica$MAX_STEP_MS = 2000L;
    @Unique
    private static long angelica$steadyMillis = System.currentTimeMillis();
    @Unique
    private static long angelica$lastRawMillis = System.nanoTime() / 1_000_000L;

    @Inject(method = "getSystemTime", at = @At("HEAD"), cancellable = true)
    private static void angelica$monotonicSystemTime(CallbackInfoReturnable<Long> cir) {
        if (angelica$ANDROID) {
            cir.setReturnValue(angelica$steadyNow());
        }
    }

    @Unique
    private static synchronized long angelica$steadyNow() {
        final long raw = System.nanoTime() / 1_000_000L;
        final long step = raw - angelica$lastRawMillis;
        if (step > 0 && step <= angelica$MAX_STEP_MS) {
            angelica$steadyMillis += step;
        }
        angelica$lastRawMillis = raw;
        return angelica$steadyMillis;
    }
}
