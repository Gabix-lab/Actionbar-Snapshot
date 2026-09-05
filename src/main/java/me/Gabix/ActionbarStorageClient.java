package me.Gabix;

import me.Gabix.badge.ModPlayerTracker;
import me.Gabix.badge.ModServerClient;
import me.Gabix.developer.DeveloperModClient;
import me.Gabix.fpsreducer.FpsManager;
import me.Gabix.fullbrightnesstoggle.FullbrightModClient;
import me.Gabix.motionblur.NaturalMotionBlurMod;
import me.Gabix.norender.NoRenderEngine;
import me.Gabix.soundreducer.MuteOnInactive;
import me.Gabix.soundreducer.SoundReducer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ActionbarStorageClient implements ClientModInitializer {
    private static ActionbarStorageClient instance;
    private static StorageConfig savedConfig;
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);
    private final ManaManager manaManager = new ManaManager();
    private final CloneManager cloneManager = new CloneManager();
    private final FpsCpsCounter fpsCpsCounter = new FpsCpsCounter();
    public static KeyBinding configKeyBinding;
    public static KeyBinding resetCooldownKeyBinding;
    public static KeyBinding toggleContinuousKeyBinding;
    public static KeyBinding noRenderKeyBinding;
    public static KeyBinding noRenderTimerKeyBinding;
    public static KeyBinding noRenderInactiveKeyBinding;
    private int globalTickCounter = 0;
    private boolean wasAttackKeyPressedLastTick = false;
    private boolean wasUseKeyPressedLastTick = false;

    public ActionbarStorageClient() {}

    public static void init() {
        ActionbarStorageClient clientInstance = new ActionbarStorageClient();
        clientInstance.onInitializeClient();
    }

    @Override
    public void onInitializeClient() {
        instance = this;

        new FullbrightModClient().onInitializeClient();
        new DeveloperModClient().onInitializeClient();
        new NaturalMotionBlurMod().onInitializeClient();

        savedConfig = StorageConfig.load();

        FpsManager.init();
        NoRenderEngine.init();
        SoundReducer.init();
        WindowFocusEvent.register(new MuteOnInactive());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.getSession() != null) {
                UUID selfUuid = getSelfUuid(client);
                if (selfUuid != null) {
                    ModServerClient.sendHeartbeatAsync(selfUuid);
                    ModServerClient.fetchUsersAsync();
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.getSession() != null) {
                UUID selfUuid = getSelfUuid(client);
                if (selfUuid != null) {
                    ModServerClient.sendLogoutAsync(selfUuid);
                }
            }
            ModPlayerTracker.clear();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
        }));

        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Actionbar Config Megnyitása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Actionbar Storage Mod"));
        resetCooldownKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Cooldown Nullázása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SEMICOLON, "Actionbar Storage Mod"));
        toggleContinuousKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Kesz Felirat Mod Valtas", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_APOSTROPHE, "Actionbar Storage Mod"));
        noRenderKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "3D Rendereles Ki/Be", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, "NoRender"));
        noRenderInactiveKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "NoRender Ablak Inaktivnal", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "NoRender"));

        ActionbarReader.init();

        HudRenderCallback.EVENT.register(CustomHudRenderer::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            FpsManager.tick();
            NoRenderEngine.tick(client);

            globalTickCounter++;
            cloneManager.tick(globalTickCounter);
            ActionbarReader.handleTick(client);
            fpsCpsCounter.tick(client);

            if (globalTickCounter % 200 == 0 && client.getSession() != null) {
                UUID selfUuid = getSelfUuid(client);
                if (selfUuid != null) {
                    ModServerClient.sendHeartbeatAsync(selfUuid);
                    ModServerClient.fetchUsersAsync();
                }
            }

            while (configKeyBinding.wasPressed()) {
                if (savedConfig == null) savedConfig = StorageConfig.load();
                client.setScreen(new StorageConfigScreen(savedConfig));
            }

            while (noRenderKeyBinding.wasPressed()) NoRenderEngine.toggle(client);
            while (noRenderInactiveKeyBinding.wasPressed()) NoRenderEngine.toggleInactive(client);

            int activeSlot = client.player.getInventory().selectedSlot;
            boolean slotEnabled = activeSlot >= 0 && activeSlot < 9 && getConfig().hudSlotsEnabled[activeSlot];

            boolean isAttackPressedNow = client.options.attackKey.isPressed();
            boolean isLeftClicked = isAttackPressedNow && !wasAttackKeyPressedLastTick;

            boolean isUsePressedNow = client.options.useKey.isPressed();

            if (slotEnabled) {
                while (resetCooldownKeyBinding.wasPressed()) {
                    SlotSettings settings = getSlotSettings(activeSlot);
                    if (settings != null && settings.cooldownKey() != null) {
                        CooldownTracker.INSTANCE.resetCooldowns(settings.cooldownKey());
                    }
                }

                // BAL KLIKK: csak a lenyomás pillanatában
                if (isLeftClicked) {
                    ItemStack mainhandStack = client.player.getMainHandStack();
                    if (!mainhandStack.isEmpty()) {
                        SlotSettings settings = getSlotSettings(activeSlot);
                        boolean mainhandUsed = false;
                        if (settings != null && settings.cooldownKey() != null && settings.leftCD() > 0) {
                            if (CooldownTracker.INSTANCE.isReady(settings.cooldownKey(), settings, globalTickCounter, manaManager.getCurrentMana(), false)) {
                                CooldownTracker.INSTANCE.registerLeftClick(settings.cooldownKey(), globalTickCounter, settings.leftCD());
                                mainhandUsed = true;
                            }
                        }
                        if (!mainhandUsed) {
                            ItemStack offhandStack = client.player.getOffHandStack();
                            boolean isOffhandLeftClickable = false;
                            if (!offhandStack.isEmpty()) {
                                String offhandId = Registries.ITEM.getId(offhandStack.getItem()).toString();
                                if (offhandId.equals("minecraft:bow")) {
                                    isOffhandLeftClickable = true;
                                } else if (offhandId.equals("minecraft:paper") && offhandStack.hasNbt() && offhandStack.getNbt().contains("CustomModelData")) {
                                    int cmd = offhandStack.getNbt().getInt("CustomModelData");
                                    if (cmd == 585) {
                                        isOffhandLeftClickable = true;
                                    }
                                }
                            }
                            if (isOffhandLeftClickable) {
                                SlotSettings offhandSettings = getSettingsForItem(offhandStack);
                                if (offhandSettings != null && offhandSettings.cooldownKey() != null && offhandSettings.leftCD() > 0) {
                                    if (CooldownTracker.INSTANCE.isReady(offhandSettings.cooldownKey(), offhandSettings, globalTickCounter, manaManager.getCurrentMana(), false)) {
                                        CooldownTracker.INSTANCE.registerLeftClick(offhandSettings.cooldownKey(), globalTickCounter, offhandSettings.leftCD());
                                    }
                                }
                            }
                        }
                    }
                }

                // JOBB KLIKK: folyamatosan, amíg nyomva van
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
            } else {
                while (resetCooldownKeyBinding.wasPressed()) {}
            }

            wasAttackKeyPressedLastTick = isAttackPressedNow;
            wasUseKeyPressedLastTick = isUsePressedNow;
        });
    }

    private static UUID getSelfUuid(MinecraftClient client) {
        if (client.getSession() == null) return null;
        UUID uuid = client.getSession().getUuidOrNull();
        if (uuid == null) {
            String username = client.getSession().getUsername();
            if (username != null && !username.isEmpty()) {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
            }
        }
        return uuid;
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
    public ScheduledExecutorService getScheduler() { return scheduler; }
    public FpsCpsCounter getFpsCpsCounter() { return fpsCpsCounter; }

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