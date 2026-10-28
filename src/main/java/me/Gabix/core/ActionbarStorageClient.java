package me.Gabix.core;

import me.Gabix.badge.ModPlayerTracker;
import me.Gabix.badge.ModServerClient;
import me.Gabix.boss.BossMod;
import me.Gabix.cooldown.CloneManager;
import me.Gabix.cooldown.CooldownMod;
import me.Gabix.cooldown.CooldownTracker;
import me.Gabix.developer.DeveloperModClient;
import me.Gabix.fireoverlay.FireOverlayMod;
import me.Gabix.fpsreducer.FpsReducerMod;
import me.Gabix.fullbright.FullbrightMod;
import me.Gabix.hud.HudMod;
import me.Gabix.hud.StorageConfigScreen;
import me.Gabix.motionblur.MotionBlurMod;
import me.Gabix.norender.NoRenderMod;
import me.Gabix.soundreducer.SoundReducerMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ActionbarStorageClient implements ClientModInitializer {
    private static ActionbarStorageClient instance;
    private static volatile StorageConfig config;
    private int globalTickCounter = 0;
    private int heartbeatCounter = 0;
    private int fetchCounter = 0;
    private boolean wasAttackKeyPressedLastTick = false;
    private boolean wasUseKeyPressedLastTick = false;

    public static ActionbarStorageClient getInstance() {
        if (instance == null) {
            instance = new ActionbarStorageClient();
        }
        return instance;
    }

    public static StorageConfig getConfig() {
        StorageConfig local = config;
        if (local == null) {
            synchronized (ActionbarStorageClient.class) {
                local = config;
                if (local == null) {
                    local = StorageConfig.load();
                    config = local;
                }
            }
        }
        return local;
    }

    public int getGlobalTickCounter() {
        return globalTickCounter;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        config = StorageConfig.load();

        ResourceManagerHelper.registerBuiltinResourcePack(
                new Identifier("mvp", "mvp_resources"),
                FabricLoader.getInstance().getModContainer("mvp").orElseThrow(),
                Text.literal("MVP Font"),
                ResourcePackActivationType.ALWAYS_ENABLED
        );

        safeInit(() -> new HudMod().onInitializeClient(), "HudMod");
        safeInit(() -> new CooldownMod().onInitializeClient(), "CooldownMod");
        safeInit(() -> new BossMod().onInitializeClient(), "BossMod");
        safeInit(() -> new NoRenderMod().onInitializeClient(), "NoRenderMod");
        safeInit(() -> new MotionBlurMod().onInitializeClient(), "MotionBlurMod");
        safeInit(() -> new FullbrightMod().onInitializeClient(), "FullbrightMod");
        safeInit(() -> new FireOverlayMod().onInitializeClient(), "FireOverlayMod");
        safeInit(() -> new FpsReducerMod().onInitializeClient(), "FpsReducerMod");
        safeInit(() -> new SoundReducerMod().onInitializeClient(), "SoundReducerMod");
        safeInit(() -> new DeveloperModClient().onInitializeClient(), "DeveloperModClient");

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
            HudMod.getArrowCounter().reset();
        });

        KeyBinding configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Actionbar Config Megnyitása", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "MVP Mod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            globalTickCounter++;
            CloneManager.INSTANCE.tick(globalTickCounter);
            HudMod.getFpsCpsCounter().tick(client);
            HudMod.getArrowCounter().tick(client);

            heartbeatCounter++;
            fetchCounter++;
            if (client.getSession() != null && heartbeatCounter >= 400) {
                heartbeatCounter = 0;
                UUID selfUuid = getSelfUuid(client);
                if (selfUuid != null) ModServerClient.sendHeartbeatAsync(selfUuid);
            }
            if (client.getSession() != null && fetchCounter >= 600) {
                fetchCounter = 0;
                ModServerClient.fetchUsersAsync();
            }

            while (configKey.wasPressed()) {
                StorageConfig cfg = getConfig();
                client.setScreen(new StorageConfigScreen(cfg));
            }

            int activeSlot = client.player.getInventory().selectedSlot;
            boolean slotEnabled = activeSlot >= 0 && activeSlot < 9 && getConfig().hudSlotsEnabled[activeSlot];

            boolean isAttackPressedNow = client.options.attackKey.isPressed();
            boolean isLeftClicked = isAttackPressedNow && !wasAttackKeyPressedLastTick;

            boolean isUsePressedNow = client.options.useKey.isPressed();
            boolean isUsePressedEdge = isUsePressedNow && !wasUseKeyPressedLastTick;

            boolean hasBowInOffhand = isOffhandBow(client);

            boolean isRightClicked = hasBowInOffhand
                    ? isUsePressedEdge
                    : isUsePressedNow;

            if (slotEnabled) {
                while (CooldownMod.resetCooldownKeyBinding.wasPressed()) {
                    SlotSettings settings = getSlotSettings(activeSlot);
                    if (settings != null && settings.cooldownKey() != null) {
                        CooldownTracker.INSTANCE.resetCooldowns(settings.cooldownKey());
                    }
                }
                if (isLeftClicked) handleLeftClick(client, activeSlot);
                if (isRightClicked) handleRightClick(client, activeSlot, isUsePressedEdge);
            } else {
                while (CooldownMod.resetCooldownKeyBinding.wasPressed()) {}
            }

            wasAttackKeyPressedLastTick = isAttackPressedNow;
            wasUseKeyPressedLastTick = isUsePressedNow;
        });
    }

    private boolean isOffhandBow(MinecraftClient client) {
        if (client.player == null) return false;
        ItemStack offhandStack = client.player.getOffHandStack();
        return !offhandStack.isEmpty() && offhandStack.getItem() instanceof BowItem;
    }

    private void handleLeftClick(MinecraftClient client, int activeSlot) {
        if (isOffhandBow(client) && client.options.useKey.isPressed()) {
            return;
        }

        ItemStack mainhandStack = client.player.getMainHandStack();
        if (!mainhandStack.isEmpty()) {
            SlotSettings settings = getSlotSettings(activeSlot);
            if (settings != null && settings.cooldownKey() != null && settings.leftCD() > 0) {
                if (CooldownTracker.INSTANCE.isReady(settings.cooldownKey(), settings, globalTickCounter, HudMod.getManaManager().getCurrentMana(), false)) {
                    CooldownTracker.INSTANCE.registerLeftClick(settings.cooldownKey(), globalTickCounter, settings.leftCD());
                }
            }
        }

        ItemStack offhandStack = client.player.getOffHandStack();
        if (isOffhandLeftClickable(offhandStack)) {
            SlotSettings offhandSettings = getSettingsForItem(offhandStack);
            if (offhandSettings != null && offhandSettings.cooldownKey() != null && offhandSettings.leftCD() > 0) {
                if (CooldownTracker.INSTANCE.isReady(offhandSettings.cooldownKey(), offhandSettings, globalTickCounter, HudMod.getManaManager().getCurrentMana(), false)) {
                    CooldownTracker.INSTANCE.registerLeftClick(offhandSettings.cooldownKey(), globalTickCounter, offhandSettings.leftCD());
                }
            }
        }
    }

    private void handleRightClick(MinecraftClient client, int activeSlot, boolean isUsePressedEdge) {
        boolean sneaking = client.player.isSneaking();

        ItemStack offhandStack = client.player.getOffHandStack();
        SlotSettings offhandSettings = getSettingsForItem(offhandStack);
        boolean offhandReady = false;
        if (offhandSettings != null && offhandSettings.cooldownKey() != null && offhandSettings.rightCD() > 0
                && isRightClickAllowed(offhandSettings, sneaking, isUsePressedEdge)) {
            if (CooldownTracker.INSTANCE.isReady(offhandSettings.cooldownKey(), offhandSettings, globalTickCounter, HudMod.getManaManager().getCurrentMana(), true)) {
                offhandReady = true;
            }
        }

        SlotSettings mainhandSettings = getSlotSettings(activeSlot);
        boolean mainhandReady = false;
        if (mainhandSettings != null && mainhandSettings.cooldownKey() != null && mainhandSettings.rightCD() > 0
                && isRightClickAllowed(mainhandSettings, sneaking, isUsePressedEdge)) {
            if (mainhandSettings.cooldownKey().startsWith("minecraft:paper:581")) {
                if (CooldownTracker.INSTANCE.isReady(mainhandSettings.cooldownKey(), mainhandSettings, globalTickCounter, HudMod.getManaManager().getCurrentMana(), true)
                        && CloneManager.INSTANCE.getActiveClonesCount() < 2) {
                    mainhandReady = true;
                }
            } else {
                if (CooldownTracker.INSTANCE.isReady(mainhandSettings.cooldownKey(), mainhandSettings, globalTickCounter, HudMod.getManaManager().getCurrentMana(), true)) {
                    mainhandReady = true;
                }
            }
        }

        if (offhandReady && offhandSettings != null && offhandSettings.cooldownKey() != null) {
            CooldownTracker.INSTANCE.registerRightClick(offhandSettings.cooldownKey(), globalTickCounter, offhandSettings.rightCD());
        }

        if (mainhandReady && mainhandSettings != null && mainhandSettings.cooldownKey() != null) {
            if (mainhandSettings.cooldownKey().startsWith("minecraft:paper:581")) {
                CooldownTracker.INSTANCE.registerRightClick(mainhandSettings.cooldownKey(), globalTickCounter, mainhandSettings.rightCD());
                CloneManager.INSTANCE.spawnClone(globalTickCounter, mainhandSettings.maxCharges());
            } else {
                CooldownTracker.INSTANCE.registerRightClick(mainhandSettings.cooldownKey(), globalTickCounter, mainhandSettings.rightCD());
            }
        }
    }

    /**
     * A shiftes íjaknál (sneakShotOnly) a jobb klikkes cooldown csak akkor indul,
     * ha a játékos le van guggolva és épp most nyomta le a jobb klikket.
     * Nem kell hozzá kilőni a nyilat.
     */
    private static boolean isRightClickAllowed(SlotSettings settings, boolean sneaking, boolean isUsePressedEdge) {
        if (!settings.sneakShotOnly()) return true;
        return sneaking && isUsePressedEdge;
    }

    private boolean isOffhandLeftClickable(ItemStack offhandStack) {
        if (offhandStack.isEmpty()) return false;
        if (offhandStack.getItem() instanceof BowItem) {
            return true;
        }
        String offhandId = Registries.ITEM.getId(offhandStack.getItem()).toString();
        if (offhandId.equals("minecraft:paper") && offhandStack.hasNbt() && offhandStack.getNbt().contains("CustomModelData")) {
            return offhandStack.getNbt().getInt("CustomModelData") == 585;
        }
        return false;
    }

    private static void safeInit(Runnable init, String name) {
        try {
            init.run();
        } catch (Exception e) {
            System.err.println("[MVP] Hiba a " + name + " inicializálásakor:");
            e.printStackTrace();
        }
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
                    return new SlotSettings(globalKey, currentItemName, preset.leftCD, preset.rightCD, preset.leftMana, preset.rightMana, preset.autoMode, preset.maxCharges, preset.sneakShotOnly);
                }
            }
        }
        if (config.leveledItems != null) {
            for (StorageConfig.LeveledItemPreset preset : config.leveledItems) {
                if (preset.itemId.equals(itemId) && preset.customModelData == customModelData) {
                    int leftCd = preset.leftCD, rightCd = preset.rightCD, leftMana = preset.leftMana, rightMana = preset.rightMana;
                    String levelKey = globalKey;
                    if (preset.levels != null && currentItemName != null) {
                        for (StorageConfig.ItemLevel lvl : preset.levels) {
                            if (currentItemName.contains(lvl.levelNameIndicator)) {
                                leftCd = lvl.leftCD;
                                rightCd = lvl.rightCD;
                                leftMana = lvl.leftMana;
                                rightMana = lvl.rightMana;
                                if (preset.customModelData == 7002 || preset.customModelData == 7003 || preset.customModelData == 7000 || preset.customModelData == 7001) {
                                    levelKey = globalKey + ":" + lvl.levelNameIndicator;
                                }
                                break;
                            }
                        }
                    }
                    return new SlotSettings(levelKey, currentItemName, leftCd, rightCd, leftMana, rightMana, preset.autoMode, preset.maxCharges, preset.sneakShotOnly);
                }
            }
        }
        return null;
    }

    public record SlotSettings(String cooldownKey, String displayName, int leftCD, int rightCD, int leftMana, int rightMana, int autoMode, int maxCharges, boolean sneakShotOnly) {}
}