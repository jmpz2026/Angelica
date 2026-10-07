package com.gtnewhorizons.angelica.mixins.early.angelica;

import com.gtnewhorizons.angelica.glsm.hooks.FrameHooks;
import com.gtnewhorizons.angelica.glsm.hooks.LtwFrameProbe;
import net.minecraft.client.shader.Framebuffer;
import com.gtnewhorizons.angelica.rendering.GlintClock;
import com.gtnewhorizons.angelica.rendering.culling.GpuCulling;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks frame boundaries and shutdown in Minecraft to notify the active render backend.
 *
 * Frame end: at the start of func_147120_f[resetSize] (before Display.update) -- universal, covers
 *            all callers including LoadingScreenRenderer, drawSplashScreen, toggleFullscreen.
 * Frame begin: at the end of func_147120_f (after Display.update), but only once the game loop has started.
 * Bootstrap: one-shot at the start of the first runGameLoop call to start the first frame.
 * Shutdown: at the start of shutdownMinecraftApplet (cleanup GPU resources).
 */
@Mixin(Minecraft.class)
public class MixinMinecraft_FrameHook {

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void angelica$bootstrapFirstFrame(CallbackInfo ci) {
        FrameHooks.bootstrapFirstFrame();
    }

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void angelica$beginGlintFrame(CallbackInfo ci) {
        GlintClock.beginFrame(Minecraft.getSystemTime());
    }

    @Inject(method = "func_147120_f"/*resetSize*/, at = @At("HEAD"))
    private void angelica$onFrameEnd(CallbackInfo ci) {
        final Minecraft mc = (Minecraft) (Object) this;
        final Framebuffer main = mc.getFramebuffer();
        final Object screen = mc.currentScreen;
        LtwFrameProbe.onFrameEnd(main != null ? main.framebufferObject : 0, mc.displayWidth, mc.displayHeight,
            screen == null ? "none" : screen.getClass().getSimpleName(),
            () -> angelica$describeScreen(mc, screen));
        FrameHooks.frameEnd();
    }

    @Inject(method = "func_147120_f"/*resetSize*/, at = @At("RETURN"))
    private void angelica$onFrameBegin(CallbackInfo ci) {
        FrameHooks.frameBegin();
    }

    /** Primitive fields of the open screen's own class, for the LTW black-screen probe. */
    @Unique
    private static String angelica$describeScreen(Minecraft mc, Object screen) {
        final StringBuilder sb = new StringBuilder("world=").append(mc.theWorld != null)
            .append(" inGameHasFocus=").append(mc.inGameHasFocus).append(" skipRenderWorld=").append(mc.skipRenderWorld);
        if (screen == null) return sb.toString();
        for (java.lang.reflect.Field f : screen.getClass().getDeclaredFields()) {
            if (!f.getType().isPrimitive()) continue;
            try {
                f.setAccessible(true);
                sb.append(' ').append(f.getName()).append('=').append(f.get(java.lang.reflect.Modifier.isStatic(f.getModifiers()) ? null : screen));
            } catch (Throwable ignored) {
            }
        }
        return sb.toString();
    }

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void angelica$onShutdown(CallbackInfo ci) {
        GpuCulling.shutdown();
        FrameHooks.shutdown();
    }
}
