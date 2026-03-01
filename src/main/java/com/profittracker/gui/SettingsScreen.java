package com.profittracker.gui;

import com.profittracker.SkyblockProfitTracker;
import com.profittracker.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Settings GUI for the Profit Tracker, styled in a salmon-pink theme
 * with full RGB color pickers (HSB square + hue bar).
 */
public class SettingsScreen extends Screen {

    // Salmon pink color palette
    private static final int ACCENT = 0xFFFA8072;
    private static final int BG_PANEL = 0xE6141414;
    private static final int BG_SETTING = 0xC00A0A0A;
    private static final int BG_HOVER = 0x40FFFFFF;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DARK = 0xFF666666;

    private final List<SettingEntry> settings = new ArrayList<>();
    private int panelX, panelY, panelW, panelH;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Active overlay state — only one can be open at a time
    private DropdownEntry activeDropdown = null;
    private ColorPickerEntry activeColorPicker = null;

    // Track mouse position from render() for use in click handlers
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public SettingsScreen() {
        super(Text.literal("Profit Tracker Settings"));
    }

    @Override
    protected void init() {
        super.init();
        settings.clear();

        panelW = 240;
        panelH = Math.min(height - 60, 340);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        ModConfig config = SkyblockProfitTracker.config;

        // Build settings list
        settings.add(new DropdownEntry("Pricing Mode", new String[]{"Bazaar Instant Sell", "Bazaar Sell Offer", "NPC Prices"},
                switch (config.pricingMode) {
                    case "bazaar_buy" -> 1;
                    case "npc" -> 2;
                    default -> 0;
                }, idx -> {
            config.pricingMode = switch (idx) {
                case 1 -> "bazaar_buy";
                case 2 -> "npc";
                default -> "bazaar_sell";
            };
        }));

        settings.add(new DropdownEntry("Gemstone Rarity", new String[]{"Flawed", "Fine", "Flawless"},
                config.gemstoneRarity - 1, idx -> config.gemstoneRarity = idx + 1));

        // RGB color pickers
        settings.add(new ColorPickerEntry("Label Color", config.labelColor, c -> config.labelColor = c));
        settings.add(new ColorPickerEntry("Value Color", config.valueColor, c -> config.valueColor = c));
        settings.add(new ColorPickerEntry("Time Color", config.timeColor, c -> config.timeColor = c));
        settings.add(new ColorPickerEntry("Separator Color", config.separatorColor, c -> config.separatorColor = c));
        settings.add(new ColorPickerEntry("Chat Prefix Color", config.chatPrefixColor, c -> config.chatPrefixColor = c));

        settings.add(new ToggleEntry("Show Profit", config.showProfit, v -> config.showProfit = v));
        settings.add(new ToggleEntry("Show $/hr", config.showProfitPerHour, v -> config.showProfitPerHour = v));
        settings.add(new ToggleEntry("Show Time", config.showTime, v -> config.showTime = v));
        settings.add(new ToggleEntry("Show Items", config.showItems, v -> config.showItems = v));
        settings.add(new ToggleEntry("Show Pricing Mode", config.showPricingMode, v -> config.showPricingMode = v));
        settings.add(new ToggleEntry("Show Item Breakdown", config.showItemBreakdown, v -> config.showItemBreakdown = v));
        settings.add(new ToggleEntry("Show Separators", config.showSeparators, v -> config.showSeparators = v));

        settings.add(new DropdownEntry("Breakdown Items", new String[]{"3", "5", "8", "10"},
                switch (config.maxBreakdownItems) {
                    case 3 -> 0;
                    case 8 -> 2;
                    case 10 -> 3;
                    default -> 1;
                }, idx -> config.maxBreakdownItems = switch (idx) {
                    case 0 -> 3;
                    case 2 -> 8;
                    case 3 -> 10;
                    default -> 5;
                }));

        settings.add(new DropdownEntry("Idle Timeout", new String[]{"30s", "60s", "120s", "300s"},
                switch (config.sessionTimeoutSeconds) {
                    case 30 -> 0;
                    case 120 -> 2;
                    case 300 -> 3;
                    default -> 1;
                }, idx -> config.sessionTimeoutSeconds = switch (idx) {
                    case 0 -> 30;
                    case 2 -> 120;
                    case 3 -> 300;
                    default -> 60;
                }));

        // Calculate max scroll
        int contentHeight = 0;
        for (SettingEntry s : settings) {
            contentHeight += s.getCollapsedHeight() + 3;
        }
        maxScroll = Math.max(0, contentHeight - (panelH - 40));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // Dim background
        ctx.fill(0, 0, width, height, 0x88000000);

        // Panel background
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_PANEL);

