package sh.slb.dimthing;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

@Mod(DimThing.MODID)
public class DimThing {
    public static final String MODID = "dimthing";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Default overlay text color (light gray)
    public static int overlayColour = 0xE0E0E0;
    private static Map<ResourceLocation, Integer> DIMENSION_OFFSETS = new HashMap<>();

    public DimThing() {
        MinecraftForge.EVENT_BUS.register(this);

        // Register client config
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // Register config screen button in Mods list
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ConfigScreen(parent)));
    }

    /** Reload offsets + color from config whenever it changes */
    public static void reloadConfig() {
        DIMENSION_OFFSETS.clear();

        for (String entry : Config.DIM_OFFSETS.get()) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                try {
                    ResourceLocation dimId = new ResourceLocation(parts[0].trim());
                    int offset = Integer.parseInt(parts[1].trim());
                    DIMENSION_OFFSETS.put(dimId, offset);
                } catch (Exception e) {
                    LOGGER.error("Invalid dimension offset entry '{}'", entry, e);
                }
            }
        }

        overlayColour = Config.OVERLAY_COLOUR.get();
        LOGGER.info("DimThing loaded {} dimension offsets, overlay colour={}", DIMENSION_OFFSETS.size(), overlayColour);
    }

    public static int getOffset(ResourceLocation dimId) {
        return DIMENSION_OFFSETS.getOrDefault(dimId, 0);
    }

    /** Handles config reload */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void onConfigReload(ModConfigEvent event) {
            if (event.getConfig().getSpec() == Config.SPEC) {
                reloadConfig();
            }
        }
    }

    /** Handles HUD rendering */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class HudRenderer {
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void renderOverlay(CustomizeGuiOverlayEvent.DebugText event) {
            final var mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.renderDebug) return;

            try {
                var pos = mc.player.blockPosition();
                var dimId = mc.player.level().dimension().location();
                int offset = getOffset(dimId);

                String coords = String.format("X: %d, Y: %d, Z: %d",
                        pos.getX(), pos.getY() - offset, pos.getZ());

                event.getGuiGraphics().drawString(mc.font, coords,
                        Config.OVERLAY_X.get(), Config.OVERLAY_Y.get(), overlayColour);
            } catch (Exception e) {
                LOGGER.error("Error rendering overlay", e);
            }
        }
    }

    /** Client config definition */
    public static class Config {
        private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        public static final ForgeConfigSpec.IntValue OVERLAY_X = BUILDER
                .comment("Overlay X offset (screen position)")
                .defineInRange("overlay_x", 5, 0, Integer.MAX_VALUE);

        public static final ForgeConfigSpec.IntValue OVERLAY_Y = BUILDER
                .comment("Overlay Y offset (screen position)")
                .defineInRange("overlay_y", 5, 0, Integer.MAX_VALUE);

        public static final ForgeConfigSpec.IntValue OVERLAY_COLOUR = BUILDER
                .comment("Overlay text colour in hex (e.g. 0xE0E0E0)")
                .defineInRange("overlay_colour", 0xE0E0E0, 0, 0xFFFFFF);

        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIM_OFFSETS = BUILDER
                .comment("Dimension Y-offsets (format: dimension_id=offset)")
                .defineList("dimension_offsets",
                        List.of(
                                "minecraft:overworld=0",
                                "aether:the_aether=-320",
                                "minecraft:the_nether=512",
                                "theabyss:the_abyss=1024"
                        ),
                        obj -> obj instanceof String
                );

        public static final ForgeConfigSpec SPEC = BUILDER.build();
    }

    /** Config GUI */
    public static class ConfigScreen extends Screen {
        private final Screen parent;
        private EditBoxWithLabel xBox, yBox, colourBox;

        public ConfigScreen(Screen parent) {
            super(Component.literal("DimThing Config"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            super.init();
            int y = this.height / 2 - 50;

            xBox = makeBox("X Offset: ", Config.OVERLAY_X.get(), y);
            addRenderableWidget(xBox);
            y += 30;

            yBox = makeBox("Y Offset: ", Config.OVERLAY_Y.get(), y);
            addRenderableWidget(yBox);
            y += 30;

            colourBox = makeBox("Colour (hex): ", Config.OVERLAY_COLOUR.get(), y);
            addRenderableWidget(colourBox);
            y += 40;

            addRenderableWidget(new Button.Builder(CommonComponents.GUI_DONE, b -> onClose())
                    .pos(this.width / 2 - 100, y).size(200, 20).build());
        }

        private EditBoxWithLabel makeBox(String label, int initialValue, int y) {
            return new EditBoxWithLabel(font, this.width / 2 - 100, y,
                    200, 20, Component.literal(label),
                    String.valueOf(initialValue),
                    str -> {
                        try { Integer.decode(str); return true; }
                        catch (NumberFormatException e) { return false; }
                    });
        }

        @Override
        public void render(GuiGraphics ctx, int mouseX, int mouseY, float ticks) {
            this.renderDirtBackground(ctx);
            ctx.drawCenteredString(this.font, this.title, this.width / 2, 15, overlayColour);
            super.render(ctx, mouseX, mouseY, ticks);
        }

        @Override
        public void onClose() {
            try {
                Config.OVERLAY_X.set(Integer.parseInt(xBox.getValue()));
                Config.OVERLAY_Y.set(Integer.parseInt(yBox.getValue()));
                Config.OVERLAY_COLOUR.set(Integer.decode(colourBox.getValue()));
            } catch (Exception e) {
                LOGGER.error("Failed to save config", e);
            }
            DimThing.reloadConfig();

            if (minecraft != null && parent != null) minecraft.setScreen(parent);
            else super.onClose();
        }
    }

    /** Custom edit box with label */
    public static class EditBoxWithLabel extends EditBox {
        private final Component label;
        private final Font font;
        private final Function<String, Boolean> verify;

        public EditBoxWithLabel(Font font, int x, int y, int width, int height,
                                Component label, String value, Function<String, Boolean> verify) {
            super(font, x, y, width, height, label);
            this.label = label;
            this.font = font;
            setValue(value);
            this.verify = verify;
        }

        @Override
        public void render(GuiGraphics ctx, int mouseX, int mouseY, float ticks) {
            int labelWidth = font.width(label.getString());
            ctx.drawString(font, label.getString(), this.getX(), this.getY() + height / 2 - font.lineHeight / 2, overlayColour);
            setX(getX() + labelWidth + 5);
            super.render(ctx, mouseX, mouseY, ticks);
            setX(getX() - labelWidth - 5);
        }

        @Override
        public void insertText(String text) {
            if (verify.apply(text)) super.insertText(text);
        }
    }
}
