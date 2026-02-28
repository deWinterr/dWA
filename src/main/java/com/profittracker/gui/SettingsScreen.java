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

/**
 * Settings GUI for the Profit Tracker, styled in a salmon-pink theme
 * with dropdown menus inspired by NoammAddons' ClickGui.
 */
public class SettingsScreen extends Screen {

    // Salmon pink color palette
    private static final int ACCENT = 0xFFFA8072;          // Salmon
    private static final int ACCENT_DARK = 0xFFE06050;     // Darker salmon
    private static final int BG_PANEL = 0xE6141414;        // Dark panel bg
    private static final int BG_SETTING = 0xC00A0A0A;      // Setting row bg
    private static final int BG_HOVER = 0x40FFFFFF;        // Hover overlay
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int TEXT_DARK = 0xFF666666;

    private final List<SettingEntry> settings = new ArrayList<>();
    private int panelX, panelY, panelW, panelH;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Active dropdown state
    private DropdownEntry activeDropdown = null;

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

        settings.add(new DropdownEntry("Title Color", colorNames(), colorIndex(config.titleColor),
                idx -> config.titleColor = COLOR_CODES[idx]));

        settings.add(new DropdownEntry("Label Color", colorNames(), colorIndex(config.labelColor),
                idx -> config.labelColor = COLOR_CODES[idx]));

        settings.add(new DropdownEntry("Value Color", colorNames(), colorIndex(config.valueColor),
                idx -> config.valueColor = COLOR_CODES[idx]));

        settings.add(new DropdownEntry("Time Color", colorNames(), colorIndex(config.timeColor),
                idx -> config.timeColor = COLOR_CODES[idx]));

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

    // Color code mapping
    private static final String[] COLOR_CODES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
    private static final String[] COLOR_NAMES = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Pink", "Yellow", "White"};
    private static final int[] COLOR_VALUES = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
            0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
            0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };

    private String[] colorNames() { return COLOR_NAMES; }

    private int colorIndex(String code) {
        for (int i = 0; i < COLOR_CODES.length; i++) {
            if (COLOR_CODES[i].equals(code)) return i;
        }
        return 15; // default white
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

            // Hover bar
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
        java.util.function.IntConsumer onChange;

        DropdownEntry(String name, String[] options, int selected, java.util.function.IntConsumer onChange) {
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

            // For color dropdowns, draw a color swatch
            if (name.contains("Color") && selected < COLOR_VALUES.length) {
                ctx.fill(x + w - valW - 18, y + 5, x + w - valW - 10, y + 15, COLOR_VALUES[selected]);
            }
        }

        void renderDropdownOverlay(DrawContext ctx, int mx, int my) {
            if (!expanded) return;
            MinecraftClient mc = MinecraftClient.getInstance();

            int dropX = x + 4;
            int dropY = y + 20;
            int dropW = w - 8;
            int optH = 16;
            int totalH = options.length * optH;

            // Dropdown background
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

                // For color dropdowns, show swatch
                if (name.contains("Color") && i < COLOR_VALUES.length) {
                    ctx.fill(dropX + dropW - 14, oy + 3, dropX + dropW - 4, oy + optH - 3, COLOR_VALUES[i]);
                }

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
                } else {
                    activeDropdown = null;
                }
                return true;
            }
            return false;
        }
    }
}
