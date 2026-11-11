package com.fpickup;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = FPickup.MODID, name = FPickup.MODID)
public class FPickupConfig {

    @Config.Comment("true = items are NOT picked up by walking over them, only with the pickup key (Rust style)")
    public static boolean disableAutoPickup = true;

    @Config.Comment("Max distance (blocks) to pick up an item with the key")
    @Config.RangeDouble(min = 1.0, max = 8.0)
    public static double reach = 4.0;

    @Config.Comment("Draw white outline around the item under the crosshair")
    public static boolean showOutline = true;

    @Config.Comment("Outline line width")
    @Config.RangeDouble(min = 1.0, max = 10.0)
    public static double outlineWidth = 3.0;

    @Config.Comment("Show '[F] Pick up <item>' text under the crosshair")
    public static boolean showPrompt = true;

    @Config.Comment("Vertical offset of the prompt from the screen center (pixels, GUI scale)")
    @Config.RangeInt(min = -200, max = 200)
    public static int promptOffsetY = 16;

    @Mod.EventBusSubscriber(modid = FPickup.MODID)
    public static class Sync {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (FPickup.MODID.equals(event.getModID())) {
                ConfigManager.sync(FPickup.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
