package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to remove parts from an entity.  This packet only appears on clients.
 *
 * @author don_bruce
 */
public class PacketPartChange_Remove extends APacketEntity<AEntityF_Multipart<?>> {
    private final UUID partUUID;

    public PacketPartChange_Remove(APart partRemoved) {
        super(partRemoved.entityOn);
        this.partUUID = partRemoved.uniqueUUID;
    }

    public PacketPartChange_Remove(ByteBuf buf) {
        super(buf);
        this.partUUID = readUUIDFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(partUUID, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityF_Multipart<?> entity) {
        APart part = world.getEntity(partUUID);
        if (part != null) {
            part.remove();
        }
        return false;
    }
}
