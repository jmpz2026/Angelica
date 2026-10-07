package com.gtnewhorizons.angelica.rendering.celeritas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChunkShaderLocationPinTest {

    @Test
    void pinsOnlyTheNamedDeclaration() {
        final String src = "in uvec4 a_PosId;\nin vec4 a_Color;\n    out vec4 fragColor; // color\nout vec4 v_Color;\n";
        String out = AngelicaChunkRenderer.pinLocation(src, "a_Color", 1);
        out = AngelicaChunkRenderer.pinLocation(out, "fragColor", 0);
        assertEquals("in uvec4 a_PosId;\nlayout(location = 1) in vec4 a_Color;\n    layout(location = 0) out vec4 fragColor; // color\nout vec4 v_Color;\n", out);
    }

    @Test
    void leavesPrefixedNamesAlone() {
        final String src = "in vec4 a_ColorExtra;\n";
        assertEquals(src, AngelicaChunkRenderer.pinLocation(src, "a_Color", 1));
    }
}
