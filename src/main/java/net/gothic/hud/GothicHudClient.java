package net.gothic.hud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class GothicHudClient implements ClientModInitializer {
    public static final String MOD_ID = "gothic_hud";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(Effects::tick);
        // drawn right before the chat, i.e. above the hotbar and below every screen
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("overlay"), Effects::extract);
    }
}
