package net.necookie.disastersim.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.necookie.disastersim.BerongSMP;
import net.necookie.disastersim.item.ModularExtinguisher;
import net.necookie.disastersim.item.ModularExtinguisher.ExtinguisherState;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class FireExtinguisherHud {
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath(BerongSMP.MODID, "fire_extinguisher_hud");
    private static final long PULLING_WINDOW_MS = 850L;
    private static final float SWEEP_THRESHOLD = 1.35F;

    private static long lastPullStartedAt = Long.MIN_VALUE;
    private static float lastYaw;
    private static boolean yawSeeded;
    private static float sweepEnergy;

    private FireExtinguisherHud() {
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, HUD_LAYER, FireExtinguisherHud::render);
    }

    public static void onPullPinStarted() {
        lastPullStartedAt = Util.getMillis();
    }

    private static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            yawSeeded = false;
            sweepEnergy = 0.0F;
            return;
        }

        ItemStack stack = getEquippedExtinguisher(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof ModularExtinguisher extinguisher)) {
            yawSeeded = false;
            sweepEnergy = 0.0F;
            return;
        }

        long now = Util.getMillis();
        ExtinguisherState state = extinguisher.getState(stack);
        updateSweepEnergy(player, state);

        PassVisualState visualState = resolveVisualState(player, stack, state, now);
        drawHud(guiGraphics, minecraft.font, now, visualState, state);
    }

    private static ItemStack getEquippedExtinguisher(Player player) {
        if (player.getMainHandItem().getItem() instanceof ModularExtinguisher) {
            return player.getMainHandItem();
        }

        if (player.getOffhandItem().getItem() instanceof ModularExtinguisher) {
            return player.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    private static void updateSweepEnergy(Player player, ExtinguisherState state) {
        float yaw = player.getYHeadRot();
        if (!yawSeeded) {
            lastYaw = yaw;
            yawSeeded = true;
        }

        float yawDelta = Math.abs(Mth.wrapDegrees(yaw - lastYaw));
        lastYaw = yaw;

        if (state == ExtinguisherState.DISCHARGING || player.isUsingItem()) {
            sweepEnergy = Mth.clamp((sweepEnergy * 0.82F) + (yawDelta * 0.18F), 0.0F, 6.0F);
        } else {
            sweepEnergy *= 0.72F;
        }
    }

    private static PassVisualState resolveVisualState(Player player, ItemStack stack, ExtinguisherState state, long now) {
        boolean usingExtinguisher = player.isUsingItem() && player.getUseItem() == stack;
        boolean pulling = now - lastPullStartedAt <= PULLING_WINDOW_MS;

        if (pulling) {
            return PassVisualState.PULLING;
        }

        if (usingExtinguisher && sweepEnergy >= SWEEP_THRESHOLD) {
            return PassVisualState.SWEEPING;
        }

        if (usingExtinguisher || state == ExtinguisherState.DISCHARGING) {
            return PassVisualState.SQUEEZING;
        }

        if (state == ExtinguisherState.PULLED || state == ExtinguisherState.AIMED) {
            return PassVisualState.PULLED;
        }

        return PassVisualState.LOCKED;
    }

    private static void drawHud(GuiGraphicsExtractor guiGraphics, Font font, long now, PassVisualState visualState, ExtinguisherState state) {
        int width = 232;
        int height = 96;
        int x = guiGraphics.guiWidth() - width - 14;
        int y = guiGraphics.guiHeight() - height - 26;

        int shell = 0xD4191E24;
        int shellEdge = 0xFF5B6671;
        int inner = 0xC411151A;
        int ember = 0xFFEB6A36;
        int gold = 0xFFF2C75C;
        int mist = 0xFFE5EFEA;
        int cool = 0xFF8CCED9;
        int muted = 0xFFA6B1B9;
        int ink = 0xFFE7ECE9;

        guiGraphics.fill(x, y, x + width, y + height, shell);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, inner);
        guiGraphics.horizontalLine(x, x + width - 1, y, shellEdge);
        guiGraphics.horizontalLine(x, x + width - 1, y + height - 1, shellEdge);
        guiGraphics.verticalLine(x, y, y + height - 1, shellEdge);
        guiGraphics.verticalLine(x + width - 1, y, y + height - 1, shellEdge);

        guiGraphics.fillGradient(x + 8, y + 8, x + width - 8, y + 20, 0xE02E353D, 0x80242A31);
        guiGraphics.text(font, "FIRE EXTINGUISHER", x + 12, y + 11, ink, false);
        guiGraphics.text(font, labelForState(visualState), x + 12, y + 25, activeColor(visualState, ember, gold, cool, mist), false);
        guiGraphics.text(font, helperText(state, visualState), x + 12, y + 37, muted, false);

        drawExtinguisherFigure(guiGraphics, x + 16, y + 54, now, visualState, ember, gold, cool, mist, muted);
        drawPassSteps(guiGraphics, font, x + 100, y + 54, visualState, now, ember, gold, cool, mist, muted, ink);
        drawPressureBand(guiGraphics, x + 14, y + height - 12, width - 28, visualState, ember, gold, cool, mist);
    }

    private static void drawExtinguisherFigure(
            GuiGraphicsExtractor guiGraphics,
            int x,
            int y,
            long now,
            PassVisualState visualState,
            int ember,
            int gold,
            int cool,
            int mist,
            int muted
    ) {
        float pulse = 0.5F + 0.5F * Mth.sin(now / 160.0F);
        int handleDrop = visualState == PassVisualState.SQUEEZING || visualState == PassVisualState.SWEEPING ? 4 : 0;
        int nozzleShift = visualState == PassVisualState.SWEEPING ? Math.round(Mth.sin(now / 115.0F) * 10.0F) : 0;
        int pinLift = visualState == PassVisualState.PULLING ? Math.min(12, (int) ((now - lastPullStartedAt) / 45L)) : 0;
        int pinOffset = visualState == PassVisualState.PULLED || visualState == PassVisualState.SQUEEZING || visualState == PassVisualState.SWEEPING ? 14 : 0;

        guiGraphics.fill(x + 12, y - 20, x + 38, y + 20, ember);
        guiGraphics.fillGradient(x + 14, y - 18, x + 36, y + 18, 0xA0FFFFFF, 0x10FFFFFF);
        guiGraphics.fill(x + 18, y - 28, x + 40, y - 24 + handleDrop, muted);
        guiGraphics.fill(x + 14, y - 31, x + 22, y - 28 + handleDrop, gold);
        guiGraphics.fill(x + 26 + pinOffset, y - 34 - pinLift, x + 31 + pinOffset, y - 25 - pinLift, gold);
        guiGraphics.fill(x + 31 + pinOffset, y - 33 - pinLift, x + 36 + pinOffset, y - 28 - pinLift, gold);

        guiGraphics.fill(x + 38, y - 24, x + 48, y - 20, muted);
        guiGraphics.fill(x + 46, y - 21, x + 56 + nozzleShift, y - 18, muted);
        guiGraphics.fill(x + 54 + nozzleShift, y - 20, x + 62 + nozzleShift, y - 16, cool);

        if (visualState == PassVisualState.PULLING) {
            int sparkle = 0x80FFFFFF | ((int) (pulse * 64) << 16);
            guiGraphics.fill(x + 38, y - 38, x + 40, y - 26, sparkle);
            guiGraphics.fill(x + 42, y - 34, x + 50, y - 32, sparkle);
        }

        if (visualState == PassVisualState.SQUEEZING || visualState == PassVisualState.SWEEPING) {
            int streamWidth = visualState == PassVisualState.SWEEPING ? 26 : 18;
            guiGraphics.fillGradient(
                    x + 62 + nozzleShift,
                    y - 22,
                    x + 62 + nozzleShift + streamWidth,
                    y - 10,
                    0xB0DDF6F2,
                    0x10DDF6F2
            );
            guiGraphics.fillGradient(
                    x + 62 + nozzleShift,
                    y - 17,
                    x + 70 + nozzleShift + streamWidth,
                    y - 6,
                    0x70C4E7EA,
                    0x00C4E7EA
            );
        }

        if (visualState == PassVisualState.PULLED) {
            int readyGlow = alpha((int) (55 + pulse * 40), gold);
            guiGraphics.fill(x + 7, y - 25, x + 43, y + 23, readyGlow);
        }
    }

    private static void drawPassSteps(
            GuiGraphicsExtractor guiGraphics,
            Font font,
            int x,
            int y,
            PassVisualState visualState,
            long now,
            int ember,
            int gold,
            int cool,
            int mist,
            int muted,
            int ink
    ) {
        drawStep(guiGraphics, font, x, y - 24, "P", "Pull", isStepActive(visualState, 0), ember, gold, muted, ink, now);
        drawStep(guiGraphics, font, x + 56, y - 24, "A", "Armed", isStepActive(visualState, 1), ember, gold, muted, ink, now);
        drawStep(guiGraphics, font, x, y + 10, "S", "Squeeze", isStepActive(visualState, 2), ember, cool, muted, ink, now);
        drawStep(guiGraphics, font, x + 56, y + 10, "S", "Sweep", isStepActive(visualState, 3), ember, mist, muted, ink, now);
    }

    private static void drawStep(
            GuiGraphicsExtractor guiGraphics,
            Font font,
            int x,
            int y,
            String letter,
            String word,
            boolean active,
            int accentA,
            int accentB,
            int muted,
            int ink,
            long now
    ) {
        float shimmer = 0.5F + 0.5F * Mth.sin((now + x * 9L) / 180.0F);
        int background = active ? alpha((int) (90 + shimmer * 45), accentA) : 0x4020282E;
        int border = active ? blend(accentA, accentB, shimmer) : 0xFF4F5860;
        int text = active ? ink : muted;

        guiGraphics.fill(x, y, x + 50, y + 26, background);
        guiGraphics.horizontalLine(x, x + 49, y, border);
        guiGraphics.horizontalLine(x, x + 49, y + 25, border);
        guiGraphics.verticalLine(x, y, y + 25, border);
        guiGraphics.verticalLine(x + 49, y, y + 25, border);

        guiGraphics.text(font, letter, x + 6, y + 6, text, false);
        guiGraphics.text(font, word, x + 18, y + 6, text, false);
    }

    private static void drawPressureBand(GuiGraphicsExtractor guiGraphics, int x, int y, int width, PassVisualState visualState, int ember, int gold, int cool, int mist) {
        float fill = switch (visualState) {
            case LOCKED -> 0.18F;
            case PULLING -> 0.38F;
            case PULLED -> 0.62F;
            case SQUEEZING -> 0.82F;
            case SWEEPING -> 1.0F;
        };

        int fillWidth = Math.max(10, Math.round(width * fill));
        int color = activeColor(visualState, ember, gold, cool, mist);
        guiGraphics.fill(x, y, x + width, y + 6, 0x60252C31);
        guiGraphics.fill(x, y, x + fillWidth, y + 6, color);
    }

    private static boolean isStepActive(PassVisualState visualState, int step) {
        return switch (visualState) {
            case LOCKED -> step == 0;
            case PULLING -> step == 0;
            case PULLED -> step <= 1;
            case SQUEEZING -> step <= 2;
            case SWEEPING -> true;
        };
    }

    private static String helperText(ExtinguisherState state, PassVisualState visualState) {
        return switch (visualState) {
            case LOCKED -> "Press P to break the safety pin";
            case PULLING -> "Pin is coming free, keep the extinguisher steady";
            case PULLED -> state == ExtinguisherState.AIMED ? "Aim at the fire base, then squeeze" : "Safety pin out, right click to start discharge";
            case SQUEEZING -> "Hold pressure, the charge is leaving the nozzle";
            case SWEEPING -> "Move side to side across the base of the fire";
        };
    }

    private static String labelForState(PassVisualState visualState) {
        return switch (visualState) {
            case LOCKED -> "SAFE LOCKED";
            case PULLING -> "PULLING PIN";
            case PULLED -> "READY TO DISCHARGE";
            case SQUEEZING -> "SQUEEZING AGENT";
            case SWEEPING -> "SWEEPING ARC";
        };
    }

    private static int activeColor(PassVisualState visualState, int ember, int gold, int cool, int mist) {
        return switch (visualState) {
            case LOCKED, PULLING -> ember;
            case PULLED -> gold;
            case SQUEEZING -> cool;
            case SWEEPING -> mist;
        };
    }

    private static int alpha(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int blend(int from, int to, float amount) {
        amount = Mth.clamp(amount, 0.0F, 1.0F);
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = Mth.floor(Mth.lerp(amount, fr, tr));
        int g = Mth.floor(Mth.lerp(amount, fg, tg));
        int b = Mth.floor(Mth.lerp(amount, fb, tb));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private enum PassVisualState {
        LOCKED,
        PULLING,
        PULLED,
        SQUEEZING,
        SWEEPING
    }
}
