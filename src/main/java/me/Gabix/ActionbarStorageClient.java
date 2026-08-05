package me.Gabix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ActionbarStorageClient implements ClientModInitializer {
    private static ActionbarStorageClient instance;
    private static StorageConfig savedConfig;

    public static KeyBinding configKeyBinding;
    public static KeyBinding resetCooldownKeyBinding;
    public static KeyBinding readActionbarKeyBinding;
    public static KeyBinding readBossBarsKeyBinding;
    public static KeyBinding toggleContinuousKeyBinding;
    public static KeyBinding noRenderKeyBinding;
    public static KeyBinding noRenderTimerKeyBinding;
    public static KeyBinding noRenderInactiveKeyBinding;
    public static KeyBinding itemInfoKeyBinding;
    public static KeyBinding fullbrightKeyBinding;

    private ManaManager manaManager;
    private CloneManager cloneManager;
    private int globalTickCounter = 0;
    private boolean wasAttackKeyPressedLastTick = false;
    private boolean wasUseKeyPressedLastTick = false;

    public ActionbarStorageClient() {
        this.manaManager = new ManaManager();
        this.cloneManager = new CloneManager();
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        savedConfig = StorageConfig.load();

        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Actionbar Config Megnyitása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Actionbar Storage Mod"));
        readActionbarKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Actionbar Nyers Beolvasása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "Actionbar Storage Mod"));
        readBossBarsKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "BossBar Név Beolvasása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "Actionbar Storage Mod"));
        resetCooldownKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Cooldown Nullázása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SEMICOLON, "Actionbar Storage Mod"));
        toggleContinuousKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Kész Felirat Mód Váltás", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_APOSTROPHE, "Actionbar Storage Mod"));
        noRenderKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "3D Renderelés Ki/Be", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, "Actionbar Storage Mod"));
        noRenderTimerKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "NoRender Időzítő Ki/Be", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, "Actionbar Storage Mod"));
        noRenderInactiveKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "NoRender Ablak Inaktívnál", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "Actionbar Storage Mod"));
        itemInfoKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Item Info Megjelenítése", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, "Actionbar Storage Mod"));
        fullbrightKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Fullbright Ki/Be", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "Actionbar Storage Mod"));

        ActionbarReader.init();
        NoRenderManager.init(savedConfig);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            globalTickCounter++;
            cloneManager.tick(globalTickCounter);
            ActionbarReader.handleTick(client);

            while (configKeyBinding.wasPressed()) {
                if (savedConfig == null) savedConfig = StorageConfig.load();
                client.setScreen(new StorageConfigScreen(savedConfig));
            }

            while (noRenderTimerKeyBinding.wasPressed()) {
                savedConfig.noRenderTimerEnabled = !savedConfig.noRenderTimerEnabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Időzítő: " +
                            (savedConfig.noRenderTimerEnabled ? "§aBE" : "§cKI")), true);
                }
                savedConfig.save();
            }

            while (noRenderInactiveKeyBinding.wasPressed()) {
                savedConfig.noRenderOnInactive = !savedConfig.noRenderOnInactive;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[GabixMod] §fNoRender Ablak Inaktívnál: " +
                            (savedConfig.noRenderOnInactive ? "§aBE" : "§cKI")), true);
                }
                savedConfig.save();
            }

            while (fullbrightKeyBinding.wasPressed()) {
                FullbrightManager.toggle(client);
            }

            while (itemInfoKeyBinding.wasPressed()) {
                ItemStack held = client.player.getMainHandStack();
                if (held.isEmpty()) {
                    client.player.sendMessage(Text.literal("§c[Item Info] Nincs tárgy a kezedben!"), false);
                } else {
                    String name = held.getName().getString();
                    client.player.sendMessage(Text.literal("§a[Item Info] §fNév: §e" + name), false);

                    int cmd = 0;
                    boolean hasCmd = false;
                    if (held.hasNbt() && held.getNbt().contains("CustomModelData")) {
                        cmd = held.getNbt().getInt("CustomModelData");
                        hasCmd = true;
                    }
                    if (hasCmd) {
                        client.player.sendMessage(Text.literal("§a[Item Info] §fCMD: §b" + cmd), false);
                    } else {
                        client.player.sendMessage(Text.literal("§a[Item Info] §fCMD: §7nincs"), false);
                    }

                    if (held.hasNbt() && held.getNbt().contains("display")) {
                        NbtCompound display = held.getNbt().getCompound("display");
                        if (display.contains("Lore", 9)) {
                            NbtList loreList = display.getList("Lore", 8);
                            client.player.sendMessage(Text.literal("§a[Item Info] §fLore:"), false);
                            for (int i = 0; i < loreList.size(); i++) {
                                String jsonText = loreList.getString(i);
                                try {
                                    Text line = Text.Serializer.fromJson(jsonText);
                                    if (line != null) {
                                        client.player.sendMessage(Text.literal("§7- §f").append(line), false);
                                    } else {
                                        client.player.sendMessage(Text.literal("§7- §c[hibás JSON]"), false);
                                    }
                                } catch (Exception e) {
                                    client.player.sendMessage(Text.literal("§7- §c[hibás formátum]"), false);
                                }
                            }
                        } else {
                            client.player.sendMessage(Text.literal("§a[Item Info] §fLore: §7nincs"), false);
                        }
                    } else {
                        client.player.sendMessage(Text.literal("§a[Item Info] §fLore: §7nincs"), false);
                    }
                }
            }

            int activeSlot = client.player.getInventory().selectedSlot;

            if (activeSlot >= 0 && activeSlot < 9 && getConfig().hudSlotsEnabled[activeSlot]) {
                while (resetCooldownKeyBinding.wasPressed()) {
                    SlotSettings settings = getSlotSettings(activeSlot);
                    if (settings != null && settings.cooldownKey() != null) {
                        CooldownTracker.INSTANCE.resetCooldowns(settings.cooldownKey());
                    }
                }

                boolean isAttackPressedNow = client.options.attackKey.isPressed();
                boolean isLeftClicked = client.options.attackKey.wasPressed() || (isAttackPressedNow && !wasAttackKeyPressedLastTick);
                if (isLeftClicked) {
                    SlotSettings settings = getSlotSettings(activeSlot);
                    if (settings != null && settings.cooldownKey() != null && settings.leftCD() > 0) {
                        if (CooldownTracker.INSTANCE.isReady(settings.cooldownKey(), settings, globalTickCounter, manaManager.getCurrentMana(), false)) {
                            CooldownTracker.INSTANCE.registerLeftClick(settings.cooldownKey(), globalTickCounter, settings.leftCD());
                        }
                    }
                }
                wasAttackKeyPressedLastTick = isAttackPressedNow;

                boolean isUsePressedNow = client.options.useKey.isPressed();
                if (isUsePressedNow) {
                    ItemStack offhandStack = client.player.getOffHandStack();
                    SlotSettings offhandSettings = getSettingsForItem(offhandStack);
                    boolean offhandUsed = false;
                    if (offhandSettings != null && offhandSettings.cooldownKey() != null && offhandSettings.rightCD() > 0) {
                        if (CooldownTracker.INSTANCE.isReady(offhandSettings.cooldownKey(), offhandSettings, globalTickCounter, manaManager.getCurrentMana(), true)) {
                            CooldownTracker.INSTANCE.registerRightClick(offhandSettings.cooldownKey(), globalTickCounter, offhandSettings.rightCD());
                            offhandUsed = true;
                        }
                    }
                    if (!offhandUsed) {
                        SlotSettings mainhandSettings = getSlotSettings(activeSlot);
                        if (mainhandSettings != null && mainhandSettings.cooldownKey() != null && mainhandSettings.rightCD() > 0) {
                            if (mainhandSettings.cooldownKey().startsWith("minecraft:paper:581")) {
                                if (CooldownTracker.INSTANCE.isReady(mainhandSettings.cooldownKey(), mainhandSettings, globalTickCounter, manaManager.getCurrentMana(), true)
                                        && cloneManager.getActiveClonesCount() < 2) {
                                    CooldownTracker.INSTANCE.registerRightClick(mainhandSettings.cooldownKey(), globalTickCounter, mainhandSettings.rightCD());
                                    cloneManager.spawnClone(globalTickCounter);
                                }
                            } else {
                                if (CooldownTracker.INSTANCE.isReady(mainhandSettings.cooldownKey(), mainhandSettings, globalTickCounter, manaManager.getCurrentMana(), true)) {
                                    CooldownTracker.INSTANCE.registerRightClick(mainhandSettings.cooldownKey(), globalTickCounter, mainhandSettings.rightCD());
                                }
                            }
                        }
                    }
                }
                wasUseKeyPressedLastTick = isUsePressedNow;
            } else {
                while (resetCooldownKeyBinding.wasPressed()) {}
                wasAttackKeyPressedLastTick = false;
                wasUseKeyPressedLastTick = false;
            }
        });
    }

    public static ActionbarStorageClient getInstance() {
        if (instance == null) {
            instance = new ActionbarStorageClient();
        }
        return instance;
    }

    public ManaManager getManaManager() { return this.manaManager; }
    public CloneManager getCloneManager() { return this.cloneManager; }
    public int getGlobalTickCounter() { return this.globalTickCounter; }

    public static StorageConfig getConfig() {
        if (savedConfig == null) savedConfig = StorageConfig.load();
        return savedConfig;
    }

    public static SlotSettings getSlotSettings(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || slot < 0 || slot > 8) return null;
        return getSettingsForItem(client.player.getInventory().getStack(slot));
    }

    public static SlotSettings getSettingsForItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        StorageConfig config = getConfig();
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        int customModelData = 0;
        if (stack.hasNbt() && stack.getNbt().contains("CustomModelData")) {
            customModelData = stack.getNbt().getInt("CustomModelData");
        }
        String currentItemName = stack.getName().getString();
        String globalKey = itemId + ":" + customModelData;

        if (config.normalItems != null) {
            for (StorageConfig.ItemPreset preset : config.normalItems) {
                if (preset.itemId.equals(itemId) && preset.customModelData == customModelData) {
                    return new SlotSettings(globalKey, currentItemName, preset.leftCD, preset.rightCD, preset.leftMana, preset.rightMana, preset.autoMode, preset.maxCharges);
                }
            }
        }
        if (config.leveledItems != null) {
            for (StorageConfig.LeveledItemPreset preset : config.leveledItems) {
                if (preset.itemId.equals(itemId) && preset.customModelData == customModelData) {
                    int leftCd = preset.leftCD, rightCd = preset.rightCD, leftMana = preset.leftMana, rightMana = preset.rightMana;
                    if (preset.levels != null && currentItemName != null) {
                        for (StorageConfig.ItemLevel lvl : preset.levels) {
                            if (currentItemName.contains(lvl.levelNameIndicator)) {
                                leftCd = lvl.leftCD; rightCd = lvl.rightCD;
                                leftMana = lvl.leftMana; rightMana = lvl.rightMana;
                                break;
                            }
                        }
                    }
                    return new SlotSettings(globalKey, currentItemName, leftCd, rightCd, leftMana, rightMana, preset.autoMode, preset.maxCharges);
                }
            }
        }
        return null;
    }

    public record SlotSettings(String cooldownKey, String displayName, int leftCD, int rightCD, int leftMana, int rightMana, int autoMode, int maxCharges) {}
}