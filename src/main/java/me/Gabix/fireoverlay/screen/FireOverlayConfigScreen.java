package me.Gabix.fireoverlay.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import me.Gabix.StorageConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@SuppressWarnings("deprecation")
public class FireOverlayConfigScreen extends Screen {
    private static final Identifier FIRE_SPRITE_ID = new Identifier("minecraft", "block/fire_0");

    private final StorageConfig config;
    private final Screen parent;

    private static final int SLIDER_WIDTH = 20;
    private static final int SLIDER_HEIGHT = 150;
    private static final int SLIDER_Y = 60;
    private int sliderX;

    private boolean dragging = false;

    public FireOverlayConfigScreen(StorageConfig config, Screen parent) {
        super(Text.literal("Tűz Overlay Magasság"));
        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.sliderX = this.width / 2 - SLIDER_WIDTH / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Kész"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(this.width / 2 - 40, SLIDER_Y + SLIDER_HEIGHT + 20, 80, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= sliderX - 4 && mouseX <= sliderX + SLIDER_WIDTH + 4 &&
                    mouseY >= SLIDER_Y - 4 && mouseY <= SLIDER_Y + SLIDER_HEIGHT + 4) {
                dragging = true;
                updateValueFromMouse(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            updateValueFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateValueFromMouse(double mouseY) {
        double clampedY = Math.max(SLIDER_Y, Math.min(SLIDER_Y + SLIDER_HEIGHT, mouseY));
        double fraction = (clampedY - SLIDER_Y) / SLIDER_HEIGHT;
        int newValue = (int) Math.round((1.0 - fraction) * 100);
        config.fireOverlayHeight = Math.max(0, Math.min(100, newValue));
        config.save();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        // --- PONTOS MINECRAFT ÉGÉS OVERLAY (NEM TORZÍTÓ ELTOLÁSSAL) ---
        if (config.fireOverlayHeight > 0 && this.client != null) {
            Sprite fireSprite = MinecraftClient.getInstance()
                    .getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                    .apply(FIRE_SPRITE_ID);

            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.9F);
            RenderSystem.enableBlend();

            matrices.push();

            float heightPercent = config.fireOverlayHeight / 100.0F;

            // Fix textúra magasság (a képernyő 1.3-szorosa, hogy 100%-nál bőven felérjen)
            int fireWidth = (int) (this.width * 0.6F);
            int fireHeight = (int) (this.height * 1.3F);

            // Az Y pozíció eltolása:
            // 100%-nál a képernyő tetejéig ér (renderY = this.height - fireHeight)
            // 30%-nál pontosan a hotbar környékén bukkan csak ki alulról
            int maxOffset = fireHeight;
            int renderY = this.height - (int) (maxOffset * heightPercent);

            // 1. Bal oldali tűz
            DrawableHelper.drawSprite(
                    matrices,
                    0, renderY,
                    0,
                    fireWidth, fireHeight,
                    fireSprite
            );

            // 2. Jobb oldali tűz (középen 1/5 átfedéssel)
            DrawableHelper.drawSprite(
                    matrices,
                    this.width - fireWidth, renderY,
                    0,
                    fireWidth, fireHeight,
                    fireSprite
            );

            matrices.pop();
            RenderSystem.disableBlend();
        }

        // --- CÍM ---
        drawCenteredTextWithShadow(matrices, this.textRenderer, "Tűz Overlay Magasság", this.width / 2, 20, 0xFFFFFF);

        // --- CSÚSZKA ---
        int panelX = sliderX - 30;
        int panelY = SLIDER_Y - 10;
        int panelW = SLIDER_WIDTH + 60;
        int panelH = SLIDER_HEIGHT + 20;
        fill(matrices, panelX + 2, panelY + 2, panelX + panelW + 2, panelY + panelH + 2, 0x55000000);
        fill(matrices, panelX, panelY, panelX + panelW, panelY + panelH, 0xCC2D2D2D);

        fill(matrices, sliderX, SLIDER_Y, sliderX + SLIDER_WIDTH, SLIDER_Y + SLIDER_HEIGHT, 0xFF111111);
        int filledHeight = (int) (config.fireOverlayHeight / 100.0 * SLIDER_HEIGHT);
        int filledY = SLIDER_Y + SLIDER_HEIGHT - filledHeight;
        if (filledHeight > 0) {
            fillGradient(matrices, sliderX + 1, filledY, sliderX + SLIDER_WIDTH - 1, SLIDER_Y + SLIDER_HEIGHT, 0xFFFF5500, 0xFFFF0000);
        }

        int handleY = SLIDER_Y + SLIDER_HEIGHT - filledHeight - 6;
        fill(matrices, sliderX - 6 + 1, handleY + 1, sliderX + SLIDER_WIDTH + 6 + 1, handleY + 12 + 1, 0x55000000);
        fill(matrices, sliderX - 6, handleY, sliderX + SLIDER_WIDTH + 6, handleY + 12, 0xFFFFFFFF);
        fill(matrices, sliderX - 5, handleY + 1, sliderX + SLIDER_WIDTH + 5, handleY + 11, 0xFF333333);
        String percentText = config.fireOverlayHeight + "%";
        int textWidth = this.textRenderer.getWidth(percentText);
        this.textRenderer.drawWithShadow(matrices, percentText, sliderX + SLIDER_WIDTH / 2 - textWidth / 2, handleY + 2, 0xFFFF5555);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}