package com.fpickup;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = FPickup.MODID, name = FPickup.NAME, version = FPickup.VERSION,
        acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "*")
public class FPickup {
    public static final String MODID = "fpickup";
    public static final String NAME = "F Pickup";
    public static final String VERSION = "3.0";

    public static SimpleNetworkWrapper NET;

    @SidedProxy(clientSide = "com.fpickup.ClientProxy", serverSide = "com.fpickup.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NET = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        NET.registerMessage(MsgPickup.Handler.class, MsgPickup.class, 0, Side.SERVER);
        MinecraftForge.EVENT_BUS.register(new ServerHandler());
        proxy.preInit(event);
    }
}
