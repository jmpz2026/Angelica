package com.gtnewhorizons.angelica.glsm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LTWWorkaroundShaderTest {

    @Test
    void dropsTheInvariantPositionRedeclaration() {
        final String src = "#version 330 core\nout vec4 v_Color;\ninvariant gl_Position;\n  invariant  gl_Position ;\nvoid main() { gl_Position = vec4(0.0); }\n";
        assertEquals("#version 330 core\nout vec4 v_Color;\n\n\nvoid main() { gl_Position = vec4(0.0); }\n",
            LTWWorkaround.stripInvariantPosition(src).toString());
    }

    @Test
    void leavesOtherInvariantDeclarationsAlone() {
        final String src = "invariant out vec4 v_Color;\nvoid main() { gl_Position = vec4(0.0); }\n";
        assertEquals(src, LTWWorkaround.stripInvariantPosition(src).toString());
    }
}
