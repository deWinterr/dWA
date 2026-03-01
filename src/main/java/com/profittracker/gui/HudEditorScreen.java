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
 * and scroll to resize.
 *
 * - Left click + drag to move
 * - Scroll wheel to adjust scale
 * - ESC to save and exit
 */
public class HudEditorScreen extends Screen {

    private static final int BORDER_NORMAL = 0x40FFFFFF;
    private static final int BORDER_HOVER = 0xFFFA8072;
    private static final int LINE_HEIGHT = 11;

    private boolean isDragging = false;
    private float dragOffsetX = 0;
    private float dragOffsetY = 0;

    // Preview HUD dimensions (calculated during render, in SCALED pixels)
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
        float scale = config.hudScale;

        // Dim background
        ctx.fill(0, 0, width, height, 0x60000000);

        int hudX = config.hudX;
        int hudY = config.hudY;

        // Handle dragging
        if (isDragging) {
            config.hudX = Math.max(0, Math.min((int)(mouseX - dragOffsetX), width - (int)(previewW * scale)));
            config.hudY = Math.max(0, Math.min((int)(mouseY - dragOffsetY), height - (int)(previewH * scale)));
            hudX = config.hudX;
            hudY = config.hudY;
        }

        // Build preview lines (no title, no background — matches actual HUD)
        String[][] previewSegments = {
                {"Profit: ", "$1,234,567"},
                {"$/hr: ", "$4,567,890"},
                {"Time: ", "1h 23m 45s"},
                {"Items: ", "12,345"},
                {"\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"},
                {"Diamond: ", "$123K", " (x4,200)"},
                {"[BZ Sell | Flawed]"}
        };
        int[][] previewColors = {
                {config.labelColor, config.valueColor},
                {config.labelColor, config.valueColor},
                {config.labelColor, config.timeColor},
                {config.labelColor, config.timeColor},
                {config.separatorColor},
                {0xFFFFFF, config.valueColor, 0x555555},
                {config.separatorColor}
        };

        // Measure unscaled content size
        int maxLineW = 0;
        for (String[] segs : previewSegments) {
            int lineW = 0;
            for (String s : segs) lineW += tr.getWidth(s);
            if (lineW > maxLineW) maxLineW = lineW;
        }
        previewW = maxLineW;
        previewH = previewSegments.length * LINE_HEIGHT;

        // Scaled bounding box for hit-testing
        int scaledW = (int)(previewW * scale);
        int scaledH = (int)(previewH * scale);

        // Check hover against the scaled bounding box
        boolean hovered = mouseX >= hudX && mouseX <= hudX + scaledW
                && mouseY >= hudY && mouseY <= hudY + scaledH;

        // Draw border around scaled area
        int borderColor = (isDragging || hovered) ? BORDER_HOVER : BORDER_NORMAL;
        int pad = 2;
        ctx.fill(hudX - pad, hudY - pad, hudX + scaledW + pad, hudY - pad + 1, borderColor);
        ctx.fill(hudX - pad, hudY + scaledH + pad - 1, hudX + scaledW + pad, hudY + scaledH + pad, borderColor);
        ctx.fill(hudX - pad, hudY - pad, hudX - pad + 1, hudY + scaledH + pad, borderColor);
        ctx.fill(hudX + scaledW + pad - 1, hudY - pad, hudX + scaledW + pad, hudY + scaledH + pad, borderColor);

        // Draw preview text at configured scale (Matrix3x2fStack in 1.21.10)
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(hudX, hudY);
        ctx.getMatrices().scale(scale, scale);

        int lineY = 0;
        for (int i = 0; i < previewSegments.length; i++) {
            int segX = 0;
            for (int j = 0; j < previewSegments[i].length; j++) {
                int color = (j < previewColors[i].length) ? previewColors[i][j] : 0xFFFFFF;
                ctx.drawTextWithShadow(tr, previewSegments[i][j], segX, lineY, 0xFF000000 | color);
                segX += tr.getWidth(previewSegments[i][j]);
            }
            lineY += LINE_HEIGHT;
        }

        ctx.getMatrices().popMatrix();

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
            float scale = config.hudScale;
            int scaledW = (int)(previewW * scale);
            int scaledH = (int)(previewH * scale);

            if (lastMouseX >= config.hudX && lastMouseX <= config.hudX + scaledW
                    && lastMouseY >= config.hudY && lastMouseY <= config.hudY + scaledH) {
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
