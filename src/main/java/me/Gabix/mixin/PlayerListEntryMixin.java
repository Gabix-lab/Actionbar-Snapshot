package me.Gabix.mixin;

import me.Gabix.tracker.ModPlayerTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListEntryMixin {

    private static final Identifier DEFAULT_FONT = new Identifier("minecraft", "default");

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    private void overridePlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (entry == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        boolean isSelf = entry.getProfile().getId().equals(client.player.getUuid());
        boolean isModUser = ModPlayerTracker.usesMod(entry.getProfile().getId());

        // Ha a játékos saját maga, VAGY a külső webszerver szerint mod-használó
        if (isSelf || isModUser) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) {
                displayName = Text.literal(entry.getProfile().getName());
            }

            String raw = displayName.getString();

            if (!raw.contains("\uE001")) {
                Style iconStyle = Style.EMPTY.withFont(DEFAULT_FONT).withColor(0xFFFFFF);
                MutableText iconPrefix = Text.literal("\uE001 ").setStyle(iconStyle);

                MutableText finalTabName = Text.empty();
                finalTabName.append(iconPrefix);
                finalTabName.append(displayName);

                cir.setReturnValue(finalTabName);
            }
        }
    }
}