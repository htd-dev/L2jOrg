package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;

public class ExAutoSoulShot extends ServerPacket {
    private final int _itemId;
    private final boolean _enable;
    private final int _type;

    public ExAutoSoulShot(int itemId, boolean enable, int type) {
        _itemId = itemId;
        _enable = enable;
        _type = type;
    }

    @Override
    public void writeImpl(GameClient client) {
        writeId(ServerExPacketId.EX_AUTO_SOULSHOT);

        writeInt(_itemId);
        writeInt(_enable);
        writeInt(_type);
    }

}
