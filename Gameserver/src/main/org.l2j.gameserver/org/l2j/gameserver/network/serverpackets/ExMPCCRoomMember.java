package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.enums.MatchingMemberType;
import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.model.matching.CommandChannelMatchingRoom;
import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;
import org.l2j.gameserver.world.MapRegionManager;

/**
 * @author Sdw
 */
public class ExMPCCRoomMember extends ServerPacket {
    private final CommandChannelMatchingRoom _room;
    private final MatchingMemberType _type;

    public ExMPCCRoomMember(Player player, CommandChannelMatchingRoom room) {
        _room = room;
        _type = room.getMemberType(player);
    }

    @Override
    public void writeImpl(GameClient client) {
        writeId(ServerExPacketId.EX_MPCC_ROOM_MEMBER);

        writeInt(_type.ordinal());
        writeInt(_room.getMembersCount());
        for (Player member : _room.getMembers()) {
            writeInt(member.getObjectId());
            writeString(member.getName());
            writeInt(member.getLevel());
            writeInt(member.getClassId().getId());
            writeInt(MapRegionManager.getInstance().getBBs(member.getLocation()));
            writeInt(_room.getMemberType(member).ordinal());
        }
    }

}
