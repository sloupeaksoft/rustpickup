package com.fpickup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientHandler {
    /** Rebindable in Options -> Controls -> "F Pickup". */
    public static KeyBinding KEY;

    /** Item under the crosshair (updated every client tick). */
    private static EntityItem target;

    public static void init() {
        KEY = new KeyBinding("key.fpickup.pickup", KeyConflictContext.IN_GAME, Keyboard.KEY_F, "key.categories.fpickup");
        ClientRegistry.registerKeyBinding(KEY);
        MinecraftForge.EVENT_BUS.register(new ClientHandler());
    }

    // ---------------------------------------------------------------- input

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        handleKey();
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        handleKey();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            target = null;
            return;
        }
        target = findTarget(mc, 1.0F);
        handleKey();
    }

    private static void handleKey() {
        if (KEY == null) return;
        boolean pressed = false;
        while (KEY.isPressed()) pressed = true;
        if (!pressed) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        EntityItem item = findTarget(mc, 1.0F);
        if (item == null) return;

        FPickup.NET.sendToServer(new MsgPickup(item.getEntityId()));

        // F is also vanilla "swap item with offhand": don't swap when we picked something up
        KeyBinding swap = mc.gameSettings.keyBindSwapHands;
        if (swap != null && swap.getKeyCode() == KEY.getKeyCode()) {
            while (swap.isPressed()) {
                // consume
            }
        }
    }

    // ------------------------------------------------------------ targeting

    private static EntityItem findTarget(Minecraft mc, float partialTicks) {
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null) return null;

        double reach = FPickupConfig.reach;
        Vec3d eyes = player.getPositionEyes(partialTicks);
        Vec3d look = player.getLook(partialTicks);
        Vec3d end = eyes.add(look.scale(reach));

        // items behind a wall must not be selectable
        double maxDist = reach;
        RayTraceResult block = mc.world.rayTraceBlocks(eyes, end, false, true, false);
        if (block != null && block.typeOfHit == RayTraceResult.Type.BLOCK && block.hitVec != null) {
            maxDist = Math.min(reach, eyes.distanceTo(block.hitVec) + 0.5);
        }

        AxisAlignedBB area = player.getEntityBoundingBox()
                .expand(look.x * reach, look.y * reach, look.z * reach)
                .grow(1.0);

        EntityItem best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityItem item : mc.world.getEntitiesWithinAABB(EntityItem.class, area)) {
            if (item.isDead || item.getItem().isEmpty()) continue;

            // the item model bobs above its tiny hitbox, so make the box a bit bigger
            AxisAlignedBB bb = item.getEntityBoundingBox().grow(0.2);
            double dist;
            if (isInside(bb, eyes)) {
                dist = 0.0;
            } else {
                RayTraceResult hit = bb.calculateIntercept(eyes, end);
                if (hit == null || hit.hitVec == null) continue;
                dist = eyes.distanceTo(hit.hitVec);
            }
            if (dist <= maxDist && dist < bestDist) {
                best = item;
                bestDist = dist;
            }
        }
        return best;
    }

    private static boolean isInside(AxisAlignedBB bb, Vec3d v) {
        return v.x > bb.minX && v.x < bb.maxX
                && v.y > bb.minY && v.y < bb.maxY
                && v.z > bb.minZ && v.z < bb.maxZ;
    }

    // ------------------------------------------------------- white outline

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        EntityItem t = target;
        if (!FPickupConfig.showOutline || t == null || t.isDead) return;

        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        float pt = event.getPartialTicks();
        double vx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
        double vy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
        double vz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;

        double tx = t.lastTickPosX + (t.posX - t.lastTickPosX) * pt;
        double ty = t.lastTickPosY + (t.posY - t.lastTickPosY) * pt;
        double tz = t.lastTickPosZ + (t.posZ - t.lastTickPosZ) * pt;

        AxisAlignedBB bb = t.getEntityBoundingBox()
                .offset(tx - t.posX - vx, ty - t.posY - vy, tz - t.posZ - vz)
                .grow(0.08);

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth((float) FPickupConfig.outlineWidth);

        RenderGlobal.drawSelectionBoundingBox(bb, 1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    // -------------------------------------------- "[F] Pick up <item>" text

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        EntityItem t = target;
        if (!FPickupConfig.showPrompt || t == null || t.isDead) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.gameSettings.hideGUI) return;

        ItemStack stack = t.getItem();
        if (stack.isEmpty()) return;

        String name = stack.getDisplayName();
        if (stack.getCount() > 1) name = name + " x" + stack.getCount();
        String text = TextFormatting.YELLOW + "[" + KEY.getDisplayName() + "] "
                + TextFormatting.WHITE + I18n.format("fpickup.prompt") + " " + name;

        FontRenderer fr = mc.fontRenderer;
        ScaledResolution res = event.getResolution();
        int w = fr.getStringWidth(text);
        int x = (res.getScaledWidth() - w) / 2;
        int y = res.getScaledHeight() / 2 + FPickupConfig.promptOffsetY;

        Gui.drawRect(x - 4, y - 3, x + w + 4, y + fr.FONT_HEIGHT + 2, 0x90000000);
        fr.drawStringWithShadow(text, x, y, 0xFFFFFF);
    }
}
