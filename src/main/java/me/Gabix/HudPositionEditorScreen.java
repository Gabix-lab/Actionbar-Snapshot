package me.Gabix;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HudPositionEditorScreen extends Screen {
    private final StorageConfig config;
    private final Screen parent;

    private DragTarget draggingTarget = null;
    private int draggedSlotIndex = -1;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private static final int SNAP_THRESHOLD = 5;

    private enum DragTarget {
        MANA, DAMAGE, SLOT
    }

    private static class BoxBounds {
        int x, y, w, h;
        DragTarget target;
        int slotIndex;

        BoxBounds(int x, int y, int w, int h, DragTarget target, int slotIndex) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.target = target;
            this.slotIndex = slotIndex;
        }
    }

    public HudPositionEditorScreen(StorageConfig config, Screen parent) {
        super(Text.literal("HUD Szerkesztő"));
        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Kész & Mentés"), button -> {
            this.config.save();
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(this.width / 2 - 75, this.height - 28, 150, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int manaW = (int) (80 * config.manaScale);
            int manaH = (int) (16 * config.manaScale);
            if (isHovered(config.manaX, config.manaY, manaW, manaH, mouseX, mouseY)) {
                draggingTarget = DragTarget.MANA;
                dragOffsetX = (int) mouseX - config.manaX;
                dragOffsetY = (int) mouseY - config.manaY;
                return true;
            }

            int dmgW = (int) (80 * config.damageScale);
            int dmgH = (int) (16 * config.damageScale);
            if (isHovered(config.damageX, config.damageY, dmgW, dmgH, mouseX, mouseY)) {
                draggingTarget = DragTarget.DAMAGE;
                dragOffsetX = (int) mouseX - config.damageX;
                dragOffsetY = (int) mouseY - config.damageY;
                return true;
            }

            for (int i = 0; i < 9; i++) {
                int slotW = (int) (18 * config.hudSlotsScale[i]);
                int slotH = (int) (18 * config.hudSlotsScale[i]);
                if (isHovered(config.hudSlotsX[i], config.hudSlotsY[i], slotW, slotH, mouseX, mouseY)) {
                    draggingTarget = DragTarget.SLOT;
                    draggedSlotIndex = i;
                    dragOffsetX = (int) mouseX - config.hudSlotsX[i];
                    dragOffsetY = (int) mouseY - config.hudSlotsY[i];
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingTarget != null) {
            int rawX = (int) mouseX - dragOffsetX;
            int rawY = (int) mouseY - dragOffsetY;

            int targetW = getTargetWidth(draggingTarget, draggedSlotIndex);
            int targetH = getTargetHeight(draggingTarget, draggedSlotIndex);

            int snappedX = applySnapX(rawX, targetW, draggingTarget, draggedSlotIndex);
            int snappedY = applySnapY(rawY, targetH, draggingTarget, draggedSlotIndex);

            snappedX = Math.max(0, Math.min(this.width - targetW, snappedX));
            snappedY = Math.max(0, Math.min(this.height - targetH, snappedY));

            switch (draggingTarget) {
                case MANA -> { config.manaX = snappedX; config.manaY = snappedY; }
                case DAMAGE -> { config.damageX = snappedX; config.damageY = snappedY; }
                case SLOT -> {
                    if (draggedSlotIndex >= 0 && draggedSlotIndex < 9) {
                        config.hudSlotsX[draggedSlotIndex] = snappedX;
                        config.hudSlotsY[draggedSlotIndex] = snappedY;
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float delta = amount > 0 ? 0.1f : -0.1f;

        int manaW = (int) (80 * config.manaScale);
        int manaH = (int) (16 * config.manaScale);
        if (isHovered(config.manaX, config.manaY, manaW, manaH, mouseX, mouseY)) {
            config.manaScale = Math.max(0.5f, Math.min(3.0f, config.manaScale + delta));
            return true;
        }

        int dmgW = (int) (80 * config.damageScale);
        int dmgH = (int) (16 * config.damageScale);
        if (isHovered(config.damageX, config.damageY, dmgW, dmgH, mouseX, mouseY)) {
            config.damageScale = Math.max(0.5f, Math.min(3.0f, config.damageScale + delta));
            return true;
        }

        for (int i = 0; i < 9; i++) {
            int slotW = (int) (18 * config.hudSlotsScale[i]);
            int slotH = (int) (18 * config.hudSlotsScale[i]);
            if (isHovered(config.hudSlotsX[i], config.hudSlotsY[i], slotW, slotH, mouseX, mouseY)) {
                config.hudSlotsScale[i] = Math.max(0.5f, Math.min(3.0f, config.hudSlotsScale[i] + delta));
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingTarget = null;
        draggedSlotIndex = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        drawCenteredTextWithShadow(matrices, this.textRenderer, "Húzás = Mozgatás | Görgő = Méretezés | Mágneses igazítás bekapcsolva", this.width / 2, 8, 0xFFFF55);

        int manaW = (int) (80 * config.manaScale);
        int manaH = (int) (16 * config.manaScale);
        renderBox(matrices, config.manaX, config.manaY, manaW, manaH, "§bMana", config.showMana, mouseX, mouseY);

        int dmgW = (int) (80 * config.damageScale);
        int dmgH = (int) (16 * config.damageScale);
        renderBox(matrices, config.damageX, config.damageY, dmgW, dmgH, "§cSebzés", config.showDamage, mouseX, mouseY);

        for (int i = 0; i < 9; i++) {
            boolean enabled = config.hudSlotsEnabled[i];
            int slotSize = (int) (18 * config.hudSlotsScale[i]);
            int sx = config.hudSlotsX[i];
            int sy = config.hudSlotsY[i];
            boolean hovered = isHovered(sx, sy, slotSize, slotSize, mouseX, mouseY);

            int bgColor = hovered ? 0xAA00FF00 : (enabled ? 0x8822AA22 : 0x88AA2222);
            fill(matrices, sx, sy, sx + slotSize, sy + slotSize, bgColor);
            drawCustomBorder(matrices, sx, sy, slotSize, slotSize, hovered ? 0xFF00FF00 : 0xFFFFFFFF);

            String label = String.valueOf(i + 1);
            this.textRenderer.drawWithShadow(matrices, label, sx + slotSize / 2 - 3, sy + slotSize / 2 - 4, 0xFFFFFF);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private int applySnapX(int x, int width, DragTarget currentTarget, int currentSlotIndex) {
        for (BoxBounds b : getAllOtherBoxes(currentTarget, currentSlotIndex)) {
            if (Math.abs(x - b.x) < SNAP_THRESHOLD) return b.x;
            if (Math.abs((x + width) - b.x) < SNAP_THRESHOLD) return b.x - width;
            if (Math.abs(x - (b.x + b.w)) < SNAP_THRESHOLD) return b.x + b.w;
            if (Math.abs((x + width) - (b.x + b.w)) < SNAP_THRESHOLD) return b.x + b.w - width;
        }
        return x;
    }

    private int applySnapY(int y, int height, DragTarget currentTarget, int currentSlotIndex) {
        for (BoxBounds b : getAllOtherBoxes(currentTarget, currentSlotIndex)) {
            if (Math.abs(y - b.y) < SNAP_THRESHOLD) return b.y;
            if (Math.abs((y + height) - b.y) < SNAP_THRESHOLD) return b.y - height;
            if (Math.abs(y - (b.y + b.h)) < SNAP_THRESHOLD) return b.y + b.h;
            if (Math.abs((y + height) - (b.y + b.h)) < SNAP_THRESHOLD) return b.y + b.h - height;
        }
        return y;
    }

    private List<BoxBounds> getAllOtherBoxes(DragTarget ignoreTarget, int ignoreSlotIndex) {
        List<BoxBounds> list = new ArrayList<>();
        if (ignoreTarget != DragTarget.MANA)
            list.add(new BoxBounds(config.manaX, config.manaY, (int)(80 * config.manaScale), (int)(16 * config.manaScale), DragTarget.MANA, -1));
        if (ignoreTarget != DragTarget.DAMAGE)
            list.add(new BoxBounds(config.damageX, config.damageY, (int)(80 * config.damageScale), (int)(16 * config.damageScale), DragTarget.DAMAGE, -1));

        for (int i = 0; i < 9; i++) {
            if (ignoreTarget == DragTarget.SLOT && ignoreSlotIndex == i) continue;
            int slotSize = (int)(18 * config.hudSlotsScale[i]);
            list.add(new BoxBounds(config.hudSlotsX[i], config.hudSlotsY[i], slotSize, slotSize, DragTarget.SLOT, i));
        }
        return list;
    }

    private int getTargetWidth(DragTarget target, int slotIndex) {
        return switch (target) {
            case MANA -> (int) (80 * config.manaScale);
            case DAMAGE -> (int) (80 * config.damageScale);
            case SLOT -> (int) (18 * config.hudSlotsScale[slotIndex]);
        };
    }

    private int getTargetHeight(DragTarget target, int slotIndex) {
        return switch (target) {
            case MANA -> (int) (16 * config.manaScale);
            case DAMAGE -> (int) (16 * config.damageScale);
            case SLOT -> (int) (18 * config.hudSlotsScale[slotIndex]);
        };
    }

    private void renderBox(MatrixStack matrices, int x, int y, int w, int h, String text, boolean isEnabledInConfig, int mouseX, int mouseY) {
        boolean hovered = isHovered(x, y, w, h, mouseX, mouseY);
        int bgColor = hovered ? 0xAA00FF00 : (isEnabledInConfig ? 0x77000000 : 0x77AA0000);
        fill(matrices, x, y, x + w, y + h, bgColor);
        drawCustomBorder(matrices, x, y, w, h, hovered ? 0xFF00FF00 : (isEnabledInConfig ? 0xFFFFFFFF : 0xFFFF5555));
        String fullText = text + (isEnabledInConfig ? "" : " §c(KI)");
        this.textRenderer.drawWithShadow(matrices, fullText, x + 4, y + h / 2 - 4, 0xFFFFFF);
    }

    private void drawCustomBorder(MatrixStack matrices, int x, int y, int w, int h, int color) {
        fill(matrices, x, y, x + w, y + 1, color);
        fill(matrices, x, y + h - 1, x + w, y + h, color);
        fill(matrices, x, y, x + 1, y + h, color);
        fill(matrices, x + w - 1, y, x + w, y + h, color);
    }

    private boolean isHovered(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}