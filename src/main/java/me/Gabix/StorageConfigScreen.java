package me.Gabix;

import me.Gabix.fireoverlay.screen.FireOverlayConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageConfigScreen extends Screen {
    private final StorageConfig config;
    private ItemPresetListWidget listWidget;

    public StorageConfigScreen(StorageConfig config) {
        super(Text.literal("Actionbar Storage Beállítások"));
        this.config = config;
    }

    @Override
    protected void init() {
        this.listWidget = new ItemPresetListWidget(this.config);
        this.addDrawableChild(this.listWidget);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("HUD Elemek Áthelyezése"), button -> {
            if (this.client != null) {
                this.client.setScreen(new HudPositionEditorScreen(this.config, this));
            }
        }).dimensions(this.width / 2 - 155, this.height - 26, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Bezárás"), button -> {
            this.config.save();
            this.close();
        }).dimensions(this.width / 2 + 5, this.height - 26, 150, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.client != null && this.client.player != null) {
            long windowHandle = this.client.getWindow().getHandle();
            if (InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                if (mouseY >= this.listWidget.getTop() && mouseY <= this.listWidget.getBottom() &&
                        mouseX >= this.listWidget.getLeft() && mouseX <= this.listWidget.getRight()) {
                    ItemPresetListWidget.Entry hoveredEntry = this.listWidget.getHoveredEntry(mouseX, mouseY);
                    if (hoveredEntry != null) {
                        if (hoveredEntry instanceof ItemPresetListWidget.NormalItemEntry normalEntry) {
                            StorageConfig.ItemPreset preset = normalEntry.preset;
                            String cmd = "give @s " + preset.itemId + "{CustomModelData:" + preset.customModelData + ",display:{Name:'{\"text\":\"" + preset.name.replace("§", "\\u00A7") + "\",\"italic\":false}'}}";
                            this.client.player.networkHandler.sendCommand(cmd);
                            return true;
                        }
                        if (hoveredEntry instanceof ItemPresetListWidget.LevelSubEntry subEntry) {
                            StorageConfig.ItemLevel level = subEntry.level;
                            StorageConfig.LeveledItemPreset parentPreset = null;
                            if (this.config.leveledItems != null) {
                                for (StorageConfig.LeveledItemPreset p : this.config.leveledItems) {
                                    if (p.levels != null && p.levels.contains(level)) {
                                        parentPreset = p;
                                        break;
                                    }
                                }
                            }
                            if (parentPreset != null) {
                                String fullName = parentPreset.name + " " + level.levelNameIndicator;
                                String cmd = "give @s " + parentPreset.itemId + "{CustomModelData:" + parentPreset.customModelData + ",display:{Name:'{\"text\":\"" + fullName.replace("§", "\\u00A7") + "\",\"italic\":false}'}}";
                                this.client.player.networkHandler.sendCommand(cmd);
                                return true;
                            }
                        }
                        if (hoveredEntry instanceof ItemPresetListWidget.LeveledItemHeaderEntry headerEntry) {
                            StorageConfig.LeveledItemPreset preset = headerEntry.preset;
                            String cmd = "give @s " + preset.itemId + "{CustomModelData:" + preset.customModelData + ",display:{Name:'{\"text\":\"" + preset.name.replace("§", "\\u00A7") + "\",\"italic\":false}'}}";
                            this.client.player.networkHandler.sendCommand(cmd);
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.listWidget.render(matrices, mouseX, mouseY, delta);
        drawCenteredTextWithShadow(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private class ItemPresetListWidget extends ElementListWidget<ItemPresetListWidget.Entry> {
        public ItemPresetListWidget(StorageConfig config) {
            super(StorageConfigScreen.this.client, StorageConfigScreen.this.width, StorageConfigScreen.this.height, 24, StorageConfigScreen.this.height - 32, 24);

            this.addEntry(new AutoBossDetectionEntry(config));
            this.addEntry(new BossNotificationEntry(config));
            this.addEntry(new HudSlotsToggleEntry(config));
            this.addEntry(new DisplayTogglesEntry(config));
            this.addEntry(new NoRenderInactiveEntry(config));
            this.addEntry(new FpsLimitEntry(config));
            this.addEntry(new FpsLimitImmediateEntry(config));
            this.addEntry(new HideBlockBreakingParticlesEntry(config));
            this.addEntry(new FireOverlayEntry(config));
            this.addEntry(new FpsCpsToggleEntry(config));

            if (config.normalItems != null) {
                for (StorageConfig.ItemPreset preset : config.normalItems) {
                    this.addEntry(new NormalItemEntry(preset));
                }
            }

            if (config.leveledItems != null) {
                for (StorageConfig.LeveledItemPreset leveledPreset : config.leveledItems) {
                    LeveledItemHeaderEntry header = new LeveledItemHeaderEntry(leveledPreset, this);
                    this.addEntry(header);
                }
            }
        }

        public Entry getHoveredEntry(double mouseX, double mouseY) {
            return this.getEntryAtPosition(mouseX, mouseY);
        }

        public int getLeft() { return this.left; }
        public int getRight() { return this.right; }
        public int getTop() { return this.top; }
        public int getBottom() { return this.bottom; }

        public void toggleLevels(LeveledItemHeaderEntry header, StorageConfig.LeveledItemPreset preset) {
            int headerIndex = this.children().indexOf(header);
            if (headerIndex == -1) return;

            if (header.isExpanded()) {
                int insertOffset = 1;
                for (StorageConfig.ItemLevel level : preset.levels) {
                    this.children().add(headerIndex + insertOffset, new LevelSubEntry(level));
                    insertOffset++;
                }
            } else {
                int numLevels = preset.levels.size();
                for (int i = 0; i < numLevels; i++) {
                    if (headerIndex + 1 < this.children().size() && this.children().get(headerIndex + 1) instanceof LevelSubEntry) {
                        this.children().remove(headerIndex + 1);
                    }
                }
            }
        }

        @Override
        public int getRowWidth() { return 360; }

        @Override
        protected int getScrollbarPositionX() { return this.width / 2 + 190; }

        abstract static class Entry extends ElementListWidget.Entry<Entry> {}

        private class AutoBossDetectionEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget toggleBtn;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();

            public AutoBossDetectionEntry(StorageConfig config) {
                this.config = config;
                this.toggleBtn = ButtonWidget.builder(
                        Text.literal(config.autoBossDetection ? "§aBE" : "§cKI"),
                        btn -> {
                            config.autoBossDetection = !config.autoBossDetection;
                            btn.setMessage(Text.literal(config.autoBossDetection ? "§aBE" : "§cKI"));
                            config.save();
                        }
                ).dimensions(0, 0, 45, 18).build();

                this.children.add(this.toggleBtn);
                this.selectables.add(this.toggleBtn);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x4400AAAA);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "Auto Boss Felismerés:", x + 6, y + 5, 0xFFFFFF);
                this.toggleBtn.setX(x + entryWidth - 55);
                this.toggleBtn.setY(y + 1);
                this.toggleBtn.render(matrices, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }
        }

        private class BossNotificationEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget cycleButton;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();
            private final String[] choices = {"Ki", "JégSárkány", "Trex", "Endermágus", "Warden", "RadioPók", "Kraken"};

            public BossNotificationEntry(StorageConfig config) {
                this.config = config;
                this.cycleButton = ButtonWidget.builder(Text.literal(config.selectedBossNotification), button -> {
                    int currentIndex = 0;
                    for (int i = 0; i < choices.length; i++) {
                        if (choices[i].equals(config.selectedBossNotification)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    int nextIndex = (currentIndex + 1) % choices.length;
                    config.selectedBossNotification = choices[nextIndex];
                    button.setMessage(Text.literal(config.selectedBossNotification));
                    config.save();
                }).dimensions(0, 0, 100, 18).build();

                this.children.add(this.cycleButton);
                this.selectables.add(this.cycleButton);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x4400AAAA);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§bBoss Sebzés Értesítő:", x + 6, y + 5, 0xFFFFFF);
                this.cycleButton.setX(x + entryWidth - 106);
                this.cycleButton.setY(y + 1);
                this.cycleButton.render(matrices, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }
        }

        private class FpsCpsToggleEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget fpsToggle, cpsToggle;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();

            public FpsCpsToggleEntry(StorageConfig config) {
                this.config = config;
                fpsToggle = ButtonWidget.builder(
                        Text.literal(config.showFps ? "§aFPS BE" : "§cFPS KI"),
                        btn -> {
                            config.showFps = !config.showFps;
                            btn.setMessage(Text.literal(config.showFps ? "§aFPS BE" : "§cFPS KI"));
                            config.save();
                        }
                ).dimensions(0, 0, 70, 18).build();
                cpsToggle = ButtonWidget.builder(
                        Text.literal(config.showCps ? "§aCPS BE" : "§cCPS KI"),
                        btn -> {
                            config.showCps = !config.showCps;
                            btn.setMessage(Text.literal(config.showCps ? "§aCPS BE" : "§cCPS KI"));
                            config.save();
                        }
                ).dimensions(0, 0, 70, 18).build();
                children.add(fpsToggle);
                children.add(cpsToggle);
                selectables.add(fpsToggle);
                selectables.add(cpsToggle);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x44AA88AA);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§dFPS / CPS:", x + 6, y + 5, 0xFFFFFF);
                fpsToggle.setX(x + 100);
                fpsToggle.setY(y + 1);
                fpsToggle.render(matrices, mouseX, mouseY, delta);
                cpsToggle.setX(x + 180);
                cpsToggle.setY(y + 1);
                cpsToggle.render(matrices, mouseX, mouseY, delta);
                String posInfo = "§7(Fps: " + config.fpsX + "," + config.fpsY + "  Cps: " + config.cpsX + "," + config.cpsY + ")";
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, posInfo, x + 260, y + 5, 0xAAAAAA);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return selectables; }
        }

        private class DisplayTogglesEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget manaBtn;
            private final ButtonWidget damageBtn;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();

            public DisplayTogglesEntry(StorageConfig config) {
                this.config = config;

                this.manaBtn = ButtonWidget.builder(
                        Text.literal(config.showMana ? "§aBE" : "§cKI"),
                        btn -> {
                            config.showMana = !config.showMana;
                            btn.setMessage(Text.literal(config.showMana ? "§aBE" : "§cKI"));
                            config.save();
                        }
                ).dimensions(0, 0, 45, 18).build();

                this.damageBtn = ButtonWidget.builder(
                        Text.literal(config.showDamage ? "§aBE" : "§cKI"),
                        btn -> {
                            config.showDamage = !config.showDamage;
                            btn.setMessage(Text.literal(config.showDamage ? "§aBE" : "§cKI"));
                            config.save();
                        }
                ).dimensions(0, 0, 45, 18).build();

                this.children.add(this.manaBtn);
                this.children.add(this.damageBtn);
                this.selectables.add(this.manaBtn);
                this.selectables.add(this.damageBtn);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x445555AA);

                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§bMana:", x + 6, y + 5, 0xFFFFFF);
                this.manaBtn.setX(x + 50);
                this.manaBtn.setY(y + 1);
                this.manaBtn.render(matrices, mouseX, mouseY, delta);

                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§cSebzés:", x + 180, y + 5, 0xFFFFFF);
                this.damageBtn.setX(x + 235);
                this.damageBtn.setY(y + 1);
                this.damageBtn.render(matrices, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }
        }

        private class NoRenderInactiveEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget toggleBtn;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();

            public NoRenderInactiveEntry(StorageConfig config) {
                this.config = config;
                this.toggleBtn = ButtonWidget.builder(
                        Text.literal(config.noRenderOnInactive ? "§aBE" : "§cKI"),
                        btn -> {
                            config.noRenderOnInactive = !config.noRenderOnInactive;
                            btn.setMessage(Text.literal(config.noRenderOnInactive ? "§aBE" : "§cKI"));
                            config.save();
                        }
                ).dimensions(0, 0, 45, 18).build();

                this.children.add(this.toggleBtn);
                this.selectables.add(this.toggleBtn);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x44AA5500);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§6NoRender Ablak Inaktívnál:", x + 6, y + 5, 0xFFFFFF);
                this.toggleBtn.setX(x + entryWidth - 55);
                this.toggleBtn.setY(y + 1);
                this.toggleBtn.render(matrices, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }
        }

        private class FpsLimitEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget toggleBtn;
            private final ButtonWidget minMinusBtn, minPlusBtn;
            private final ButtonWidget secMinusBtn, secPlusBtn;
            private final net.minecraft.client.gui.widget.TextFieldWidget fpsTextField;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();

            public FpsLimitEntry(StorageConfig config) {
                this.config = config;

                this.toggleBtn = ButtonWidget.builder(Text.literal(config.fpsLimitOnInactive ? "§aBE" : "§cKI"), btn -> {
                    config.fpsLimitOnInactive = !config.fpsLimitOnInactive;
                    btn.setMessage(Text.literal(config.fpsLimitOnInactive ? "§aBE" : "§cKI"));
                    config.save();
                }).dimensions(0, 0, 32, 18).build();

                this.minMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> {
                    if (config.fpsLimitMinutes > 0) config.fpsLimitMinutes--;
                    config.save();
                }).dimensions(0, 0, 16, 18).build();

                this.minPlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> {
                    if (config.fpsLimitMinutes < 60) config.fpsLimitMinutes++;
                    config.save();
                }).dimensions(0, 0, 16, 18).build();

                this.secMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> {
                    if (config.fpsLimitSeconds >= 5) config.fpsLimitSeconds -= 5;
                    config.save();
                }).dimensions(0, 0, 16, 18).build();

                this.secPlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> {
                    if (config.fpsLimitSeconds < 55) config.fpsLimitSeconds += 5;
                    config.save();
                }).dimensions(0, 0, 16, 18).build();

                this.fpsTextField = new net.minecraft.client.gui.widget.TextFieldWidget(
                        StorageConfigScreen.this.textRenderer, 0, 0, 40, 16, Text.literal("FPS")
                );
                this.fpsTextField.setText(String.valueOf(config.fpsLimitValue));
                this.fpsTextField.setChangedListener(text -> {
                    try {
                        int value = Integer.parseInt(text);
                        if (value >= 1 && value <= 260) {
                            config.fpsLimitValue = value;
                            config.save();
                            this.fpsTextField.setEditableColor(0xFFFFFF);
                        } else {
                            this.fpsTextField.setEditableColor(0xFF5555);
                        }
                    } catch (NumberFormatException e) {
                        this.fpsTextField.setEditableColor(0xFF5555);
                    }
                });

                addButton(toggleBtn);
                addButton(minMinusBtn);
                addButton(minPlusBtn);
                addButton(secMinusBtn);
                addButton(secPlusBtn);
                this.children.add(this.fpsTextField);
            }

            private void addButton(ButtonWidget btn) {
                this.children.add(btn);
                this.selectables.add(btn);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x44AA5500);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§6Auto FPS Limit Idle:", x + 6, y + 5, 0xFFFFFF);

                this.toggleBtn.setX(x + 130);
                this.toggleBtn.setY(y + 1);
                this.toggleBtn.render(matrices, mouseX, mouseY, delta);

                this.minMinusBtn.setX(x + 170);
                this.minMinusBtn.setY(y + 1);
                this.minMinusBtn.render(matrices, mouseX, mouseY, delta);

                String minText = config.fpsLimitMinutes + "m";
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, minText, x + 190, y + 5, 0xFFFF55);

                this.minPlusBtn.setX(x + 212);
                this.minPlusBtn.setY(y + 1);
                this.minPlusBtn.render(matrices, mouseX, mouseY, delta);

                this.secMinusBtn.setX(x + 235);
                this.secMinusBtn.setY(y + 1);
                this.secMinusBtn.render(matrices, mouseX, mouseY, delta);

                String secText = config.fpsLimitSeconds + "s";
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, secText, x + 255, y + 5, 0xFFFF55);

                this.secPlusBtn.setX(x + 277);
                this.secPlusBtn.setY(y + 1);
                this.secPlusBtn.render(matrices, mouseX, mouseY, delta);

                this.fpsTextField.setX(x + 305);
                this.fpsTextField.setY(y + 1);
                this.fpsTextField.render(matrices, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.fpsTextField.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (this.fpsTextField.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean charTyped(char chr, int modifiers) {
                if (this.fpsTextField.charTyped(chr, modifiers)) {
                    return true;
                }
                return super.charTyped(chr, modifiers);
            }
        }

        private class FpsLimitImmediateEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget toggleBtn;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();

            public FpsLimitImmediateEntry(StorageConfig config) {
                this.config = config;
                this.toggleBtn = ButtonWidget.builder(
                        Text.literal(config.fpsLimitImmediateOnInactive ? "§aBE" : "§cKI"),
                        btn -> {
                            config.fpsLimitImmediateOnInactive = !config.fpsLimitImmediateOnInactive;
                            btn.setMessage(Text.literal(config.fpsLimitImmediateOnInactive ? "§aBE" : "§cKI"));
                            config.save();
                        }
                ).dimensions(0, 0, 45, 18).build();

                this.children.add(this.toggleBtn);
                this.selectables.add(this.toggleBtn);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x44AA5500);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§6FPS Limit Ablak Inaktívnál:", x + 6, y + 5, 0xFFFFFF);
                this.toggleBtn.setX(x + entryWidth - 55);
                this.toggleBtn.setY(y + 1);
                this.toggleBtn.render(matrices, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }
        }

        private class HideBlockBreakingParticlesEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget toggleBtn;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();

            public HideBlockBreakingParticlesEntry(StorageConfig config) {
                this.config = config;
                this.toggleBtn = ButtonWidget.builder(
                        Text.literal(config.hideBlockBreakingParticles ? "§cKI" : "§aBE"),
                        btn -> {
                            config.hideBlockBreakingParticles = !config.hideBlockBreakingParticles;
                            btn.setMessage(Text.literal(config.hideBlockBreakingParticles ? "§cKI" : "§aBE"));
                            config.save();
                        }
                ).dimensions(0, 0, 45, 18).build();

                this.children.add(this.toggleBtn);
                this.selectables.add(this.toggleBtn);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x44AA5500);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§6Block Break Részecskék:", x + 6, y + 5, 0xFFFFFF);
                this.toggleBtn.setX(x + entryWidth - 55);
                this.toggleBtn.setY(y + 1);
                this.toggleBtn.render(matrices, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }
        }

        private class FireOverlayEntry extends Entry {
            private final StorageConfig config;
            private final ButtonWidget toggleBtn;
            private final ButtonWidget settingsBtn;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();

            public FireOverlayEntry(StorageConfig config) {
                this.config = config;

                this.toggleBtn = ButtonWidget.builder(
                        Text.literal(config.hideFireOverlay ? "§cKI" : "§aBE"),
                        btn -> {
                            config.hideFireOverlay = !config.hideFireOverlay;
                            btn.setMessage(Text.literal(config.hideFireOverlay ? "§cKI" : "§aBE"));
                            config.save();
                        }
                ).dimensions(0, 0, 32, 18).build();

                this.settingsBtn = ButtonWidget.builder(Text.literal("Beállítás"), btn -> {
                    if (StorageConfigScreen.this.client != null) {
                        StorageConfigScreen.this.client.setScreen(new FireOverlayConfigScreen(config, StorageConfigScreen.this));
                    }
                }).dimensions(0, 0, 60, 18).build();

                addButton(toggleBtn);
                addButton(settingsBtn);
            }

            private void addButton(ButtonWidget btn) {
                this.children.add(btn);
                this.selectables.add(btn);
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x44CC5500);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§cTűz Overlay:", x + 6, y + 5, 0xFFFFFF);

                this.toggleBtn.setX(x + 130);
                this.toggleBtn.setY(y + 1);
                this.toggleBtn.render(matrices, mouseX, mouseY, delta);

                this.settingsBtn.setX(x + 170);
                this.settingsBtn.setY(y + 1);
                this.settingsBtn.render(matrices, mouseX, mouseY, delta);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }
        }

        // ─── MÓDOSÍTOTT RÉSZ: 10 gomb (0 = offhand) ───
        private class HudSlotsToggleEntry extends Entry {
            private final List<ButtonWidget> buttons = new ArrayList<>();
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();

            public HudSlotsToggleEntry(StorageConfig config) {
                // 10 gomb: 0 (offhand) + 1..9 (hotbar)
                for (int i = 0; i < 10; i++) {
                    final int index = i;
                    ButtonWidget btn;
                    if (i == 0) {
                        // 0. gomb → offhandSlotEnabled
                        boolean isEnabled = config.offhandSlotEnabled;
                        btn = ButtonWidget.builder(Text.literal("0"), button -> {
                            config.offhandSlotEnabled = !config.offhandSlotEnabled;
                            button.setMessage(Text.literal((config.offhandSlotEnabled ? "§a" : "§c") + "0"));
                            config.save();
                        }).dimensions(0, 0, 16, 18).build();
                        btn.setMessage(Text.literal((isEnabled ? "§a" : "§c") + "0"));
                    } else {
                        // 1..9 → hudSlotsEnabled[slotIndex-1]
                        final int slotIndex = i - 1;
                        boolean isEnabled = config.hudSlotsEnabled[slotIndex];
                        btn = ButtonWidget.builder(Text.literal(String.valueOf(i)), button -> {
                            config.hudSlotsEnabled[slotIndex] = !config.hudSlotsEnabled[slotIndex];
                            String num = button.getMessage().getString().replaceAll("§.", "");
                            button.setMessage(Text.literal((config.hudSlotsEnabled[slotIndex] ? "§a" : "§c") + num));
                            config.save();
                        }).dimensions(0, 0, 16, 18).build();
                        btn.setMessage(Text.literal((isEnabled ? "§a" : "§c") + String.valueOf(i)));
                    }
                    buttons.add(btn);
                    children.add(btn);
                }
            }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x4422AAAA);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, "§eHUD Slotok (0=offhand):", x + 6, y + 5, 0xFFFFFF);

                // 10 gombhoz szélesebb hely (10*18 = 180 + kis margó)
                int startX = x + entryWidth - 185;
                for (int i = 0; i < buttons.size(); i++) {
                    ButtonWidget btn = buttons.get(i);
                    btn.setX(startX + (i * 18));
                    btn.setY(y + 1);
                    btn.render(matrices, mouseX, mouseY, delta);
                }
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.buttons; }
        }

        // ─── A többi belső osztály változatlan marad ───

        public class NormalItemEntry extends Entry {
            public final StorageConfig.ItemPreset preset;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();

            public NormalItemEntry(StorageConfig.ItemPreset preset) { this.preset = preset; }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                int bgColor = hovered ? 0x44FFFFFF : 0x22FFFFFF;
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, bgColor);

                String label = preset.name + " §7(CMD: " + preset.customModelData + ")";
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, label, x + 6, y + 6, 0xE0E0E0);
                String info = "§7B: §e" + (preset.leftCD / 20.0f) + "s §7| J: §d" + (preset.rightCD / 20.0f) + "s";
                int infoWidth = StorageConfigScreen.this.textRenderer.getWidth(info);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, info, x + entryWidth - infoWidth - 6, y + 6, 0xFFFFFF);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return Collections.emptyList(); }
        }

        public class LeveledItemHeaderEntry extends Entry {
            public final StorageConfig.LeveledItemPreset preset;
            private final ButtonWidget toggleButton;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();
            private final List<ButtonWidget> selectables = new ArrayList<>();
            private boolean expanded = false;

            public LeveledItemHeaderEntry(StorageConfig.LeveledItemPreset preset, ItemPresetListWidget parent) {
                this.preset = preset;
                this.toggleButton = ButtonWidget.builder(Text.literal("►"), button -> {
                    this.expanded = !this.expanded;
                    button.setMessage(Text.literal(this.expanded ? "▼" : "►"));
                    parent.toggleLevels(this, this.preset);
                }).dimensions(0, 0, 20, 18).build();
                this.children.add(this.toggleButton);
                this.selectables.add(this.toggleButton);
            }

            public boolean isExpanded() { return expanded; }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight - 2, 0x44AA00AA);
                this.toggleButton.setX(x + 4);
                this.toggleButton.setY(y + 1);
                this.toggleButton.render(matrices, mouseX, mouseY, delta);
                String label = "§6" + preset.name + " §7(CMD: " + preset.customModelData + ") §d[Fejleszthető]";
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, label, x + 28, y + 5, 0xFFFFFF);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return this.selectables; }
        }

        public class LevelSubEntry extends Entry {
            public final StorageConfig.ItemLevel level;
            private final List<net.minecraft.client.gui.Element> children = new ArrayList<>();

            public LevelSubEntry(StorageConfig.ItemLevel level) { this.level = level; }

            @Override
            public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
                int bgColor = hovered ? 0x44000000 : 0x22000000;
                fill(matrices, x + 15, y, x + entryWidth, y + entryHeight - 2, bgColor);
                fill(matrices, x + 8, y, x + 9, y + entryHeight, 0x44FFFFFF);
                fill(matrices, x + 8, y + (entryHeight / 2), x + 14, y + (entryHeight / 2) + 1, 0x44FFFFFF);
                String lvlLabel = "§b• " + level.levelNameIndicator;
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, lvlLabel, x + 20, y + 6, 0xE0E0E0);
                String stats = "§7B: §e" + (level.leftCD / 20.0f) + "s (" + level.leftMana + "m) §7| J: §d" + (level.rightCD / 20.0f) + "s (" + level.rightMana + "m)";
                int statsWidth = StorageConfigScreen.this.textRenderer.getWidth(stats);
                StorageConfigScreen.this.textRenderer.drawWithShadow(matrices, stats, x + entryWidth - statsWidth - 6, y + 6, 0xCCCCCC);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() { return this.children; }
            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() { return Collections.emptyList(); }
        }
    }
}