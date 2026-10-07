package com.gtnewhorizons.angelica.glsm.backend;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.IntBuffer;

/**
 * LWJGL 3's {@code GL32C}, called by method handle. Only used when Celeritas runs its LWJGL 3 service: that service is
 * redirected into GLSM, so the backend cannot hand these calls back to it (sync objects would recurse, and the
 * multi-draw has no LWJGL 2 equivalent in the backend).
 */
final class Lwjgl3Direct {
    static final boolean ACTIVE = LWJGL.getClass().getName().endsWith(".LWJGL3Service");

    private static final MethodHandle FENCE_SYNC;
    private static final MethodHandle CLIENT_WAIT_SYNC;
    private static final MethodHandle DELETE_SYNC;
    private static final MethodHandle WAIT_SYNC;
    private static final MethodHandle GET_SYNCI;
    private static final MethodHandle MULTI_DRAW_ELEMENTS_BASE_VERTEX;

    static {
        MethodHandle fence = null, clientWait = null, delete = null, wait = null, getSynci = null, multiDraw = null;
        if (ACTIVE) {
            try {
                final Class<?> gl32c = Class.forName("org.lwjgl.opengl.GL32C");
                final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                fence = lookup.findStatic(gl32c, "glFenceSync", MethodType.methodType(long.class, int.class, int.class));
                clientWait = lookup.findStatic(gl32c, "glClientWaitSync", MethodType.methodType(int.class, long.class, int.class, long.class));
                delete = lookup.findStatic(gl32c, "glDeleteSync", MethodType.methodType(void.class, long.class));
                wait = lookup.findStatic(gl32c, "glWaitSync", MethodType.methodType(void.class, long.class, int.class, long.class));
                getSynci = lookup.findStatic(gl32c, "glGetSynci", MethodType.methodType(int.class, long.class, int.class, IntBuffer.class));
                multiDraw = lookup.findStatic(gl32c, "nglMultiDrawElementsBaseVertex",
                    MethodType.methodType(void.class, int.class, long.class, int.class, long.class, int.class, long.class));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("LWJGL 3 service without the expected GL32C functions", e);
            }
        }
        FENCE_SYNC = fence;
        CLIENT_WAIT_SYNC = clientWait;
        DELETE_SYNC = delete;
        WAIT_SYNC = wait;
        GET_SYNCI = getSynci;
        MULTI_DRAW_ELEMENTS_BASE_VERTEX = multiDraw;
    }

    private Lwjgl3Direct() {}

    static long fenceSync(int condition, int flags) {
        try {
            return (long) FENCE_SYNC.invokeExact(condition, flags);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    static int clientWaitSync(long sync, int flags, long timeout) {
        try {
            return (int) CLIENT_WAIT_SYNC.invokeExact(sync, flags, timeout);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    static void deleteSync(long sync) {
        try {
            DELETE_SYNC.invokeExact(sync);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    static void waitSync(long sync, int flags, long timeout) {
        try {
            WAIT_SYNC.invokeExact(sync, flags, timeout);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    static int getSynci(long sync, int pname, IntBuffer length) {
        try {
            return (int) GET_SYNCI.invokeExact(sync, pname, length);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    static void multiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int drawcount, long pBaseVertex) {
        try {
            MULTI_DRAW_ELEMENTS_BASE_VERTEX.invokeExact(mode, pCount, type, pIndices, drawcount, pBaseVertex);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) return (RuntimeException) t;
        if (t instanceof Error) throw (Error) t;
        return new IllegalStateException(t);
    }
}
