package org.l2j.gameserver.network.serverpackets.luckygame;

import org.l2j.gameserver.enums.LuckyGameType;
import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;
import org.l2j.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Sdw
 */
public class ExStartLuckyGame extends ServerPacket {
    private final LuckyGameType _type;
    private final int _ticketCount;

    public ExStartLuckyGame(LuckyGameType type, long ticketCount) {
        _type = type;
        _ticketCount = (int) ticketCount;
    }

    @Override
    public void writeImpl(GameClient client) {
        writeId(ServerExPacketId.EX_START_LUCKY_GAME);
        writeInt(_type.ordinal());
        writeInt(_ticketCount);
    }

}
