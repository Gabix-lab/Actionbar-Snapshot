package me.Gabix.renderscale;

import me.Gabix.renderscale.config.RenderScaleConfig;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL30;

public class LegacyFsrRenderer {
    private boolean failed = false;

    public boolean blit(Framebuffer input, Framebuffer output, RenderScaleConfig cfg) {
        if (input == null || output == null) return false;
        if (failed) return false;

        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        try {
            int filter = 9729;
            if (cfg != null && !cfg.getFilter()) filter = 9728;

            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, input.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, output.fbo);
            GL30.glBlitFramebuffer(
                    0, 0, input.viewportWidth, input.viewportHeight,
                    0, 0, output.viewportWidth, output.viewportHeight,
                    GL30.GL_COLOR_BUFFER_BIT, filter
            );
            return true;
        } catch (Throwable t) {
            failed = true;
            return false;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
        }
    }

    public boolean isFailed() { return failed; }

    public void close() { failed = false; }
}