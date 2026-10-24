package me.Gabix.badge.mixin;

import com.mojang.authlib.GameProfile;
import me.Gabix.badge.ModPlayerTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Shadow public abstract GameProfile getProfile();
    @Shadow private Text displayName;

    private static final Identifier DEFAULT_FONT = Identifier.of("minecraft", "default");
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void overridePlayerName(CallbackInfoReturnable<Text> cir) {
        if (IS_PROCESSING.get()) return;

        try {
            IS_PROCESSING.set(true);

            PlayerListEntry entry = (PlayerListEntry) (Object) this;
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null || entry.getProfile() == null) return;

            boolean isSelf = entry.getProfile().getId().equals(client.player.getUuid());
            boolean isModUser = ModPlayerTracker.usesMod(
                    entry.getProfile().getId(), entry.getProfile().getName());

            if (!isSelf && !isModUser) return;

            Text baseDisplayName = this.displayName;
            if (baseDisplayName == null || baseDisplayName.getString().isEmpty()) {
                baseDisplayName = Text.literal(entry.getProfile().getName());
            }

            if (baseDisplayName.getString().contains("\uE111")) return;

            Style iconStyle = Style.EMPTY.withFont(DEFAULT_FONT).withColor(0xFFFFFF);
            MutableText badge = Text.literal("\uEffe ").setStyle(iconStyle);

            MutableText result = Text.empty();
            result.append(badge);
            result.append(baseDisplayName);

            cir.setReturnValue(result);

        } finally {
            IS_PROCESSING.set(false);
        }
    }
}