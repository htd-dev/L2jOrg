package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;

/**
 * Opens the CommandChannel Information window
 *
 * @author chris_00
 */
public class ExOpenMPCC extends ServerPacket {
    public static final ExOpenMPCC STATIC_PACKET = new ExOpenMPCC();

    private ExOpenMPCC() {
    }

    @Override
    public void writeImpl(GameClient client) {
        writeId(ServerExPacketId.EX_OPEN_MPCC);
    }

}
