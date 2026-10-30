package me.Gabix.hud;

import me.Gabix.core.MinecraftSettingsApplier;
import me.Gabix.core.StorageConfig;
import me.Gabix.developer.DevAccess;
import me.Gabix.fireoverlay.screen.FireOverlayConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
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

    private static final String DEV_CODE   = "gabixvagyok";
    private static final String MC_CODE    = "marcsi";
    private static final String BLOCK_CODE = "seged";

    private static final int BUFFER_LIMIT = 32;
    private static final int TAB_WIDTH = 120;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_Y = 30;

    private static boolean developerUnlocked    = false;
    private static boolean mcButtonUnlocked      = false;
    private static boolean blockTexturesUnlocked = false;

    private enum Category {
        GENERAL("Általános", 0xFFFFD060),
        PERFORMANCE("Teljesítmény", 0xFFFF9040),
        VISUAL("Vizuális", 0xFFD060FF),
        DEVELOPER("Developer", 0xFFFF4040);

        final String name;
        final int accent;
        Category(String name, int accent) { this.name = name; this.accent = accent; }
    }

    private final StorageConfig config;
    private final StringBuilder secretBuffer = new StringBuilder();
    private Category currentCategory = Category.GENERAL;
    private ItemPresetListWidget listWidget;

    public StorageConfigScreen(StorageConfig config) {
        super(Text.literal("Actionbar Storage Beállítások"));
        this.config = config;
    }

    private static List<Category> visibleCategories() {
        List<Category> list = new ArrayList<>();
        for (Category c : Category.values()) {
            if (c == Category.DEVELOPER && !developerUnlocked) continue;
            list.add(c);
        }
        return list;
    }

    @Override
    protected void init() {
        if (config.hideBlockTextures) blockTexturesUnlocked = true;

        List<Category> visible = visibleCategories();
        if (!visible.contains(currentCategory)) currentCategory = Category.GENERAL;

        listWidget = new ItemPresetListWidget(config, currentCategory);
        addDrawableChild(listWidget);

        int bottomY = height - 26;
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§bHUD Elemek Áthelyezése"),
                b -> { if (client != null) client.setScreen(new HudPositionEditorScreen(config, this)); }
        ).dimensions(width / 2 - 155, bottomY, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("§cBezárás"),
                b -> { config.save(); close(); }
        ).dimensions(width / 2 + 5, bottomY, 150, 20).build());
    }

    private void switchCategory(Category cat) {
        if (currentCategory == cat) return;
        currentCategory = cat;
        this.clearAndInit();
    }

    private void applyMcSettings() {
        if (client != null) MinecraftSettingsApplier.applyPreset(client);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!(getFocused() instanceof TextFieldWidget) && !Character.isISOControl(chr)) {
            secretBuffer.append(Character.toLowerCase(chr));
            if (secretBuffer.length() > BUFFER_LIMIT)
                secretBuffer.delete(0, secretBuffer.length() - BUFFER_LIMIT);

            String buf = secretBuffer.toString();
            boolean matched = false;

            if (!developerUnlocked && buf.endsWith(DEV_CODE)) {
                developerUnlocked = true;
                DevAccess.setToolsEnabled(true);
                currentCategory = Category.DEVELOPER;
                matched = true;
                notifyUnlock("Developer mód");
            } else if (DevAccess.isToolsEnabled()) {
                if (!mcButtonUnlocked && buf.endsWith(MC_CODE)) {
                    mcButtonUnlocked = true;
                    matched = true;
                    notifyUnlock("MC Beállítások");
                } else if (!blockTexturesUnlocked && buf.endsWith(BLOCK_CODE)) {
                    blockTexturesUnlocked = true;
                    matched = true;
                    notifyUnlock("Block Textúrák");
                }
            }

            if (matched) {
                secretBuffer.setLength(0);
                clearAndInit();
            }
        }
        return super.charTyped(chr, modifiers);
    }

    private void notifyUnlock(String what) {
        if (client != null && client.player != null) {
            client.player.sendMessage(
                    Text.literal("§a§l[✓] §r§a" + what + " feloldva!"), false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Category> visible = visibleCategories();
        int total = TAB_WIDTH * visible.size();
        int startX = (width - total) / 2;

        if (button == 0 && mouseY >= TAB_Y && mouseY <= TAB_Y + TAB_HEIGHT) {
            for (int i = 0; i < visible.size(); i++) {
                int tx = startX + i * TAB_WIDTH;
                if (mouseX >= tx && mouseX <= tx + TAB_WIDTH - 2) {
                    switchCategory(visible.get(i));
                    return true;
                }
            }
        }

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
        if (entry instanceof ItemPresetListWidget.NormalItemEntry e)
            return giveCommand(e.preset.itemId, e.preset.customModelData, e.preset.name);
        if (entry instanceof ItemPresetListWidget.LevelSubEntry e) {
            var parent = findParent(e.level);
            return parent == null ? null
                    : giveCommand(parent.itemId, parent.customModelData, parent.name + " " + e.level.levelNameIndicator);
        }
        if (entry instanceof ItemPresetListWidget.LeveledItemHeaderEntry e)
            return giveCommand(e.preset.itemId, e.preset.customModelData, e.preset.name);
        return null;
    }

    private StorageConfig.LeveledItemPreset findParent(StorageConfig.ItemLevel level) {
        if (config.leveledItems == null) return null;
        for (var preset : config.leveledItems)
            if (preset.levels != null && preset.levels.contains(level)) return preset;
        return null;
    }

    private static String giveCommand(String itemId, int cmd, String name) {
        String escaped = name
                .replace("\\", "\\\\").replace("'", "\\'")
                .replace("\"", "\\\"").replace("§", "\\u00A7");
        return "give @s " + itemId + "{CustomModelData:" + cmd
                + ",display:{Name:'{\"text\":\"" + escaped + "\",\"italic\":false}'}}";
    }

    private static ButtonWidget toggleButton(int w, BooleanSupplier getter,
                                             Consumer<Boolean> setter,
                                             String onLabel, String offLabel) {
        return ButtonWidget.builder(
                Text.literal(getter.getAsBoolean() ? onLabel : offLabel),
                b -> {
                    boolean next = !getter.getAsBoolean();
                    setter.accept(next);
                    b.setMessage(Text.literal(next ? onLabel : offLabel));
                }
        ).dimensions(0, 0, w, 18).build();
    }

    private static ButtonWidget actionButton(int w, String label, Runnable action) {
        return ButtonWidget.builder(Text.literal(label), b -> action.run())
                .dimensions(0, 0, w, 18).build();
    }

    @Override
    public void render(MatrixStack m, int mx, int my, float d) {
        super.render(m, mx, my, d);
        drawCenteredTextWithShadow(m, textRenderer, title, width / 2, 8, 0xFFFFFF);

        List<Category> visible = visibleCategories();
        int total = TAB_WIDTH * visible.size();
        int startX = (width - total) / 2;

        for (int i = 0; i < visible.size(); i++) {
            Category cat = visible.get(i);
            boolean active = (cat == currentCategory);
            boolean hovered = !active && mx >= startX + i * TAB_WIDTH && mx <= startX + (i + 1) * TAB_WIDTH - 2
                    && my >= TAB_Y && my <= TAB_Y + TAB_HEIGHT;
            int tx = startX + i * TAB_WIDTH;

            int bgColor = active ? 0x66000000 : (hovered ? 0x44FFFFFF : 0x22000000);
            fill(m, tx, TAB_Y, tx + TAB_WIDTH - 2, TAB_Y + TAB_HEIGHT, bgColor);
            fill(m, tx, TAB_Y, tx + TAB_WIDTH - 2, TAB_Y + 1, 0x44FFFFFF);

            String text = (active ? "§l" : "") + cat.name;
            int color = active ? cat.accent : (hovered ? 0xFFFFFF : 0xAAAAAA);
            int tw = textRenderer.getWidth(text);
            textRenderer.drawWithShadow(m, Text.literal(text), tx + (TAB_WIDTH - 2 - tw) / 2, TAB_Y + (TAB_HEIGHT - 8) / 2, color);

            if (active) {
                fill(m, tx, TAB_Y + TAB_HEIGHT, tx + TAB_WIDTH - 2, TAB_Y + TAB_HEIGHT + 2, cat.accent | 0xFF000000);
            }
        }
    }

    private class ItemPresetListWidget extends ElementListWidget<ItemPresetListWidget.Entry> {

        ItemPresetListWidget(StorageConfig config, Category cat) {
            super(StorageConfigScreen.this.client, StorageConfigScreen.this.width,
                    StorageConfigScreen.this.height, 55,
                    StorageConfigScreen.this.height - 34, 24);
            buildEntries(config, cat);
        }

        private void buildEntries(StorageConfig config, Category cat) {
            switch (cat) {
                case GENERAL -> {
                    addEntry(new SectionHeader("Boss", 0xFFFF8080));
                    addEntry(new AutoBossDetectionEntry(config));
                    addEntry(new BossNotificationEntry(config));
                    addEntry(new SectionHeader("Kijelzés", 0xFF60C0FF));
                    addEntry(new DisplayTogglesEntry(config));
                    addEntry(new FpsCpsToggleEntry(config));
                    addEntry(new ArrowTotemToggleEntry(config));
                    addEntry(new SectionHeader("HUD Slotok", 0xFF80FF80));
                    addEntry(new HudSlotsToggleEntry(config));
                }
                case PERFORMANCE -> {
                    addEntry(new SectionHeader("Inaktív ablak", 0xFFFFA040));
                    addEntry(new NoRenderInactiveEntry(config));
                    addEntry(new MuteOnInactiveEntry(config));
                    addEntry(new PauseHudOnInactiveEntry(config));
                    addEntry(new PauseBossMonitorOnInactiveEntry(config));
                    addEntry(new PauseChunkLoadingOnInactiveEntry(config));
                    addEntry(new DownscaleOnInactiveEntry(config));
                    addEntry(new SectionHeader("FPS Limit", 0xFFFF6060));
                    addEntry(new FpsLimitEntry(config));
                    addEntry(new FpsLimitImmediateEntry(config));
                    addEntry(new SectionHeader("Egyéb", 0xFF60C0FF));
                    addEntry(new MaxFpsEntry(config));
                    addEntry(new VsyncEntry(config));
                }
                case VISUAL -> {
                    addEntry(new SectionHeader("Részecskék", 0xFFD060FF));
                    addEntry(new HideBlockBreakingParticlesEntry(config));
                    addEntry(new HideAllParticlesEntry(config));
                    addEntry(new SectionHeader("Egyéb", 0xFFFF8040));
                    addEntry(new FireOverlayEntry(config));
                    addEntry(new SectionHeader("Title / HUD effektek", 0xFF60C0FF));
                    addEntry(new HidePuaTitleSubtitleEntry(config));
                }
                case DEVELOPER -> {
                    addEntry(new SectionHeader("Fejlesztői eszközök", 0xFFFF4040));
                    addEntry(new DevToolsMasterEntry(config));

                    if (DevAccess.isToolsEnabled()) {
                        if (mcButtonUnlocked) addEntry(new McSettingsApplyEntry(config));
                        if (blockTexturesUnlocked) addEntry(new HideBlockTexturesEntry(config));
                        if (!mcButtonUnlocked && !blockTexturesUnlocked) {
                            addEntry(new HintEntry("Gépeld be: §emarcsi §7/ §eseged §7a funkciókéhoz"));
                        }
                        addEntry(new DebugInfoEntry(config));
                    } else {
                        addEntry(new HintEntry("§cDev eszközök kikapcsolva. §7A H/J/I/O/U billentyűk inaktívak."));
                    }

                    boolean hasNormal = config.normalItems != null && !config.normalItems.isEmpty();
                    boolean hasLeveled = config.leveledItems != null && !config.leveledItems.isEmpty();

                    if (hasNormal) {
                        addEntry(new SectionHeader("Normál Itemek", 0xFF60FF60));
                        config.normalItems.forEach(p -> addEntry(new NormalItemEntry(p)));
                    }
                    if (hasLeveled) {
                        addEntry(new SectionHeader("Fejleszthető Itemek", 0xFF60C0FF));
                        config.leveledItems.forEach(p -> addEntry(new LeveledItemHeaderEntry(p, this)));
                    }
                    if (!hasNormal && !hasLeveled) {
                        addEntry(new EmptyEntry("Nincsenek item presetek"));
                    }
                }
            }
        }

        Entry getHoveredEntry(double x, double y) { return getEntryAtPosition(x, y); }
        int getLeft()   { return left; }
        int getRight()  { return right; }
        int getTop()    { return top; }
        int getBottom() { return bottom; }

        void toggleLevels(LeveledItemHeaderEntry header, StorageConfig.LeveledItemPreset preset) {
            if (preset.levels == null || preset.levels.isEmpty()) return;
            var idx = children().indexOf(header);
            if (idx == -1) return;
            if (header.isExpanded()) {
                int offset = 1;
                for (var level : preset.levels)
                    children().add(idx + offset++, new LevelSubEntry(level));
            } else {
                for (int i = 0; i < preset.levels.size(); i++)
                    if (idx + 1 < children().size() && children().get(idx + 1) instanceof LevelSubEntry)
                        children().remove(idx + 1);
            }
        }

        @Override public int getRowWidth() { return 380; }
        @Override protected int getScrollbarPositionX() { return width / 2 + 200; }

        abstract static class Entry extends ElementListWidget.Entry<Entry> {}

        abstract class BaseEntry extends Entry {
            protected final List<Element> children = new ArrayList<>();
            protected final List<Selectable> selectables = new ArrayList<>();

            @Override public List<? extends Element> children() { return children; }
            @Override public List<? extends Selectable> selectableChildren() { return selectables; }

            protected <T extends ButtonWidget> T track(T b) {
                children.add(b); selectables.add(b); return b;
            }

            protected void fillBg(MatrixStack m, int x, int y, int w, int h, int color) {
                fill(m, x, y, x + w, y + h - 2, color);
                fill(m, x, y, x + w, y + 1, (color & 0x00FFFFFF) | 0x66000000);
                fill(m, x, y + h - 3, x + w, y + h - 2, 0x55000000);
            }

            protected void label(MatrixStack m, String t, int x, int y, int c) {
                StorageConfigScreen.this.textRenderer.drawWithShadow(m, Text.literal(t), x, y, c);
            }
            protected void label(MatrixStack m, String t, int x, int y) { label(m, t, x, y, 0xFFFFFF); }
        }

        class SectionHeader extends Entry {
            final String title; final int color;
            SectionHeader(String title, int color) { this.title = title; this.color = color; }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fill(m, x, y + 4, x + w, y + h - 6, 0x33000000);
                fill(m, x, y + 4, x + 4, y + h - 6, color | 0xFF000000);
                StorageConfigScreen.this.textRenderer.drawWithShadow(m,
                        Text.literal("§l" + title), x + 12, y + 8, 0xFFFFFF);
            }
            @Override public List<? extends Element> children() { return List.of(); }
            @Override public List<? extends Selectable> selectableChildren() { return List.of(); }
        }

        class EmptyEntry extends Entry {
            final String msg;
            EmptyEntry(String msg) { this.msg = msg; }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                drawCenteredTextWithShadow(m, StorageConfigScreen.this.textRenderer,
                        Text.literal("§7" + msg), x + w / 2, y + h / 2 - 4, 0xAAAAAA);
            }
            @Override public List<? extends Element> children() { return List.of(); }
            @Override public List<? extends Selectable> selectableChildren() { return List.of(); }
        }

        class HintEntry extends Entry {
            final String msg;
            HintEntry(String msg) { this.msg = msg; }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fill(m, x, y, x + w, y + h - 2, 0x33000000);
                StorageConfigScreen.this.textRenderer.drawWithShadow(m,
                        Text.literal("§7§o" + msg), x + 10, y + 6, 0xAAAAAA);
            }
            @Override public List<? extends Element> children() { return List.of(); }
            @Override public List<? extends Selectable> selectableChildren() { return List.of(); }
        }

        class DevToolsMasterEntry extends BaseEntry {
            private final ButtonWidget toggleBtn;
            DevToolsMasterEntry(StorageConfig c) {
                toggleBtn = track(toggleButton(60,
                        () -> DevAccess.isToolsEnabled(),
                        v -> {
                            DevAccess.setToolsEnabled(v);
                            StorageConfigScreen.this.clearAndInit();
                        },
                        "§aBE", "§cKI"));
            }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, DevAccess.isToolsEnabled() ? 0x5540FF40 : 0x55FF4040);
                label(m, "§e§lDev Eszközök használata", x + 10, y + 5);
                toggleBtn.setX(x + w - 70);
                toggleBtn.setY(y + 1);
                toggleBtn.render(m, mx, my, d);
            }
        }

        private class AutoBossDetectionEntry extends BaseEntry {
            private final ButtonWidget btn;
            AutoBossDetectionEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.autoBossDetection,
                        v -> { c.autoBossDetection = v; c.save(); },
                        "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x4400AAAA);
                label(m, "§bAuto Boss Felismerés", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class BossNotificationEntry extends BaseEntry {
            private final ButtonWidget cycle;
            BossNotificationEntry(StorageConfig c) {
                cycle = track(ButtonWidget.builder(Text.literal(c.selectedBossNotification), b -> {
                    int cur = 0;
                    for (int idx = 0; idx < BOSS_CHOICES.length; idx++)
                        if (BOSS_CHOICES[idx].equals(c.selectedBossNotification)) { cur = idx; break; }
                    c.selectedBossNotification = BOSS_CHOICES[(cur + 1) % BOSS_CHOICES.length];
                    b.setMessage(Text.literal(c.selectedBossNotification));
                    c.save();
                }).dimensions(0, 0, 110, 18).build());
            }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x4400AAAA);
                label(m, "§bBoss Értesítő", x + 10, y + 5);
                cycle.setX(x + w - 116); cycle.setY(y + 1); cycle.render(m, mx, my, d);
            }
        }

        private class DisplayTogglesEntry extends BaseEntry {
            private final ButtonWidget mana, dmg;
            DisplayTogglesEntry(StorageConfig c) {
                mana = track(toggleButton(50,
                        () -> c.showMana, v -> { c.showMana = v; c.save(); }, "§aBE", "§cKI"));
                dmg  = track(toggleButton(50,
                        () -> c.showDamage, v -> { c.showDamage = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x445555AA);
                label(m, "§bMana", x + 10, y + 5);
                mana.setX(x + 60); mana.setY(y + 1); mana.render(m, mx, my, d);
                label(m, "§cSebzés", x + 190, y + 5);
                dmg.setX(x + 240); dmg.setY(y + 1); dmg.render(m, mx, my, d);
            }
        }

        private class FpsCpsToggleEntry extends BaseEntry {
            private final ButtonWidget fps, cps;
            FpsCpsToggleEntry(StorageConfig c) {
                fps = track(toggleButton(80,
                        () -> c.showFps, v -> { c.showFps = v; c.save(); }, "§aFPS BE", "§cFPS KI"));
                cps = track(toggleButton(80,
                        () -> c.showCps, v -> { c.showCps = v; c.save(); }, "§aCPS BE", "§cCPS KI"));
            }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                var cfg = StorageConfigScreen.this.config;
                fillBg(m, x, y, w, h, 0x44AA88AA);
                label(m, "§dFPS / CPS", x + 10, y + 5);
                fps.setX(x + 90); fps.setY(y + 1); fps.render(m, mx, my, d);
                cps.setX(x + 175); cps.setY(y + 1); cps.render(m, mx, my, d);
                label(m, "§7(" + cfg.fpsX + "," + cfg.fpsY + " | " + cfg.cpsX + "," + cfg.cpsY + ")",
                        x + 260, y + 5, 0xAAAAAA);
            }
        }

        private class ArrowTotemToggleEntry extends BaseEntry {
            private final ButtonWidget arrows, totems;
            ArrowTotemToggleEntry(StorageConfig c) {
                arrows = track(toggleButton(80,
                        () -> c.showArrowAps, v -> { c.showArrowAps = v; c.save(); }, "§aNYÍL BE", "§cNYÍL KI"));
                totems = track(toggleButton(80,
                        () -> c.showTotems, v -> { c.showTotems = v; c.save(); }, "§aTOTEM BE", "§cTOTEM KI"));
            }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                var cfg = StorageConfigScreen.this.config;
                fillBg(m, x, y, w, h, 0x44AAAA88);
                label(m, "§eNyíl/mp / Totem", x + 10, y + 5);
                arrows.setX(x + 90); arrows.setY(y + 1); arrows.render(m, mx, my, d);
                totems.setX(x + 175); totems.setY(y + 1); totems.render(m, mx, my, d);
                label(m, "§7(" + cfg.arrowX + "," + cfg.arrowY + " | " + cfg.totemX + "," + cfg.totemY + ")",
                        x + 260, y + 5, 0xAAAAAA);
            }
        }

        private class HudSlotsToggleEntry extends BaseEntry {
            private final List<ButtonWidget> slots = new ArrayList<>();
            HudSlotsToggleEntry(StorageConfig c) {
                for (int i = 0; i < 10; i++) {
                    final int idx = i;
                    ButtonWidget b = (i == 0)
                            ? toggleButton(18,
                            () -> c.offhandSlotEnabled,
                            v -> { c.offhandSlotEnabled = v; c.save(); },
                            "§a0", "§c0")
                            : toggleButton(18,
                            () -> c.hudSlotsEnabled[idx - 1],
                            v -> { c.hudSlotsEnabled[idx - 1] = v; c.save(); },
                            "§a" + idx, "§c" + idx);
                    slots.add(b); track(b);
                }
            }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x4422AAAA);
                label(m, "§eHUD Slotok (0 = offhand)", x + 10, y + 5);
                int sx = x + w - 200;
                for (int j = 0; j < slots.size(); j++) {
                    var b = slots.get(j);
                    b.setX(sx + j * 20); b.setY(y + 1); b.render(m, mx, my, d);
                }
            }
        }

        private class NoRenderInactiveEntry extends BaseEntry {
            private final ButtonWidget btn;
            NoRenderInactiveEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.noRenderOnInactive, v -> { c.noRenderOnInactive = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6NoRender inaktív ablaknál", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class MuteOnInactiveEntry extends BaseEntry {
            private final ButtonWidget btn;
            MuteOnInactiveEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.muteOnInactive, v -> { c.muteOnInactive = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6Némítás inaktív ablaknál", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class PauseHudOnInactiveEntry extends BaseEntry {
            private final ButtonWidget btn;
            PauseHudOnInactiveEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.pauseHudOnInactive,
                        v -> { c.pauseHudOnInactive = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6HUD renderelés szüneteltetése inaktívkor", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class PauseBossMonitorOnInactiveEntry extends BaseEntry {
            private final ButtonWidget btn;
            PauseBossMonitorOnInactiveEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.pauseBossMonitorOnInactive,
                        v -> { c.pauseBossMonitorOnInactive = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6BossMonitor tick szüneteltetése inaktívkor", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class PauseChunkLoadingOnInactiveEntry extends BaseEntry {
            private final ButtonWidget btn;
            PauseChunkLoadingOnInactiveEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.pauseChunkLoadingOnInactive,
                        v -> { c.pauseChunkLoadingOnInactive = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6Chunk mesh építés szüneteltetése inaktívkor", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class DownscaleOnInactiveEntry extends BaseEntry {
            private final ButtonWidget toggleBtn;
            private final RenderScaleSlider slider;

            DownscaleOnInactiveEntry(StorageConfig c) {
                toggleBtn = track(toggleButton(50,
                        () -> c.downscaleOnInactive,
                        v -> { c.downscaleOnInactive = v; c.save(); },
                        "§aBE", "§cKI"));
                slider = new RenderScaleSlider(0, 0, 130, 18, c);
                children.add(slider);
                selectables.add(slider);
            }

            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6Render felbontás csökkentés inaktívkor", x + 10, y + 5);
                toggleBtn.setX(x + w - 200);
                toggleBtn.setY(y + 1);
                toggleBtn.render(m, mx, my, d);
                slider.setX(x + w - 140);
                slider.setY(y + 1);
                slider.render(m, mx, my, d);
            }

            @Override public boolean mouseClicked(double mx, double my, int b) {
                return slider.mouseClicked(mx, my, b) || super.mouseClicked(mx, my, b);
            }
            @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
                return slider.mouseDragged(mx, my, b, dx, dy);
            }
            @Override public boolean mouseReleased(double mx, double my, int b) {
                boolean r = slider.mouseReleased(mx, my, b);
                config.save();
                return r;
            }
        }

        private class FpsLimitEntry extends BaseEntry {
            private final ButtonWidget toggle, minM, minP, secM, secP;
            private final TextFieldWidget field;
            FpsLimitEntry(StorageConfig c) {
                toggle = track(toggleButton(36,
                        () -> c.fpsLimitOnInactive, v -> { c.fpsLimitOnInactive = v; c.save(); }, "§aBE", "§cKI"));
                minM = track(actionButton(16, "-", () -> { if (c.fpsLimitMinutes > 0) c.fpsLimitMinutes--; c.save(); }));
                minP = track(actionButton(16, "+", () -> { if (c.fpsLimitMinutes < 60) c.fpsLimitMinutes++; c.save(); }));
                secM = track(actionButton(16, "-", () -> { if (c.fpsLimitSeconds >= 5) c.fpsLimitSeconds -= 5; c.save(); }));
                secP = track(actionButton(16, "+", () -> { if (c.fpsLimitSeconds < 55) c.fpsLimitSeconds += 55; c.save(); }));

                field = new TextFieldWidget(StorageConfigScreen.this.textRenderer, 0, 0, 44, 16, Text.literal("FPS"));
                field.setText(String.valueOf(c.fpsLimitValue));
                field.setChangedListener(t -> {
                    try {
                        int v = Integer.parseInt(t);
                        boolean ok = v >= 1 && v <= 260;
                        if (ok) { c.fpsLimitValue = v; c.save(); }
                        field.setEditableColor(ok ? 0xFFFFFF : 0xFF5555);
                    } catch (NumberFormatException e) { field.setEditableColor(0xFF5555); }
                });
                children.add(field);
            }
            @Override
            public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                var cfg = StorageConfigScreen.this.config;
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6Auto FPS Limit idle", x + 10, y + 5);
                toggle.setX(x + 140); toggle.setY(y + 1); toggle.render(m, mx, my, d);
                minM.setX(x + 180); minM.setY(y + 1); minM.render(m, mx, my, d);
                label(m, cfg.fpsLimitMinutes + "m", x + 200, y + 5, 0xFFFF55);
                minP.setX(x + 224); minP.setY(y + 1); minP.render(m, mx, my, d);
                secM.setX(x + 248); secM.setY(y + 1); secM.render(m, mx, my, d);
                label(m, cfg.fpsLimitSeconds + "s", x + 268, y + 5, 0xFFFF55);
                secP.setX(x + 292); secP.setY(y + 1); secP.render(m, mx, my, d);
                field.setX(x + 316); field.setY(y + 1); field.render(m, mx, my, d);
            }
            @Override public boolean mouseClicked(double mx, double my, int b) {
                return field.mouseClicked(mx, my, b) || super.mouseClicked(mx, my, b);
            }
            @Override public boolean keyPressed(int k, int s, int mo) {
                return field.keyPressed(k, s, mo) || super.keyPressed(k, s, mo);
            }
            @Override public boolean charTyped(char c, int mo) {
                return field.charTyped(c, mo) || super.charTyped(c, mo);
            }
        }

        private class FpsLimitImmediateEntry extends BaseEntry {
            private final ButtonWidget btn;
            FpsLimitImmediateEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.fpsLimitImmediateOnInactive,
                        v -> { c.fpsLimitImmediateOnInactive = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6FPS Limit azonnal inaktívkor", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class MaxFpsEntry extends BaseEntry {
            private final FpsSlider slider;
            MaxFpsEntry(StorageConfig c) {
                slider = new FpsSlider(0, 0, 220, 18, c);
                children.add(slider); selectables.add(slider);
            }
            @Override public boolean mouseClicked(double mx, double my, int b) { return slider.mouseClicked(mx, my, b); }
            @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { return slider.mouseDragged(mx, my, b, dx, dy); }
            @Override public boolean mouseReleased(double mx, double my, int b) {
                boolean r = slider.mouseReleased(mx, my, b); config.save(); return r;
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AAAA00);
                label(m, "§eMax FPS Limit", x + 10, y + 5);
                slider.setX(x + 120); slider.setY(y + 1); slider.render(m, mx, my, d);
            }
        }

        private class VsyncEntry extends BaseEntry {
            private final ButtonWidget btn;
            VsyncEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.vsyncEnabled, v -> {
                            c.vsyncEnabled = v;
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc != null && mc.options != null) {
                                try {
                                    mc.options.getEnableVsync().setValue(v);
                                    if (mc.getWindow() != null) mc.getWindow().setVsync(v);
                                } catch (Throwable ignored) {}
                            }
                            c.save();
                        }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AAAA00);
                label(m, "§eV-Sync", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class HideBlockBreakingParticlesEntry extends BaseEntry {
            private final ButtonWidget btn;
            HideBlockBreakingParticlesEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.hideBlockBreakingParticles,
                        v -> { c.hideBlockBreakingParticles = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA5500);
                label(m, "§6Block Break részecskék", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class HideAllParticlesEntry extends BaseEntry {
            private final ButtonWidget btn;
            HideAllParticlesEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.hideAllParticles,
                        v -> { c.hideAllParticles = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x55CC0000);
                label(m, "§c§lMINDEN PARTICLE KIKAPCSOLÁSA", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class FireOverlayEntry extends BaseEntry {
            private final ButtonWidget toggle, cfgBtn;
            FireOverlayEntry(StorageConfig c) {
                toggle = track(toggleButton(36,
                        () -> c.hideFireOverlay, v -> { c.hideFireOverlay = v; c.save(); }, "§aBE", "§cKI"));
                cfgBtn = track(ButtonWidget.builder(Text.literal("§eBeállítás"), b -> {
                    if (StorageConfigScreen.this.client != null)
                        StorageConfigScreen.this.client.setScreen(
                                new FireOverlayConfigScreen(c, StorageConfigScreen.this));
                }).dimensions(0, 0, 80, 18).build());
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44CC5500);
                label(m, "§cTűz Overlay", x + 10, y + 5);
                toggle.setX(x + 120); toggle.setY(y + 1); toggle.render(m, mx, my, d);
                cfgBtn.setX(x + 162); cfgBtn.setY(y + 1); cfgBtn.render(m, mx, my, d);
            }
        }

        private class HidePuaTitleSubtitleEntry extends BaseEntry {
            private final ButtonWidget btn;
            HidePuaTitleSubtitleEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> (c.hidePuaInTitle || c.hidePuaInSubtitle) ? "§aBE" : "§cKI"),
                        v -> { 
                            if (v) { c.hidePuaInTitle = true; c.hidePuaInSubtitle = true; }
                            else { c.hidePuaInTitle = false; c.hidePuaInSubtitle = false; }
                            c.save(); 
                        }, 
                        "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA88AA);
                label(m, "§dSzerver HUD effektek (PUA) elrejtése a title-ből és subtitle-ből", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class McSettingsApplyEntry extends BaseEntry {
            private final ButtonWidget applyBtn;
            McSettingsApplyEntry(StorageConfig c) {
                applyBtn = track(ButtonWidget.builder(
                        Text.literal("§aAlkalmazás"),
                        b -> StorageConfigScreen.this.applyMcSettings()
                ).dimensions(0, 0, 130, 18).build());
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44404040);
                label(m, "§eMC Beállítások Alkalmazása", x + 10, y + 5);
                applyBtn.setX(x + w - 140); applyBtn.setY(y + 1); applyBtn.render(m, mx, my, d);
            }
        }

        private class HideBlockTexturesEntry extends BaseEntry {
            private final ButtonWidget btn;
            HideBlockTexturesEntry(StorageConfig c) {
                btn = track(toggleButton(50,
                        () -> c.hideBlockTextures,
                        v -> { c.hideBlockTextures = v; c.save(); }, "§aBE", "§cKI"));
            }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x55FF00FF);
                label(m, "§d§lBLOKK TEXTÚRÁK ELREJTÉSE", x + 10, y + 5);
                btn.setX(x + w - 60); btn.setY(y + 1); btn.render(m, mx, my, d);
            }
        }

        private class DebugInfoEntry extends BaseEntry {
            DebugInfoEntry(StorageConfig c) {}
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x22808080);
                var cfg = StorageConfigScreen.this.config;
                String info = "§7Items: §f" + (cfg.normalItems == null ? 0 : cfg.normalItems.size())
                        + "  §7Leveled: §f" + (cfg.leveledItems == null ? 0 : cfg.leveledItems.size())
                        + "  §7MaxFPS: §f" + cfg.maxFps;
                label(m, info, x + 10, y + 5);
            }
        }

        class NormalItemEntry extends BaseEntry {
            final StorageConfig.ItemPreset preset;
            NormalItemEntry(StorageConfig.ItemPreset p) { this.preset = p; }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, hov ? 0x44FFFFFF : 0x22FFFFFF);
                label(m, "§f" + preset.name + " §7(CMD: " + preset.customModelData + ")", x + 10, y + 6, 0xE0E0E0);
                String info = "§7B: §e" + (preset.leftCD / 20.0f) + "s §7| J: §d" + (preset.rightCD / 20.0f) + "s";
                int iw = StorageConfigScreen.this.textRenderer.getWidth(info);
                label(m, info, x + w - iw - 8, y + 6);
            }
        }

        class LeveledItemHeaderEntry extends BaseEntry {
            final StorageConfig.LeveledItemPreset preset;
            private final ButtonWidget toggleBtn;
            private boolean expanded = false;
            LeveledItemHeaderEntry(StorageConfig.LeveledItemPreset p, ItemPresetListWidget parent) {
                this.preset = p;
                toggleBtn = track(ButtonWidget.builder(Text.literal("►"), b -> {
                    expanded = !expanded;
                    b.setMessage(Text.literal(expanded ? "▼" : "►"));
                    parent.toggleLevels(this, preset);
                }).dimensions(0, 0, 22, 18).build());
            }
            boolean isExpanded() { return expanded; }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fillBg(m, x, y, w, h, 0x44AA00AA);
                toggleBtn.setX(x + 4); toggleBtn.setY(y + 1); toggleBtn.render(m, mx, my, d);
                label(m, "§6" + preset.name + " §7(CMD: " + preset.customModelData + ") §d[Fejleszthető]",
                        x + 32, y + 5);
            }
        }

        class LevelSubEntry extends BaseEntry {
            final StorageConfig.ItemLevel level;
            LevelSubEntry(StorageConfig.ItemLevel l) { this.level = l; }
            @Override public void render(MatrixStack m, int i, int y, int x, int w, int h, int mx, int my, boolean hov, float d) {
                fill(m, x + 15, y, x + w, y + h - 2, hov ? 0x44000000 : 0x22000000);
                fill(m, x + 8, y, x + 9, y + h, 0x44FFFFFF);
                fill(m, x + 8, y + (h / 2), x + 14, y + (h / 2) + 1, 0x44FFFFFF);
                label(m, "§b• " + level.levelNameIndicator, x + 20, y + 6, 0xE0E0E0);
                String stats = "§7B: §e" + (level.leftCD / 20.0f) + "s (" + level.leftMana + "m)"
                        + " §7| J: §d" + (level.rightCD / 20.0f) + "s (" + level.rightMana + "m)";
                int sw = StorageConfigScreen.this.textRenderer.getWidth(stats);
                label(m, stats, x + w - sw - 8, y + 6, 0xCCCCCC);
            }
        }
    }

    private static class FpsSlider extends SliderWidget {
        private final StorageConfig config;
        FpsSlider(int x, int y, int w, int h, StorageConfig c) {
            super(x, y, w, h, Text.empty(), (c.maxFps - 10) / 250.0);
            this.config = c;
            this.updateMessage();
        }
        @Override protected void updateMessage() {
            int v = 10 + (int) Math.round(this.value * 250);
            this.setMessage(Text.literal(v >= 260 ? "§aVégtelen (∞)" : "§e" + v + " FPS"));
        }
        @Override protected void applyValue() {
            int v = Math.max(10, Math.min(260, 10 + (int) Math.round(this.value * 250)));
            config.maxFps = v;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.options != null) {
                try { mc.options.getMaxFps().setValue(v); } catch (Throwable ignored) {}
            }
        }
    }

    private static class RenderScaleSlider extends SliderWidget {
        private final StorageConfig config;
        RenderScaleSlider(int x, int y, int w, int h, StorageConfig c) {
            super(x, y, w, h, Text.empty(), (c.inactiveResolutionScale - 10) / 90.0);
            this.config = c;
            this.updateMessage();
        }
        @Override protected void updateMessage() {
            int v = 10 + (int) Math.round(this.value * 90);
            this.setMessage(Text.literal("§e" + String.format(java.util.Locale.ROOT, "%.1fx", v / 100.0f)));
        }
        @Override protected void applyValue() {
            int value = Math.max(10, Math.min(100, 10 + (int) Math.round(this.value * 90)));
            config.inactiveResolutionScale = value;
        }
    }
}
