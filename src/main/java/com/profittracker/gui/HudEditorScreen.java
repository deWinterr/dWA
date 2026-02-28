package com.profittracker.gui;

import com.profittracker.SkyblockProfitTracker;
import com.profittracker.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * HUD editor screen that lets users drag the profit tracker HUD around
 * and scroll to resize. Inspired by NoammAddons' HudEditorScreen.
 *
 * - Left click + drag to move
 * - Scroll wheel to adjust scale
 * - ESC to save and exit
 */
public class HudEditorScreen extends Screen {

    private static final int ACCENT = 0xFFFA8072;
    private static final int BORDER_NORMAL = 0x40FFFFFF;
    private static final int BORDER_HOVER = 0xFFFA8072;
    private static final int BG_PREVIEW = 0x96141414;

    private boolean isDragging = false;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    // Preview HUD dimensions (calculated during render)
    private int previewW = 120;
    private int previewH = 60;

    // Track mouse position from render() for use in click handlers
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        ModConfig config = SkyblockProfitTracker.config;
        TextRenderer tr = textRenderer;

        // Dim background
        ctx.fill(0, 0, width, height, 0x60000000);

        int hudX = config.hudX;
        int hudY = config.hudY;
        float scale = config.hudScale;

        // Handle dragging
        if (isDragging) {
            config.hudX = Math.max(0, Math.min((int)(mouseX - dragOffsetX), width - previewW));
            config.hudY = Math.max(0, Math.min((int)(mouseY - dragOffsetY), height - previewH));
            hudX = config.hudX;
            hudY = config.hudY;
        }

        // Calculate preview box size
        String[] previewLines = {
                "\u00a76\u00a7l\u26cf Profit Tracker",
                "\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
                "\u00a77Profit: \u00a7a$1,234,567",
                "\u00a77$/hr: \u00a7a$4,567,890",
                "\u00a77Time: \u00a7f1h 23m 45s",
                "\u00a77Items: \u00a7f12,345",
                "\u00a78[BZ Instant Sell]"
        };

        int lineH = 11;
        int padding = 4;
        int maxLineW = 0;
        for (String line : previewLines) {
            int lw = tr.getWidth(line);
            if (lw > maxLineW) maxLineW = lw;
        }
        previewW = maxLineW + padding * 2;
        previewH = previewLines.length * lineH + padding * 2;

        // Check hover
        boolean hovered = mouseX >= hudX - padding && mouseX <= hudX + previewW
                && mouseY >= hudY - padding && mouseY <= hudY + previewH;

        // Draw preview background
        ctx.fill(hudX - padding, hudY - padding, hudX + previewW, hudY + previewH, BG_PREVIEW);

        // Border
        int borderColor = (isDragging || hovered) ? BORDER_HOVER : BORDER_NORMAL;
        ctx.fill(hudX - padding, hudY - padding, hudX + previewW, hudY - padding + 1, borderColor);
        ctx.fill(hudX - padding, hudY + previewH - 1, hudX + previewW, hudY + previewH, borderColor);
        ctx.fill(hudX - padding, hudY - padding, hudX - padding + 1, hudY + previewH, borderColor);
        ctx.fill(hudX + previewW - 1, hudY - padding, hudX + previewW, hudY + previewH, borderColor);

        // Draw preview text
        int ly = hudY;
        for (String line : previewLines) {
            ctx.drawTextWithShadow(tr, line, hudX, ly, 0xFFFFFFFF);
            ly += lineH;
        }

        // Draw element name when dragging
        if (isDragging) {
            ctx.drawCenteredTextWithShadow(tr, Text.literal("Profit Tracker"),
                    width / 2, 10, 0xFFFFFFFF);
        }

        // Scale indicator
        ctx.drawCenteredTextWithShadow(tr, Text.literal("\u00a77Scale: \u00a7f" + String.format("%.1fx", scale)),
                width / 2, height / 2, 0xFFFFFFFF);

        // Instructions
        ctx.drawCenteredTextWithShadow(tr, Text.literal("\u00a78Drag to move | Scroll to resize | ESC to save"),
                width / 2, height - 20, 0xFF666666);

        // Position display
        ctx.drawCenteredTextWithShadow(tr, Text.literal("\u00a78Position: " + hudX + ", " + hudY),
                width / 2, height - 10, 0xFF444444);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            ModConfig config = SkyblockProfitTracker.config;
            int padding = 4;

            if (lastMouseX >= config.hudX - padding && lastMouseX <= config.hudX + previewW
                    && lastMouseY >= config.hudY - padding && lastMouseY <= config.hudY + previewH) {
                isDragging = true;
                dragOffsetX = (float)(lastMouseX - config.hudX);
                dragOffsetY = (float)(lastMouseY - config.hudY);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            isDragging = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ModConfig config = SkyblockProfitTracker.config;
        float increment = (float)(verticalAmount * 0.1);
        config.hudScale = Math.max(0.5f, Math.min(3.0f, config.hudScale + increment));
        return true;
    }

    @Override
    public void close() {
        SkyblockProfitTracker.config.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
