package me.Gabix.hud;

import me.Gabix.core.MinecraftSettingsApplier;
import me.Gabix.core.StorageConfig;
import me.Gabix.fireoverlay.screen.FireOverlayConfigScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class StorageConfigScreen extends Screen {
    private static final String[] BOSS_CHOICES = {"Ki", "Jégsárkány", "Trex", "Endermágus", "Warden", "RadioPók", "Kraken", "Nyálkakirály"};
    private static final String SECRET_CODE = "marcsi";
    private static final int SECRET_BUFFER_LIMIT = 32;

    private final StorageConfig config;
    private final StringBuilder secretBuffer = new StringBuilder();
    private ItemPresetListWidget listWidget;
    private boolean mcButtonUnlocked = false;

    public StorageConfigScreen(StorageConfig config) {
        super(Text.literal("Actionbar Storage Beállítások"));
        this.config = config;
    }

    @Override
    protected void init() {
        listWidget = new ItemPresetListWidget(config);
        addDrawableChild(listWidget);

        if (mcButtonUnlocked) {
            addDrawableChild(createMcApplyButton());
        }

        addDrawableChild(ButtonWidget.builder(
                Text.literal("HUD Elemek Áthelyezése"),
                b -> {
                    if (client != null) client.setScreen(new HudPositionEditorScreen(config, this));
                }
        ).dimensions(width / 2 - 155, height - 26, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Bezárás"),
                b -> {
                    config.save();
                    close();
                }
        ).dimensions(width / 2 + 5, height - 26, 150, 20).build());
    }

    private ButtonWidget createMcApplyButton() {
        return ButtonWidget.builder(
                Text.literal("MC Beállítások Alkalmazása"),
                b -> applyMcSettings()
        ).dimensions(width / 2 - 155, height - 50, 150, 20).build();
    }

    private void applyMcSettings() {
        if (client == null) {
            return;
        }
        MinecraftSettingsApplier.applyPreset(client);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!mcButtonUnlocked && !(getFocused() instanceof TextFieldWidget) && !Character.isISOControl(chr)) {
            secretBuffer.append(Character.toLowerCase(chr));
            if (secretBuffer.length() > SECRET_BUFFER_LIMIT) {
                secretBuffer.delete(0, secretBuffer.length() - SECRET_BUFFER_LIMIT);
            }
            if (secretBuffer.toString().endsWith(SECRET_CODE)) {
                mcButtonUnlocked = true;
                secretBuffer.setLength(0);
                clearAndInit();
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isShiftHoveringList(mouseX, mouseY)) {
            var command = buildGiveCommand(listWidget.getHoveredEntry(mouseX, mouseY));
            if (command != null) {
                client.player.networkHandler.sendCommand(command);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isShiftHoveringList(double mouseX, double mouseY) {
        if (client == null || client.player == null) return false;
        if (!InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT)) return false;
        return mouseY >= listWidget.getTop() && mouseY <= listWidget.getBottom()
                && mouseX >= listWidget.getLeft() && mouseX <= listWidget.getRight();
    }

    private String buildGiveCommand(ItemPresetListWidget.Entry entry) {
        if (entry instanceof ItemPresetListWidget.NormalItemEntry e) {
            return giveCommand(e.preset.itemId, e.preset.customModelData, e.preset.name);
        }
        if (entry instanceof ItemPresetListWidget.LevelSubEntry e) {
            var parent = findParent(e.level);
            return parent == null ? null
                    : giveCommand(parent.itemId, parent.customModelData, parent.name + " " + e.level.levelNameIndicator);
        }
        if (entry instanceof ItemPresetListWidget.LeveledItemHeaderEntry e) {
            return giveCommand(e.preset.itemId, e.preset.customModelData, e.preset.name);
        }
        return null;
    }

    private StorageConfig.LeveledItemPreset findParent(StorageConfig.ItemLevel level) {
        if (config.leveledItems == null) return null;
        for (var preset : config.leveledItems) {
            if (preset.levels != null && preset.levels.contains(level)) return preset;
        }
        return null;
    }

    private static String giveCommand(String itemId, int customModelData, String name) {
        String escaped = name
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("§", "\\u00A7");
        return "give @s " + itemId + "{CustomModelData:" + customModelData
                + ",display:{Name:'{\"text\":\"" + escaped + "\",\"italic\":false}'}}";
    }

    private static ButtonWidget toggleButton(int width, BooleanSupplier getter, Consumer<Boolean> setter,
                                             String trueLabel, String falseLabel) {
        return ButtonWidget.builder(
                Text.literal(getter.getAsBoolean() ? trueLabel : falseLabel),
                b -> {
                    boolean next = !getter.getAsBoolean();
                    setter.accept(next);
                    b.setMessage(Text.literal(next ? trueLabel : falseLabel));
                }
        ).dimensions(0, 0, width, 18).build();
    }

    private static ButtonWidget actionButton(int width, String label, Runnable action) {
        return ButtonWidget.builder(Text.literal(label), b -> action.run()).dimensions(0, 0, width, 18).build();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawCenteredTextWithShadow(matrices, textRenderer, title, width / 2, 8, 0xFFFFFF);
    }

    private class ItemPresetListWidget extends ElementListWidget<ItemPresetListWidget.Entry> {
        ItemPresetListWidget(StorageConfig config) {
            super(StorageConfigScreen.this.client, StorageConfigScreen.this.width,
                    StorageConfigScreen.this.height, 24, StorageConfigScreen.this.height - 32, 24);

            addEntry(new AutoBossDetectionEntry(config));
            addEntry(new BossNotificationEntry(config));
            addEntry(new HudSlotsToggleEntry(config));
            addEntry(new DisplayTogglesEntry(config));
            addEntry(new NoRenderInactiveEntry(config));
            addEntry(new MuteOnInactiveEntry(config));
            addEntry(new FpsLimitEntry(config));
            addEntry(new FpsLimitImmediateEntry(config));
            addEntry(new HideBlockBreakingParticlesEntry(config));
            addEntry(new FireOverlayEntry(config));
            addEntry(new FpsCpsToggleEntry(config));

            if (config.normalItems != null) {
                config.normalItems.forEach(p -> addEntry(new NormalItemEntry(p)));
            }
            if (config.leveledItems != null) {
                config.leveledItems.forEach(p -> addEntry(new LeveledItemHeaderEntry(p, this)));
            }
        }

        Entry getHoveredEntry(double x, double y) { return getEntryAtPosition(x, y); }
        int getLeft() { return left; }
        int getRight() { return right; }
        int getTop() { return top; }
        int getBottom() { return bottom; }

        void toggleLevels(LeveledItemHeaderEntry header, StorageConfig.LeveledItemPreset preset) {
            if (preset.levels == null || preset.levels.isEmpty()) return;
            var idx = children().indexOf(header);
            if (idx == -1) return;
            if (header.isExpanded()) {
                int offset = 1;
                for (var level : preset.levels) {
                    children().add(idx + offset++, new LevelSubEntry(level));
                }
            } else {
                for (int i = 0; i < preset.levels.size(); i++) {
                    if (idx + 1 < children().size() && children().get(idx + 1) instanceof LevelSubEntry) {
                        children().remove(idx + 1);
                    }
                }
            }
        }

        @Override public int getRowWidth() { return 360; }
        @Override protected int getScrollbarPositionX() { return width / 2 + 190; }

        abstract static class Entry extends ElementListWidget.Entry<Entry> {}

        abstract class BaseEntry extends Entry {
            protected final List<Element> children = new ArrayList<>();
            protected final List<Selectable> selectables = new ArrayList<>();

            @Override public List<? extends Element> children() { return children; }
            @Override public List<? extends Selectable> selectableChildren() { return selectables; }

            protected <T extends ButtonWidget> T track(T button) {
                children.add(button);
                selectables.add(button);
                return button;
            }

            protected void fillBg(MatrixStack m, int x, int y, int w, int h, int color) {
                fill(m, x, y, x + w, y + h - 2, color);
            }

            protected void label(MatrixStack m, String text, int x, int y, int color) {
                StorageConfigScreen.this.textRenderer.drawWithShadow(m, text, x, y, color);
            }

            protected void label(MatrixStack m, String text, int x, int y) {
                label(m, text, x, y, 0xFFFFFF);
            }
        }

        private class AutoBossDetectionEntry extends BaseEntry {
            private final ButtonWidget toggleBtn;

            AutoBossDetectionEntry(StorageConfig config) {
                toggleBtn = track(toggleButton(45,
                        () -> config.autoBossDetection,
                        v -> { config.autoBossDetection = v; config.save(); },
                        "§aBE", "§cKI"));
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x4400AAAA);
                label(m, "Auto Boss Felismerés:", x + 6, y + 5);
                toggleBtn.setX(x + w - 55);
                toggleBtn.setY(y + 1);
                toggleBtn.render(m, mx, my, d);
            }
        }

        private class BossNotificationEntry extends BaseEntry {
            private final ButtonWidget cycleBtn;

            BossNotificationEntry(StorageConfig config) {
                cycleBtn = track(ButtonWidget.builder(
                        Text.literal(config.selectedBossNotification),
                        b -> {
                            int current = 0;
                            for (int i = 0; i < BOSS_CHOICES.length; i++) {
                                if (BOSS_CHOICES[i].equals(config.selectedBossNotification)) { current = i; break; }
                            }
                            config.selectedBossNotification = BOSS_CHOICES[(current + 1) % BOSS_CHOICES.length];
                            b.setMessage(Text.literal(config.selectedBossNotification));
                            config.save();
                        }
                ).dimensions(0, 0, 100, 18).build());
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x4400AAAA);
                label(m, "§bBoss Sebzés Értesítő:", x + 6, y + 5);
                cycleBtn.setX(x + w - 106);
                cycleBtn.setY(y + 1);
                cycleBtn.render(m, mx, my, d);
            }
        }

        private class FpsCpsToggleEntry extends BaseEntry {
            private final ButtonWidget fpsToggle, cpsToggle;

            FpsCpsToggleEntry(StorageConfig config) {
                fpsToggle = track(toggleButton(70,
                        () -> config.showFps,
                        v -> { config.showFps = v; config.save(); },
                        "§aFPS BE", "§cFPS KI"));
                cpsToggle = track(toggleButton(70,
                        () -> config.showCps,
                        v -> { config.showCps = v; config.save(); },
                        "§aCPS BE", "§cCPS KI"));
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                var cfg = StorageConfigScreen.this.config;
                fillBg(m, x, y, w, h, 0x44AA88AA);
                label(m, "§dFPS / CPS:", x + 6, y + 5);
                fpsToggle.setX(x + 100); fpsToggle.setY(y + 1); fpsToggle.render(m, mx, my, d);
                cpsToggle.setX(x + 180); cpsToggle.setY(y + 1); cpsToggle.render(m, mx, my, d);
                label(m, "§7(Fps: " + cfg.fpsX + "," + cfg.fpsY + "  Cps: " + cfg.cpsX + "," + cfg.cpsY + ")",
                        x + 260, y + 5, 0xAAAAAA);
            }
        }

        private class DisplayTogglesEntry extends BaseEntry {
            private final ButtonWidget manaBtn, damageBtn;

            DisplayTogglesEntry(StorageConfig config) {
                manaBtn = track(toggleButton(45,
                        () -> config.showMana,
                        v -> { config.showMana = v; config.save(); },
                        "§aBE", "§cKI"));
                damageBtn = track(toggleButton(45,
                        () -> config.showDamage,
                        v -> { config.showDamage = v; config.save(); },
                        "§aBE", "§cKI"));
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x445555AA);
                label(m, "§bMana:", x + 6, y + 5);
                manaBtn.setX(x + 50); manaBtn.setY(y + 1); manaBtn.render(m, mx, my, d);
                label(m, "§cSebzés:", x + 180, y + 5);
                damageBtn.setX(x + 235); damageBtn.setY(y + 1); damageBtn.render(m, mx, my, d);
            }
        }

        private class NoRenderInactiveEntry extends BaseEntry {
            private final ButtonWidget toggleBtn;

            NoRenderInactiveEntry(StorageConfig config) {
                toggleBtn = track(toggleButton(45,
                        () -> config.noRenderOnInactive,
                        v -> { config.noRenderOnInactive = v; config.save(); },
                        "§aBE", "§cKI"));
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6NoRender Ablak Inaktívnál:", x + 6, y + 5);
                toggleBtn.setX(x + w - 55); toggleBtn.setY(y + 1); toggleBtn.render(m, mx, my, d);
            }
        }

        private class MuteOnInactiveEntry extends BaseEntry {
            private final ButtonWidget toggleBtn;

            MuteOnInactiveEntry(StorageConfig config) {
                toggleBtn = track(toggleButton(45,
                        () -> config.muteOnInactive,
                        v -> { config.muteOnInactive = v; config.save(); },
                        "§aBE", "§cKI"));
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6Némítás Ablak Inaktívnál:", x + 6, y + 5);
                toggleBtn.setX(x + w - 55); toggleBtn.setY(y + 1); toggleBtn.render(m, mx, my, d);
            }
        }

        private class FpsLimitEntry extends BaseEntry {
            private final ButtonWidget toggleBtn, minMinusBtn, minPlusBtn, secMinusBtn, secPlusBtn;
            private final TextFieldWidget fpsField;

            FpsLimitEntry(StorageConfig config) {
                toggleBtn = track(toggleButton(32,
                        () -> config.fpsLimitOnInactive,
                        v -> { config.fpsLimitOnInactive = v; config.save(); },
                        "§aBE", "§cKI"));
                minMinusBtn = track(actionButton(16, "-", () -> {
                    if (config.fpsLimitMinutes > 0) config.fpsLimitMinutes--;
                    config.save();
                }));
                minPlusBtn = track(actionButton(16, "+", () -> {
                    if (config.fpsLimitMinutes < 60) config.fpsLimitMinutes++;
                    config.save();
                }));
                secMinusBtn = track(actionButton(16, "-", () -> {
                    if (config.fpsLimitSeconds >= 5) config.fpsLimitSeconds -= 5;
                    config.save();
                }));
                secPlusBtn = track(actionButton(16, "+", () -> {
                    if (config.fpsLimitSeconds < 55) config.fpsLimitSeconds += 5;
                    config.save();
                }));

                fpsField = new TextFieldWidget(StorageConfigScreen.this.textRenderer, 0, 0, 40, 16, Text.literal("FPS"));
                fpsField.setText(String.valueOf(config.fpsLimitValue));
                fpsField.setChangedListener(text -> {
                    try {
                        int v = Integer.parseInt(text);
                        boolean valid = v >= 1 && v <= 260;
                        if (valid) {
                            config.fpsLimitValue = v;
                            config.save();
                        }
                        fpsField.setEditableColor(valid ? 0xFFFFFF : 0xFF5555);
                    } catch (NumberFormatException e) {
                        fpsField.setEditableColor(0xFF5555);
                    }
                });
                children.add(fpsField);
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                var cfg = StorageConfigScreen.this.config;
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6Auto FPS Limit Idle:", x + 6, y + 5);

                toggleBtn.setX(x + 130); toggleBtn.setY(y + 1); toggleBtn.render(m, mx, my, d);
                minMinusBtn.setX(x + 170); minMinusBtn.setY(y + 1); minMinusBtn.render(m, mx, my, d);
                label(m, cfg.fpsLimitMinutes + "m", x + 190, y + 5, 0xFFFF55);
                minPlusBtn.setX(x + 212); minPlusBtn.setY(y + 1); minPlusBtn.render(m, mx, my, d);
                secMinusBtn.setX(x + 235); secMinusBtn.setY(y + 1); secMinusBtn.render(m, mx, my, d);
                label(m, cfg.fpsLimitSeconds + "s", x + 255, y + 5, 0xFFFF55);
                secPlusBtn.setX(x + 277); secPlusBtn.setY(y + 1); secPlusBtn.render(m, mx, my, d);
                fpsField.setX(x + 305); fpsField.setY(y + 1); fpsField.render(m, mx, my, d);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int b) {
                return fpsField.mouseClicked(mx, my, b) || super.mouseClicked(mx, my, b);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return fpsField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean charTyped(char chr, int modifiers) {
                return fpsField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
            }
        }

        private class FpsLimitImmediateEntry extends BaseEntry {
            private final ButtonWidget toggleBtn;

            FpsLimitImmediateEntry(StorageConfig config) {
                toggleBtn = track(toggleButton(45,
                        () -> config.fpsLimitImmediateOnInactive,
                        v -> { config.fpsLimitImmediateOnInactive = v; config.save(); },
                        "§aBE", "§cKI"));
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6FPS Limit Ablak Inaktívnál:", x + 6, y + 5);
                toggleBtn.setX(x + w - 55); toggleBtn.setY(y + 1); toggleBtn.render(m, mx, my, d);
            }
        }

        private class HideBlockBreakingParticlesEntry extends BaseEntry {
            private final ButtonWidget toggleBtn;

            HideBlockBreakingParticlesEntry(StorageConfig config) {
                toggleBtn = track(toggleButton(45,
                        () -> config.hideBlockBreakingParticles,
                        v -> { config.hideBlockBreakingParticles = v; config.save(); },
                        "§cKI", "§aBE"));
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6Block Break Részecskék:", x + 6, y + 5);
                toggleBtn.setX(x + w - 55); toggleBtn.setY(y + 1); toggleBtn.render(m, mx, my, d);
            }
        }

        private class FireOverlayEntry extends BaseEntry {
            private final ButtonWidget toggleBtn, settingsBtn;

            FireOverlayEntry(StorageConfig config) {
                toggleBtn = track(toggleButton(32,
                        () -> config.hideFireOverlay,
                        v -> { config.hideFireOverlay = v; config.save(); },
                        "§cKI", "§aBE"));
                settingsBtn = track(ButtonWidget.builder(
                        Text.literal("Beállítás"),
                        b -> {
                            if (StorageConfigScreen.this.client != null) {
                                StorageConfigScreen.this.client.setScreen(
                                        new FireOverlayConfigScreen(config, StorageConfigScreen.this));
                            }
                        }
                ).dimensions(0, 0, 60, 18).build());
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44CC5500);
                label(m, "§cTűz Overlay:", x + 6, y + 5);
                toggleBtn.setX(x + 130); toggleBtn.setY(y + 1); toggleBtn.render(m, mx, my, d);
                settingsBtn.setX(x + 170); settingsBtn.setY(y + 1); settingsBtn.render(m, mx, my, d);
            }
        }

        private class HudSlotsToggleEntry extends BaseEntry {
            private final List<ButtonWidget> slotButtons = new ArrayList<>();

            HudSlotsToggleEntry(StorageConfig config) {
                for (int i = 0; i < 10; i++) {
                    var btn = createSlotButton(config, i);
                    slotButtons.add(btn);
                    track(btn);
                }
            }

            private ButtonWidget createSlotButton(StorageConfig config, int i) {
                if (i == 0) {
                    return toggleButton(16,
                            () -> config.offhandSlotEnabled,
                            v -> { config.offhandSlotEnabled = v; config.save(); },
                            "§a0", "§c0");
                }
                int slotIdx = i - 1;
                String label = String.valueOf(i);
                return toggleButton(16,
                        () -> config.hudSlotsEnabled[slotIdx],
                        v -> { config.hudSlotsEnabled[slotIdx] = v; config.save(); },
                        "§a" + label, "§c" + label);
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x4422AAAA);
                label(m, "§eHUD Slotok (0=offhand):", x + 6, y + 5);
                int startX = x + w - 185;
                for (int j = 0; j < slotButtons.size(); j++) {
                    var btn = slotButtons.get(j);
                    btn.setX(startX + (j * 18));
                    btn.setY(y + 1);
                    btn.render(m, mx, my, d);
                }
            }
        }

        class NormalItemEntry extends BaseEntry {
            final StorageConfig.ItemPreset preset;

            NormalItemEntry(StorageConfig.ItemPreset preset) {
                this.preset = preset;
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, hov ? 0x44FFFFFF : 0x22FFFFFF);
                label(m, preset.name + " §7(CMD: " + preset.customModelData + ")", x + 6, y + 6, 0xE0E0E0);
                var info = "§7B: §e" + (preset.leftCD / 20.0f) + "s §7| J: §d" + (preset.rightCD / 20.0f) + "s";
                int infoWidth = StorageConfigScreen.this.textRenderer.getWidth(info);
                label(m, info, x + w - infoWidth - 6, y + 6);
            }
        }

        class LeveledItemHeaderEntry extends BaseEntry {
            final StorageConfig.LeveledItemPreset preset;
            private final ButtonWidget toggleBtn;
            private boolean expanded = false;

            LeveledItemHeaderEntry(StorageConfig.LeveledItemPreset preset, ItemPresetListWidget parent) {
                this.preset = preset;
                toggleBtn = track(ButtonWidget.builder(Text.literal("►"), b -> {
                    expanded = !expanded;
                    b.setMessage(Text.literal(expanded ? "▼" : "►"));
                    parent.toggleLevels(this, preset);
                }).dimensions(0, 0, 20, 18).build());
            }

            boolean isExpanded() { return expanded; }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA00AA);
                toggleBtn.setX(x + 4); toggleBtn.setY(y + 1); toggleBtn.render(m, mx, my, d);
                label(m, "§6" + preset.name + " §7(CMD: " + preset.customModelData + ") §d[Fejleszthető]",
                        x + 28, y + 5);
            }
        }

        class LevelSubEntry extends BaseEntry {
            final StorageConfig.ItemLevel level;

            LevelSubEntry(StorageConfig.ItemLevel level) {
                this.level = level;
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fill(m, x + 15, y, x + w, y + h - 2, hov ? 0x44000000 : 0x22000000);
                fill(m, x + 8, y, x + 9, y + h, 0x44FFFFFF);
                fill(m, x + 8, y + (h / 2), x + 14, y + (h / 2) + 1, 0x44FFFFFF);
                label(m, "§b• " + level.levelNameIndicator, x + 20, y + 6, 0xE0E0E0);
                var stats = "§7B: §e" + (level.leftCD / 20.0f) + "s (" + level.leftMana + "m)"
                        + " §7| J: §d" + (level.rightCD / 20.0f) + "s (" + level.rightMana + "m)";
                int statsWidth = StorageConfigScreen.this.textRenderer.getWidth(stats);
                label(m, stats, x + w - statsWidth - 6, y + 6, 0xCCCCCC);
            }
        }
    }
}