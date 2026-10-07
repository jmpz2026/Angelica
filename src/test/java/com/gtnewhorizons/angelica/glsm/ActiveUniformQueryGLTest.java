package com.gtnewhorizons.angelica.glsm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

@GLCoreTest
class ActiveUniformQueryGLTest {

    private static int compile(int type, String src) {
        final int s = GLStateManager.glCreateShader(type);
        GLStateManager.glShaderSource(s, src);
        GLStateManager.glCompileShader(s);
        assertEquals(GL11.GL_TRUE, GLStateManager.glGetShaderi(s, GL20.GL_COMPILE_STATUS), () -> GLStateManager.glGetShaderInfoLog(s, 4096));
        return s;
    }

    @Test
    void oneSizeTypeBufferServesEveryUniform() {
        final int vs = compile(GL20.GL_VERTEX_SHADER, "#version 330 core\nuniform mat4 u_Mvp;\nuniform float u_Scale;\nin vec3 a_Pos;\n"
            + "void main() { gl_Position = u_Mvp * vec4(a_Pos * u_Scale, 1.0); }\n");
        final int fs = compile(GL20.GL_FRAGMENT_SHADER, "#version 330 core\nuniform vec4 u_Color;\nuniform sampler2D u_Tex;\nout vec4 fragColor;\n"
            + "void main() { fragColor = u_Color * texture(u_Tex, vec2(0.5)); }\n");
        final int program = GLStateManager.glCreateProgram();
        try {
            GLStateManager.glAttachShader(program, vs);
            GLStateManager.glAttachShader(program, fs);
            GLStateManager.glLinkProgram(program);
            final int count = GLStateManager.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS);
            assertEquals(4, count);
            final IntBuffer sizeType = BufferUtils.createIntBuffer(2);
            final Map<String, Integer> types = new HashMap<>();
            for (int i = 0; i < count; i++) {
                final String name = RenderSystem.getActiveUniform(program, i, 128, sizeType);
                assertEquals(1, sizeType.get(0));
                types.put(name, sizeType.get(1));
            }
            assertEquals(0, sizeType.position());
            assertEquals(GL20.GL_FLOAT_MAT4, types.get("u_Mvp"));
            assertEquals(GL11.GL_FLOAT, types.get("u_Scale"));
            assertEquals(GL20.GL_FLOAT_VEC4, types.get("u_Color"));
            assertEquals(GL20.GL_SAMPLER_2D, types.get("u_Tex"));
        } finally {
            GLStateManager.glDeleteProgram(program);
            GLStateManager.glDeleteShader(vs);
            GLStateManager.glDeleteShader(fs);
        }
    }
}
