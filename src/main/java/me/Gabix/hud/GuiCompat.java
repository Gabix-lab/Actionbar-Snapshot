package me.Gabix.hud;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
//? if >=1.20 {
/*import net.minecraft.client.gui.DrawContext;
*///?} else {
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
//?}

/**
 * Small version bridge for the GUI drawing API.
 * 1.19.4 takes a MatrixStack, while 1.20+ takes a DrawContext.
 * Only one overload of each method exists per Minecraft version.
 */
public final class GuiCompat {
    private GuiCompat() {
    }

    //? if >=1.20 {
    /*public static void fill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    public static void drawText(TextRenderer textRenderer, DrawContext context, String text, int x, int y, int color) {
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void drawText(TextRenderer textRenderer, DrawContext context, Text text, int x, int y, int color) {
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void drawCenteredText(TextRenderer textRenderer, DrawContext context, String text, int x, int y, int color) {
        context.drawCenteredTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void drawCenteredText(TextRenderer textRenderer, DrawContext context, Text text, int x, int y, int color) {
        context.drawCenteredTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void renderWidget(ClickableWidget widget, DrawContext context, int mouseX, int mouseY, float delta) {
        widget.render(context, mouseX, mouseY, delta);
    }
    *///?} else {
    public static void fill(MatrixStack matrices, int x1, int y1, int x2, int y2, int color) {
        DrawableHelper.fill(matrices, x1, y1, x2, y2, color);
    }

    public static void drawText(TextRenderer textRenderer, MatrixStack matrices, String text, int x, int y, int color) {
        textRenderer.drawWithShadow(matrices, text, x, y, color);
    }

    public static void drawText(TextRenderer textRenderer, MatrixStack matrices, Text text, int x, int y, int color) {
        textRenderer.drawWithShadow(matrices, text, x, y, color);
    }

    public static void drawCenteredText(TextRenderer textRenderer, MatrixStack matrices, String text, int x, int y, int color) {
        DrawableHelper.drawCenteredTextWithShadow(matrices, textRenderer, text, x, y, color);
    }

    public static void drawCenteredText(TextRenderer textRenderer, MatrixStack matrices, Text text, int x, int y, int color) {
        DrawableHelper.drawCenteredTextWithShadow(matrices, textRenderer, text, x, y, color);
    }

    public static void renderWidget(ClickableWidget widget, MatrixStack matrices, int mouseX, int mouseY, float delta) {
        widget.render(matrices, mouseX, mouseY, delta);
    }
    //?}
}
