package com.fpickup;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ServerHandler {
    /** true only while WE are picking up an item for a player (server thread only). */
    private static boolean allowPickup = false;

    /** Blocks the vanilla "walk over item" pickup. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityItemPickupEvent event) {
        if (FPickupConfig.disableAutoPickup && !allowPickup) {
            event.setCanceled(true);
        }
    }

    static void pickup(EntityPlayerMP player, int entityId) {
        if (player == null || !player.isEntityAlive() || player.isSpectator()) return;

        Entity entity = player.world.getEntityByID(entityId);
        if (!(entity instanceof EntityItem) || entity.isDead) return;
        EntityItem item = (EntityItem) entity;

        // anti-cheat: the item must be close to the player
        double max = FPickupConfig.reach + 2.0;
        if (player.getDistanceSq(item) > max * max) return;

        ItemStack stack = item.getItem();
        if (stack.isEmpty()) return;
        int before = stack.getCount();

        allowPickup = true;
        try {
            item.setNoPickupDelay();
            // vanilla pickup: adds to inventory, plays sound + animation, fires Forge events
            item.onCollideWithPlayer(player);
        } finally {
            allowPickup = false;
        }

        if (!item.isDead && item.getItem().getCount() >= before) {
            player.sendStatusMessage(new TextComponentTranslation("fpickup.inventory_full"), true);
        }
    }
}
