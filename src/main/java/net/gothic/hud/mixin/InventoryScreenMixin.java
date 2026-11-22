package net.gothic.hud.mixin;

import net.gothic.hud.InventoryDecor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    /** Frame, candle flames and dripping blood around the inventory. */
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"), require = 0)
    private void gothic$decor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // the inventory is centred: 176 x 166 (no field shadowing, it lives in a superclass)
        Screen self = (Screen) (Object) this;
        InventoryDecor.draw(graphics, (self.width - 176) / 2, (self.height - 166) / 2);
    }

    /*
     * The player model: bigger and lower, so only the head and torso fit into the 49x70 box.
     * Both likely names of the vanilla call are targeted; the one that does not exist is skipped (require = 0).
     */
    @ModifyArgs(method = "/.*/", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse"), require = 0)
    private void gothic$bigPlayerA(Args args) {
        gothic$tweak(args);
    }

    @ModifyArgs(method = "/.*/", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse"), require = 0)
    private void gothic$bigPlayerB(Args args) {
        gothic$tweak(args);
    }

    private static void gothic$tweak(Args args) {
        try {
            // (graphics, x1, y1, x2, y2, scale, yOffset, mouseX, mouseY, entity)
            if (args.size() == 10) {
                args.set(5, 66);       // scale: 30 -> 66
                args.set(6, 0.40f);    // lower the model so the box ends around the waist
            }
        } catch (Throwable ignored) {
            // wrong argument layout in this version: leave the vanilla player untouched
        }
    }
}
