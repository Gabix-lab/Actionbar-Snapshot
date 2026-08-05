package me.Gabix;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class FireOverlayConfigScreen extends Screen {
    private final StorageConfig config;
    private final Screen parent;

    private static final int SLIDER_WIDTH = 20;
    private static final int SLIDER_HEIGHT = 150;
    private static final int SLIDER_Y = 60;
    private int sliderX;

    private boolean dragging = false;
    private int animTick = 0;

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
    public void tick() {
        animTick++;
        if (animTick > 1000) animTick = 0;
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

        // --- TŰZ ELŐNÉZET (stabil, animált átmenet) ---
        int fireHeight = (int) (this.height * (config.fireOverlayHeight / 100.0));
        if (fireHeight > 0) {
            // Animált színátmenet a tűz illúziójához
            int alpha = 200 + (int)(Math.sin(animTick * 0.1) * 55); // pulzáló átlátszóság
            int colorTop = (alpha << 24) | 0xFF4500; // Narancsvörös
            int colorBottom = (alpha << 24) | 0xFFD700; // Arany
            fillGradient(matrices, 0, this.height - fireHeight, this.width, this.height, colorTop, colorBottom);
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