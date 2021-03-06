package org.l2j.gameserver.network.clientpackets;

import org.l2j.gameserver.model.actor.instance.Player;

/**
 * @author ShanSoft Packets Structure: chdd
 */
public final class RequestTeleportBookMark extends ClientPacket {
    private int _id;

    @Override
    public void readImpl() {
        _id = readInt();
    }

    @Override
    public void runImpl() {
        final Player activeChar = client.getPlayer();
        if (activeChar == null) {
            return;
        }
        activeChar.teleportBookmarkGo(_id);
    }
}