        // Title bar with accent
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 2, ACCENT);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("\u00a7l Profit Tracker Settings"),
                panelX + panelW / 2, panelY + 10, ACCENT);

        // Separator under title
        ctx.fill(panelX + 10, panelY + 26, panelX + panelW - 10, panelY + 27, 0x30FFFFFF);

        // Enable scissor for scrollable area
        int contentTop = panelY + 32;
        int contentBottom = panelY + panelH - 8;
        ctx.enableScissor(panelX, contentTop, panelX + panelW, contentBottom);

        int yPos = contentTop - scrollOffset;
        for (SettingEntry setting : settings) {
            setting.x = panelX + 8;
            setting.y = yPos;
            setting.w = panelW - 16;

            // Only render if visible
            if (yPos + setting.getTotalHeight() > contentTop - 20 && yPos < contentBottom + 20) {
                setting.render(ctx, mouseX, mouseY);
            }

            yPos += setting.getTotalHeight() + 3;
        }

        ctx.disableScissor();

        // Render active dropdown overlay (above scissor so it's not clipped)
        if (activeDropdown != null && activeDropdown.expanded) {
            activeDropdown.renderDropdownOverlay(ctx, mouseX, mouseY);
        }

        // Render active color picker overlay (above scissor so it's not clipped)
        if (activeColorPicker != null && activeColorPicker.pickerOpen) {
            activeColorPicker.renderPickerOverlay(ctx, mouseX, mouseY);
        }

        // Scrollbar
        int contentHeight = 0;
        for (SettingEntry s : settings) contentHeight += s.getTotalHeight() + 3;
        if (contentHeight > (contentBottom - contentTop)) {
            int barHeight = contentBottom - contentTop;
            float thumbRatio = (float)(contentBottom - contentTop) / contentHeight;
            int thumbH = Math.max(10, (int)(barHeight * thumbRatio));
            int thumbY = contentTop + (int)((float) scrollOffset / maxScroll * (barHeight - thumbH));
            ctx.fill(panelX + panelW - 4, contentTop, panelX + panelW - 2, contentBottom, 0x20FFFFFF);
            ctx.fill(panelX + panelW - 4, thumbY, panelX + panelW - 2, thumbY + thumbH, (ACCENT & 0x00FFFFFF) | 0xA0000000);
        }

        // Footer hint
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("ESC to save & close"),
                panelX + panelW / 2, panelY + panelH - 6, TEXT_DARK);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        // Check if clicking on an open color picker overlay first
        if (activeColorPicker != null && activeColorPicker.pickerOpen) {
            if (activeColorPicker.clickPickerOverlay(lastMouseX, lastMouseY)) {
                return true;
            }
            // Click outside picker closes it
            activeColorPicker.pickerOpen = false;
            activeColorPicker = null;
            return true;
        }

        // Check if clicking on an open dropdown overlay first
        if (activeDropdown != null && activeDropdown.expanded) {
            if (activeDropdown.clickDropdownOverlay(lastMouseX, lastMouseY)) {
                activeDropdown = null;
                return true;
            }
            // Click outside dropdown closes it
            activeDropdown.expanded = false;
            activeDropdown = null;
            return true;
        }

        int contentTop = panelY + 32;
        int contentBottom = panelY + panelH - 8;

        for (SettingEntry setting : settings) {
            if (lastMouseY >= contentTop && lastMouseY <= contentBottom) {
                if (setting.click(lastMouseX, lastMouseY)) return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (activeColorPicker != null) {
            activeColorPicker.draggingSV = false;
            activeColorPicker.draggingHue = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int)(verticalAmount * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
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

    // ==================== HSB -> RGB conversion ====================

    /** Convert HSB (h, s, v all 0-1) to 0xAARRGGBB with full alpha. */
    static int hsbToRgb(float h, float s, float v) {
        float c = v * s;
        float hp = h * 6f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float m = v - c;
        float rf, gf, bf;
        if (hp < 1)      { rf = c; gf = x; bf = 0; }
        else if (hp < 2) { rf = x; gf = c; bf = 0; }
        else if (hp < 3) { rf = 0; gf = c; bf = x; }
        else if (hp < 4) { rf = 0; gf = x; bf = c; }
        else if (hp < 5) { rf = x; gf = 0; bf = c; }
        else              { rf = c; gf = 0; bf = x; }
        int r = Math.min(255, Math.max(0, (int)((rf + m) * 255 + 0.5f)));
        int g = Math.min(255, Math.max(0, (int)((gf + m) * 255 + 0.5f)));
        int b = Math.min(255, Math.max(0, (int)((bf + m) * 255 + 0.5f)));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ==================== Setting Entry Types ====================

    private abstract static class SettingEntry {
        String name;
        int x, y, w;

        SettingEntry(String name) {
            this.name = name;
        }

        abstract void render(DrawContext ctx, int mx, int my);
        abstract boolean click(double mx, double my);
        abstract int getCollapsedHeight();

        int getTotalHeight() {
            return getCollapsedHeight();
        }
    }

    // --- Toggle ---
    private static class ToggleEntry extends SettingEntry {
        boolean value;
        java.util.function.Consumer<Boolean> onChange;

        ToggleEntry(String name, boolean value, java.util.function.Consumer<Boolean> onChange) {
            super(name);
            this.value = value;
            this.onChange = onChange;
        }

        @Override
        int getCollapsedHeight() { return 20; }

        @Override
        void render(DrawContext ctx, int mx, int my) {
            boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + 20;
            ctx.fill(x, y, x + w, y + 20, BG_SETTING);
            if (hovered) ctx.fill(x, y, x + w, y + 20, BG_HOVER);

            if (hovered) {
                ctx.fill(x, y + 3, x + 2, y + 17, ACCENT);
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            int textX = hovered ? x + 10 : x + 8;
            ctx.drawTextWithShadow(mc.textRenderer, name, textX, y + 6, TEXT_WHITE);

            // Toggle indicator
            int toggleX = x + w - 30;
            int toggleY = y + 5;
            int toggleW = 22;
            int toggleH = 10;

            if (value) {
                ctx.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, ACCENT);
                ctx.fill(toggleX + toggleW - 8, toggleY + 1, toggleX + toggleW - 1, toggleY + toggleH - 1, TEXT_WHITE);
            } else {
                ctx.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, 0xFF333333);
                ctx.fill(toggleX + 1, toggleY + 1, toggleX + 8, toggleY + toggleH - 1, 0xFF888888);
            }
        }

        @Override
        boolean click(double mx, double my) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + 20) {
                value = !value;
                onChange.accept(value);
                return true;
            }
            return false;
        }
    }

    // --- Dropdown ---
    private class DropdownEntry extends SettingEntry {
        String[] options;
        int selected;
        boolean expanded = false;
        IntConsumer onChange;

        DropdownEntry(String name, String[] options, int selected, IntConsumer onChange) {
            super(name);
            this.options = options;
            this.selected = Math.max(0, Math.min(selected, options.length - 1));
            this.onChange = onChange;
        }

        @Override
        int getCollapsedHeight() { return 20; }

        @Override
        void render(DrawContext ctx, int mx, int my) {
            boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + 20;
            ctx.fill(x, y, x + w, y + 20, BG_SETTING);
            if (hovered) ctx.fill(x, y, x + w, y + 20, BG_HOVER);

            if (hovered) {
                ctx.fill(x, y + 3, x + 2, y + 17, ACCENT);
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            int textX = hovered ? x + 10 : x + 8;
            ctx.drawTextWithShadow(mc.textRenderer, name, textX, y + 6, TEXT_WHITE);

            // Selected value on right
            String val = "\u00a77" + options[selected];
            int valW = mc.textRenderer.getWidth(val);
            ctx.drawTextWithShadow(mc.textRenderer, val, x + w - valW - 8, y + 6, TEXT_WHITE);
        }

        void renderDropdownOverlay(DrawContext ctx, int mx, int my) {
            if (!expanded) return;
            MinecraftClient mc = MinecraftClient.getInstance();

            int dropX = x + 4;
            int dropY = y + 20;
            int dropW = w - 8;
            int optH = 16;
            int totalH = options.length * optH;

            ctx.fill(dropX, dropY, dropX + dropW, dropY + totalH, 0xF0050505);

            int oy = dropY;
            for (int i = 0; i < options.length; i++) {
                boolean optHov = mx >= dropX && mx <= dropX + dropW && my >= oy && my <= oy + optH;
                if (optHov) {
                    ctx.fill(dropX, oy, dropX + dropW, oy + optH, 0x30FFFFFF);
                }
                if (i == selected) {
                    ctx.fill(dropX, oy + 2, dropX + 2, oy + optH - 2, ACCENT);
                }

                int color = i == selected ? ACCENT : (optHov ? TEXT_WHITE : TEXT_GRAY);
                ctx.drawTextWithShadow(mc.textRenderer, options[i], dropX + 10, oy + 4, color);

                oy += optH;
            }
        }

        boolean clickDropdownOverlay(double mx, double my) {
            if (!expanded) return false;

            int dropX = x + 4;
            int dropY = y + 20;
            int dropW = w - 8;
            int optH = 16;

            int oy = dropY;
            for (int i = 0; i < options.length; i++) {
                if (mx >= dropX && mx <= dropX + dropW && my >= oy && my <= oy + optH) {
                    selected = i;
                    onChange.accept(i);
                    expanded = false;
                    return true;
                }
                oy += optH;
            }
            return false;
        }

        @Override
        boolean click(double mx, double my) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + 20) {
                expanded = !expanded;
                if (expanded) {
                    activeDropdown = this;
                    // Close color picker if open
                    if (activeColorPicker != null) {
                        activeColorPicker.pickerOpen = false;
                        activeColorPicker = null;
                    }
                } else {
                    activeDropdown = null;
                }
                return true;
            }
            return false;
        }
    }

    // --- RGB Color Picker (HSB square + hue bar, like NoamAddons) ---
    private class ColorPickerEntry extends SettingEntry {
        int currentColor; // 0xRRGGBB (no alpha)
        IntConsumer onChange;
        boolean pickerOpen = false;

        // HSB state
        float hue, sat, bri;

        // Drag state
        boolean draggingSV = false;
        boolean draggingHue = false;

        // Picker dimensions
        static final int SV_SIZE = 80;
        static final int HUE_W = 12;
        static final int GAP = 6;
        static final int PICKER_PAD = 6;

        ColorPickerEntry(String name, int color, IntConsumer onChange) {
            super(name);
            this.currentColor = color & 0xFFFFFF;
            this.onChange = onChange;
            rgbToHsb(this.currentColor);
        }

        private void rgbToHsb(int rgb) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            float rf = r / 255f, gf = g / 255f, bf = b / 255f;
            float max = Math.max(rf, Math.max(gf, bf));
            float min = Math.min(rf, Math.min(gf, bf));
            float delta = max - min;
            bri = max;
            sat = max == 0 ? 0 : delta / max;
            if (delta == 0) {
                hue = 0;
            } else if (max == rf) {
                hue = ((gf - bf) / delta + 6f) % 6f / 6f;
            } else if (max == gf) {
                hue = ((bf - rf) / delta + 2f) / 6f;
            } else {
                hue = ((rf - gf) / delta + 4f) / 6f;
            }
        }

        private void updateColor() {
            currentColor = hsbToRgb(hue, sat, bri) & 0xFFFFFF;
            onChange.accept(currentColor);
        }

        @Override
        int getCollapsedHeight() { return 20; }

        @Override
        void render(DrawContext ctx, int mx, int my) {
            boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + 20;
            ctx.fill(x, y, x + w, y + 20, BG_SETTING);
            if (hovered) ctx.fill(x, y, x + w, y + 20, BG_HOVER);

            if (hovered) {
                ctx.fill(x, y + 3, x + 2, y + 17, ACCENT);
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            int textX = hovered ? x + 10 : x + 8;
            ctx.drawTextWithShadow(mc.textRenderer, name, textX, y + 6, TEXT_WHITE);

            // Color swatch on right side
            int swatchX = x + w - 20;
            int swatchY = y + 4;
            // Border
            ctx.fill(swatchX - 1, swatchY - 1, swatchX + 13, swatchY + 13, 0xFF333333);
            // Swatch
            ctx.fill(swatchX, swatchY, swatchX + 12, swatchY + 12, 0xFF000000 | currentColor);
        }

        void renderPickerOverlay(DrawContext ctx, int mx, int my) {
            if (!pickerOpen) return;

            // Handle dragging updates every frame
            int px = x + PICKER_PAD;
            int py = y + 24;
            int hueBarX = px + SV_SIZE + GAP;

            if (draggingSV) {
                sat = clamp01((mx - px) / (float) SV_SIZE);
                bri = clamp01(1f - (my - py) / (float) SV_SIZE);
                updateColor();
            }
            if (draggingHue) {
                hue = clamp01((my - py) / (float) SV_SIZE);
                updateColor();
            }

            MinecraftClient mc = MinecraftClient.getInstance();

            int totalW = SV_SIZE + GAP + HUE_W + GAP + 24;
            int totalH = SV_SIZE;

            // Background for picker
            ctx.fill(px - PICKER_PAD, py - PICKER_PAD,
                    px + totalW + PICKER_PAD, py + totalH + PICKER_PAD + 14,
                    0xF0050505);

            // Border
            ctx.fill(px - PICKER_PAD, py - PICKER_PAD,
                    px + totalW + PICKER_PAD, py - PICKER_PAD + 1, 0xFF333333);
            ctx.fill(px - PICKER_PAD, py + totalH + PICKER_PAD + 13,
                    px + totalW + PICKER_PAD, py + totalH + PICKER_PAD + 14, 0xFF333333);
            ctx.fill(px - PICKER_PAD, py - PICKER_PAD,
                    px - PICKER_PAD + 1, py + totalH + PICKER_PAD + 14, 0xFF333333);
            ctx.fill(px + totalW + PICKER_PAD - 1, py - PICKER_PAD,
                    px + totalW + PICKER_PAD, py + totalH + PICKER_PAD + 14, 0xFF333333);

            // Draw SV square (2px blocks for performance)
            int step = 2;
            for (int sy = 0; sy < SV_SIZE; sy += step) {
                float b = 1f - (float) sy / SV_SIZE;
                for (int sx = 0; sx < SV_SIZE; sx += step) {
                    float s = (float) sx / SV_SIZE;
                    int c = hsbToRgb(hue, s, b);
                    ctx.fill(px + sx, py + sy, px + sx + step, py + sy + step, c);
                }
            }

            // SV cursor (crosshair)
            int curX = px + (int)(sat * (SV_SIZE - 1));
            int curY = py + (int)((1f - bri) * (SV_SIZE - 1));
            // Outer white
            ctx.fill(curX - 3, curY, curX - 1, curY + 1, 0xFFFFFFFF);
            ctx.fill(curX + 2, curY, curX + 4, curY + 1, 0xFFFFFFFF);
            ctx.fill(curX, curY - 3, curX + 1, curY - 1, 0xFFFFFFFF);
            ctx.fill(curX, curY + 2, curX + 1, curY + 4, 0xFFFFFFFF);
            // Inner black
            ctx.fill(curX - 1, curY, curX + 2, curY + 1, 0xFF000000);
            ctx.fill(curX, curY - 1, curX + 1, curY + 2, 0xFF000000);

            // Draw hue bar (vertical rainbow)
            for (int hy = 0; hy < SV_SIZE; hy += step) {
                float h = (float) hy / SV_SIZE;
                int c = hsbToRgb(h, 1f, 1f);
                ctx.fill(hueBarX, py + hy, hueBarX + HUE_W, py + hy + step, c);
            }

            // Hue cursor (horizontal lines)
            int hueCurY = py + (int)(hue * (SV_SIZE - 1));
            ctx.fill(hueBarX - 1, hueCurY - 1, hueBarX + HUE_W + 1, hueCurY, 0xFFFFFFFF);
            ctx.fill(hueBarX - 1, hueCurY + 1, hueBarX + HUE_W + 1, hueCurY + 2, 0xFFFFFFFF);

            // Color preview swatch
            int prevX = hueBarX + HUE_W + GAP;
            ctx.fill(prevX - 1, py - 1, prevX + 21, py + 21, 0xFF333333);
            ctx.fill(prevX, py, prevX + 20, py + 20, 0xFF000000 | currentColor);

            // Hex display
            String hex = String.format("#%06X", currentColor);
            ctx.drawTextWithShadow(mc.textRenderer, hex, px, py + SV_SIZE + 3, TEXT_GRAY);
        }

        boolean clickPickerOverlay(double mx, double my) {
            if (!pickerOpen) return false;

            int px = x + PICKER_PAD;
            int py = y + 24;
            int hueBarX = px + SV_SIZE + GAP;

            // Check SV square
            if (mx >= px && mx < px + SV_SIZE && my >= py && my < py + SV_SIZE) {
                draggingSV = true;
                sat = clamp01((float)(mx - px) / SV_SIZE);
                bri = clamp01(1f - (float)(my - py) / SV_SIZE);
                updateColor();
                return true;
            }

            // Check hue bar
            if (mx >= hueBarX && mx < hueBarX + HUE_W && my >= py && my < py + SV_SIZE) {
                draggingHue = true;
                hue = clamp01((float)(my - py) / SV_SIZE);
                updateColor();
                return true;
            }

            return false;
        }

        @Override
        boolean click(double mx, double my) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + 20) {
                pickerOpen = !pickerOpen;
                if (pickerOpen) {
                    activeColorPicker = this;
                    // Close dropdown if open
                    if (activeDropdown != null) {
                        activeDropdown.expanded = false;
                        activeDropdown = null;
                    }
                } else {
                    activeColorPicker = null;
                }
                return true;
            }
            return false;
        }
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
