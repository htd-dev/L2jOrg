package org.l2j.gameserver.network.l2.c2s;

import org.l2j.commons.lang.ArrayUtils;
import org.l2j.commons.math.SafeMath;
import org.l2j.gameserver.Config;
import org.l2j.gameserver.Contants;
import org.l2j.gameserver.Contants.Items;
import org.l2j.gameserver.model.Player;
import org.l2j.gameserver.model.instances.NpcInstance;
import org.l2j.gameserver.model.items.ItemInstance;
import org.l2j.gameserver.model.items.PcInventory;
import org.l2j.gameserver.model.items.Warehouse;
import org.l2j.gameserver.model.items.Warehouse.WarehouseType;
import org.l2j.gameserver.network.l2.components.SystemMsg;
import org.l2j.gameserver.templates.item.ItemTemplate;
import org.l2j.gameserver.utils.Log;

/**
 * Format: cdb, b - array of (dd)
 */
public class SendWareHouseDepositList extends L2GameClientPacket
{
	private static final long _WAREHOUSE_FEE = 30; //TODO [G1ta0] hardcode price

	private int _count;
	private int[] _items;
	private long[] _itemQ;

	@Override
	protected void readImpl()
	{
		_count = readInt();
		if(_count * 12 > availableData() || _count > Short.MAX_VALUE || _count < 1)
		{
			_count = 0;
			return;
		}

		_items = new int[_count];
		_itemQ = new long[_count];

		for(int i = 0; i < _count; i++)
		{
			_items[i] = readInt();
			_itemQ[i] = readLong();
			if(_itemQ[i] < 1 || ArrayUtils.indexOf(_items, _items[i]) < i)
			{
				_count = 0;
				return;
			}
		}
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if(activeChar == null || _count == 0)
			return;

		if(!activeChar.getPlayerAccess().UseWarehouse)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isActionsDisabled())
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isInStoreMode())
		{
			activeChar.sendPacket(SystemMsg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}

		if(activeChar.isInTrade())
		{
			activeChar.sendActionFailed();
			return;
		}

		// Проверяем наличие npc и расстояние до него
		NpcInstance whkeeper = activeChar.getLastNpc();
		if(!Config.BBS_WAREHOUSE_ENABLED && (whkeeper == null || !activeChar.checkInteractionDistance(whkeeper)))
		{
			activeChar.sendPacket(SystemMsg.YOU_HAVE_MOVED_TOO_FAR_AWAY_FROM_THE_WAREHOUSE_TO_PERFORM_THAT_ACTION);
			return;
		}

		PcInventory inventory = activeChar.getInventory();
		boolean privatewh = activeChar.getUsingWarehouseType() != WarehouseType.CLAN;
		Warehouse warehouse;
		if(privatewh)
			warehouse = activeChar.getWarehouse();
		else
			warehouse = activeChar.getClan().getWarehouse();

		inventory.writeLock();
		warehouse.writeLock();
		try
		{
			int slotsleft = 0;
			long adenaDeposit = 0;

			if(privatewh)
				slotsleft = activeChar.getWarehouseLimit() - warehouse.getSize();
			else
				slotsleft = activeChar.getClan().getWhBonus() + Config.WAREHOUSE_SLOTS_CLAN - warehouse.getSize();

			int items = 0;

			// Создаем новый список передаваемых предметов, на основе полученных данных
			for(int i = 0; i < _count; i++)
			{
				ItemInstance item = inventory.getItemByObjectId(_items[i]);
				if(item == null || item.getCount() < _itemQ[i] || !item.canBeStored(activeChar, privatewh))
				{
					_items[i] = 0; // Обнуляем, вещь не будет передана
					_itemQ[i] = 0L;
					continue;
				}

				if(!item.isStackable() || warehouse.getItemByItemId(item.getItemId()) == null) // вещь требует слота
				{
					if(slotsleft <= 0) // если слоты кончились нестекуемые вещи и отсутствующие стекуемые пропускаем
					{
						_items[i] = 0; // Обнуляем, вещь не будет передана
						_itemQ[i] = 0L;
						continue;
					}
					slotsleft--; // если слот есть то его уже нет
				}

				if(item.getItemId() == Items.ADENA)
					adenaDeposit = _itemQ[i];

				items++;
			}

			// Сообщаем о том, что слоты кончились
			if(slotsleft <= 0)
				activeChar.sendPacket(SystemMsg.YOUR_WAREHOUSE_IS_FULL);

			if(items == 0)
			{
				activeChar.sendPacket(SystemMsg.INCORRECT_ITEM_COUNT);
				return;
			}

			// Проверяем, хватит ли у нас денег на уплату налога
			long fee = SafeMath.mulAndCheck(items, _WAREHOUSE_FEE);

			if(fee + adenaDeposit > activeChar.getAdena())
			{
				activeChar.sendPacket(SystemMsg.YOU_LACK_THE_FUNDS_NEEDED_TO_PAY_FOR_THIS_TRANSACTION);
				return;
			}

			if(!activeChar.reduceAdena(fee, true))
			{
				activeChar.sendPacket(SystemMsg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				return;
			}

			for(int i = 0; i < _count; i++)
			{
				if(_items[i] == 0)
					continue;
				ItemInstance item = inventory.removeItemByObjectId(_items[i], _itemQ[i]);
				Log.LogItem(activeChar, privatewh ? Log.WarehouseDeposit : Log.ClanWarehouseDeposit, item);
				warehouse.addItem(item);
			}
		}
		catch(ArithmeticException ae)
		{
			//TODO audit
			activeChar.sendPacket(SystemMsg.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED);
			return;
		}
		finally
		{
			warehouse.writeUnlock();
			inventory.writeUnlock();
		}

		// Обновляем параметры персонажа
		activeChar.sendChanges();
		activeChar.sendPacket(SystemMsg.THE_TRANSACTION_IS_COMPLETE);
	}
}