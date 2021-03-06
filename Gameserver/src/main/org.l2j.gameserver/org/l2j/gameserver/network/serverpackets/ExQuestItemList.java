package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.model.item.instance.Item;
import org.l2j.gameserver.network.GameClient;
import org.l2j.gameserver.network.ServerExPacketId;

import java.util.Collection;

/**
 * @author JIV
 */
public class ExQuestItemList extends AbstractItemPacket {
    private final int _sendType;
    private final Player _activeChar;
    private final Collection<Item> _items;

    public ExQuestItemList(int sendType, Player activeChar) {
        _sendType = sendType;
        _activeChar = activeChar;
        _items = activeChar.getInventory().getItems(Item::isQuestItem);
    }

    @Override
    public void writeImpl(GameClient client) {
        writeId(ServerExPacketId.EX_QUEST_ITEMLIST);
        writeByte((byte) _sendType);
        if (_sendType == 2) {
            writeInt(_items.size());
        } else {
            writeShort((short) 0);
        }
        writeInt(_items.size());
        for (Item item : _items) {
            writeItem(item);
        }
        writeInventoryBlock(_activeChar.getInventory());
    }
}
