package com.gtnewhorizons.angelica.glsm;

import com.gtnewhorizons.angelica.glsm.states.PixelStoreState;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import static com.gtnewhorizons.angelica.glsm.backend.BackendManager.RENDER_BACKEND;

public final class LTWWorkaround {

    private LTWWorkaround() {}

    private static ByteBuffer oneTexel;

    public static boolean isLtwVersionString(String glVersion) {
        return glVersion != null && glVersion.contains("LTW");
    }

    private static final Pattern INVARIANT_POSITION = Pattern.compile("(?m)^[ \t]*invariant[ \t]+gl_Position[ \t]*;");

    /**
     * LTW's GLSL translator writes {@code layout(location=N)} in front of every variable with an explicit location,
     * builtins included, and computes N from the vertex attribute base. A redeclared {@code invariant gl_Position;}
     * comes out as {@code layout(location=-15) invariant gl_Position;}, which Mali and Adreno reject, so the shader
     * fails. Without the redeclaration LTW does not print gl_Position at all. Only position invariance between passes
     * is lost.
     */
    public static CharSequence stripInvariantPosition(CharSequence source) {
        final String src = source.toString();
        if (!src.contains("gl_Position")) return source;
        return INVARIANT_POSITION.matcher(src).replaceAll("");
    }

    public static void onTexImage2D(int target, int level, int format, boolean hasPixels) {
        if (target != GL11.GL_TEXTURE_2D || level != 0 || format != GL12.GL_BGRA || hasPixels) return;
        if (oneTexel == null) {
            oneTexel = BufferUtils.createByteBuffer(4);
        }
        oneTexel.clear();
        GLStateManager.forcePixelUnpackState(PixelStoreState.DEFAULT);
        GLStateManager.suspendPixelUnpackBuffer();
        RENDER_BACKEND.texSubImage2D(target, level, 0, 0, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, oneTexel);
        GLStateManager.restorePixelUnpackBuffer();
        GLStateManager.restorePixelUnpackState(PixelStoreState.DEFAULT);
    }
}
