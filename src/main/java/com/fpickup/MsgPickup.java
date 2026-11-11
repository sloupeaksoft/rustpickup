package com.fpickup;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: "I want to pick up the item entity with this id". */
public class MsgPickup implements IMessage {
    private int entityId;

    public MsgPickup() {
    }

    public MsgPickup(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
    }

    public static class Handler implements IMessageHandler<MsgPickup, IMessage> {
        @Override
        public IMessage onMessage(MsgPickup message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            final int id = message.entityId;
            // Network thread -> main server thread
            player.getServerWorld().addScheduledTask(() -> ServerHandler.pickup(player, id));
            return null;
        }
    }
}
