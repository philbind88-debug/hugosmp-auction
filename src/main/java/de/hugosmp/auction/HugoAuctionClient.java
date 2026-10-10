package de.hugosmp.auction;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HugoAuctionClient implements ClientModInitializer {

    public static final String MOD_ID = "hugoauction";

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "general"));
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.hugoauction.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));

        // Taste öffnet das Menü, Tick prüft das Auktionsende.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new AuctionScreen());
                }
            }
            AuctionManager.INSTANCE.tick();
        });

        // WICHTIG: Nur GAME-Nachrichten (Systemnachrichten des Servers) werden ausgewertet.
        // Signierte Spieler-Chatnachrichten laufen über das CHAT-Event und werden ignoriert.
        // Zusätzlich verlangt der Parser eine exakte Übereinstimmung der ganzen Nachricht,
        // sodass ein Spieler sie auch nicht mit Präfix/Suffix nachbauen kann.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) {
                return; // Actionbar ignorieren
            }
            AuctionManager.INSTANCE.onServerMessage(message.getString());
        });

        // HUD: Timer und Führender oben in der Mitte.
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of(MOD_ID, "auction_hud"),
                (context, tickCounter) -> AuctionHud.render(context));

        // Alternative zur Taste: /auktion bzw. /auktion stop
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("auktion")
                        .executes(ctx -> {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            mc.send(() -> mc.setScreen(new AuctionScreen()));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(ClientCommandManager.literal("stop").executes(ctx -> {
                            AuctionManager.INSTANCE.cancel();
                            return Command.SINGLE_SUCCESS;
                        }))));
    }
}
