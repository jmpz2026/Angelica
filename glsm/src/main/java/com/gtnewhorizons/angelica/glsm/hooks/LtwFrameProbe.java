package com.gtnewhorizons.angelica.glsm.hooks;

import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.glsm.RenderSystem;
import com.gtnewhorizons.angelica.glsm.states.ColorMask;
import com.gtnewhorizons.angelica.glsm.states.ViewportState;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Diagnostic for Android (LTW only): the screen goes black at random on some phones and comes back later, with no GL
 * error logged. Every few frames, at frame end, a few pixels of the window are sampled; when they stay black, the real
 * GL state (as the driver sees it, this package is not redirected) and GLSM's cached state are logged once, and again
 * when the picture comes back, so the two can be compared.
 */
public final class LtwFrameProbe {

    private static final int SAMPLE_EVERY = 10;
    private static final int BLACK_SAMPLES_TO_REPORT = 3;
    private static final int MAX_REPORTS = 6;

    private static final ByteBuffer PIXEL = BufferUtils.createByteBuffer(4);
    private static final IntBuffer INTS = BufferUtils.createIntBuffer(16);

    private static long frame;
    private static int blackSamples;
    private static boolean reportedBlack;
    private static int reports;
    private static String lastScreen = "none";

    private LtwFrameProbe() {}

    public static void onFrameEnd(int mainFbo, int width, int height, String screen) {
        onFrameEnd(mainFbo, width, height, screen, null);
    }

    /** {@code screenState} describes the open screen's fields; only evaluated when a report is written. */
    public static void onFrameEnd(int mainFbo, int width, int height, String screen, java.util.function.Supplier<String> screenState) {
        if (reports >= MAX_REPORTS || !RenderSystem.isLTW()) return;
        if (!screen.equals(lastScreen)) {
            GLStateManager.LOGGER.info("[LtwProbe] screen {} -> {}", lastScreen, screen);
            lastScreen = screen;
        }
        if (++frame % SAMPLE_EVERY != 0 || width <= 0 || height <= 0) return;
        try {
            final boolean black = isBlack(width / 2, height / 2) && isBlack(width / 4, height / 4)
                && isBlack(width * 3 / 4, height * 3 / 4);
            if (black) {
                if (++blackSamples == BLACK_SAMPLES_TO_REPORT && !reportedBlack) {
                    reportedBlack = true;
                    report("window black for ~" + (BLACK_SAMPLES_TO_REPORT * SAMPLE_EVERY) + " frames", mainFbo, width, height, screen, screenState);
                }
            } else {
                if (reportedBlack) {
                    report("window back after ~" + (blackSamples * SAMPLE_EVERY) + " frames", mainFbo, width, height, screen, screenState);
                    reportedBlack = false;
                }
                blackSamples = 0;
            }
        } catch (Throwable t) {
            reports = MAX_REPORTS;
            GLStateManager.LOGGER.warn("[LtwProbe] disabled", t);
        }
    }

    private static boolean isBlack(int x, int y) {
        PIXEL.clear();
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXEL);
        return (PIXEL.get(0) | PIXEL.get(1) | PIXEL.get(2)) == 0;
    }

    private static String pixel(int x, int y) {
        PIXEL.clear();
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXEL);
        return String.format("#%02x%02x%02x%02x", PIXEL.get(0) & 0xFF, PIXEL.get(1) & 0xFF, PIXEL.get(2) & 0xFF, PIXEL.get(3) & 0xFF);
    }

    private static int getInt(int pname) {
        return GL11.glGetInteger(pname);
    }

    private static String ints(int pname, int count) {
        INTS.clear();
        GL11.glGetInteger(pname, INTS);
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; i++) sb.append(i == 0 ? "" : ",").append(INTS.get(i));
        return sb.append(']').toString();
    }

    private static void report(String what, int mainFbo, int width, int height, String screen, java.util.function.Supplier<String> screenState) {
        reports++;
        final StringBuilder sb = new StringBuilder();
        sb.append("[LtwProbe] ").append(what).append(" | screen=").append(screen).append(" | window ").append(width).append('x').append(height);
        final int drawFb = getInt(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        final int readFb = getInt(GL30.GL_READ_FRAMEBUFFER_BINDING);
        sb.append("\n  real: drawFb=").append(drawFb).append(" readFb=").append(readFb)
            .append(" viewport=").append(ints(GL11.GL_VIEWPORT, 4))
            .append(" scissor=").append(GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)).append(ints(GL11.GL_SCISSOR_BOX, 4))
            .append(" colorMask=").append(ints(GL11.GL_COLOR_WRITEMASK, 4))
            .append(" blend=").append(GL11.glIsEnabled(GL11.GL_BLEND))
            .append(" depthTest=").append(GL11.glIsEnabled(GL11.GL_DEPTH_TEST))
            .append(" program=").append(getInt(GL20.GL_CURRENT_PROGRAM))
            .append(" drawBuffer0=0x").append(Integer.toHexString(getInt(GL20.GL_DRAW_BUFFER0)));
        final ViewportState vp = GLStateManager.getViewportState();
        final ColorMask cm = GLStateManager.getColorMask();
        sb.append("\n  glsm: drawFb=").append(GLStateManager.getDrawFramebuffer())
            .append(" viewport=[").append(vp.x).append(',').append(vp.y).append(',').append(vp.width).append(',').append(vp.height).append(']')
            .append(" scissor=").append(GLStateManager.getScissorTest().isEnabled())
            .append(" colorMask=[").append(cm.red).append(',').append(cm.green).append(',').append(cm.blue).append(',').append(cm.alpha).append(']');
        if (mainFbo > 0) {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainFbo);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFbo);
            final int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
            final String drawBuffers = "0x" + Integer.toHexString(getInt(GL20.GL_DRAW_BUFFER0)) + ",0x"
                + Integer.toHexString(getInt(GL20.GL_DRAW_BUFFER1));
            final int attach0 = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
            final String mainPixels = pixel(width / 2, height / 2) + " " + pixel(width / 4, height / 4) + " " + pixel(width * 3 / 4, height * 3 / 4);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFb);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFb);
            sb.append("\n  mainFb ").append(mainFbo).append(": status=0x").append(Integer.toHexString(status))
                .append(" drawBuffers=").append(drawBuffers).append(" colorAttachment0=").append(attach0)
                .append(" pixels(center,low-left,up-right)=").append(mainPixels);
        }
        if (screenState != null) {
            String state;
            try {
                state = screenState.get();
            } catch (Throwable t) {
                state = "unavailable: " + t;
            }
            sb.append("\n  screen state: ").append(state);
        }
        GLStateManager.LOGGER.warn(sb.toString());
    }
}
