package org.l2j.gameserver.model;

import org.l2j.commons.database.DatabaseFactory;
import org.l2j.gameserver.enums.ShortcutType;
import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.model.interfaces.IRestorable;
import org.l2j.gameserver.model.items.instance.Item;
import org.l2j.gameserver.network.serverpackets.ShortCutRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;


public class ShortCuts implements IRestorable {
    private static final int MAX_SHORTCUTS_PER_BAR = 12;
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortCuts.class);
    private final Player _owner;
    private final Map<Integer, Shortcut> shortCuts = new TreeMap<>();

    public ShortCuts(Player owner) {
        _owner = owner;
    }

    public Shortcut[] getAllShortCuts() {
        return shortCuts.values().toArray(new Shortcut[0]);
    }

    public Shortcut getShortCut(int slot, int page) {
        Shortcut sc = shortCuts.get(slot + (page * MAX_SHORTCUTS_PER_BAR));
        // Verify shortcut
        if ((sc != null) && (sc.getType() == ShortcutType.ITEM) && (_owner.getInventory().getItemByObjectId(sc.getId()) == null)) {
            deleteShortCut(sc.getSlot(), sc.getPage());
            sc = null;
        }
        return sc;
    }

    public synchronized void registerShortCut(Shortcut shortcut) {
        // Verify shortcut
        if (shortcut.getType() == ShortcutType.ITEM) {
            final Item item = _owner.getInventory().getItemByObjectId(shortcut.getId());
            if (item == null) {
                return;
            }
            shortcut.setSharedReuseGroup(item.getSharedReuseGroup());
        }
        registerShortCutInDb(shortcut, shortCuts.put(shortcut.getSlot() + (shortcut.getPage() * MAX_SHORTCUTS_PER_BAR), shortcut));
    }

    private void registerShortCutInDb(Shortcut shortcut, Shortcut oldShortCut) {
        if (oldShortCut != null) {
            deleteShortCutFromDb(oldShortCut);
        }

        try (Connection con = DatabaseFactory.getInstance().getConnection();
             PreparedStatement statement = con.prepareStatement("REPLACE INTO character_shortcuts (charId,slot,page,type,shortcut_id,level,sub_level,class_index) values(?,?,?,?,?,?,?,?)")) {
            statement.setInt(1, _owner.getObjectId());
            statement.setInt(2, shortcut.getSlot());
            statement.setInt(3, shortcut.getPage());
            statement.setInt(4, shortcut.getType().ordinal());
            statement.setInt(5, shortcut.getId());
            statement.setInt(6, shortcut.getLevel());
            statement.setInt(7, shortcut.getSubLevel());
            statement.setInt(8, _owner.getClassIndex());
            statement.execute();
        } catch (Exception e) {
            LOGGER.warn("Could not store character shortcut: " + e.getMessage(), e);
        }
    }

    /**
     * @param slot
     * @param page
     */
    public synchronized void deleteShortCut(int slot, int page) {
        final Shortcut old = shortCuts.remove(slot + (page * MAX_SHORTCUTS_PER_BAR));
        if ((old == null) || (_owner == null)) {
            return;
        }
        deleteShortCutFromDb(old);
    }

    public synchronized void deleteShortCutByObjectId(int objectId) {
        for (Shortcut shortcut : shortCuts.values()) {
            if ((shortcut.getType() == ShortcutType.ITEM) && (shortcut.getId() == objectId)) {
                deleteShortCut(shortcut.getSlot(), shortcut.getPage());
                break;
            }
        }
    }

    /**
     * @param shortcut
     */
    private void deleteShortCutFromDb(Shortcut shortcut) {
        try (Connection con = DatabaseFactory.getInstance().getConnection();
             PreparedStatement statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=? AND slot=? AND page=? AND class_index=?")) {
            statement.setInt(1, _owner.getObjectId());
            statement.setInt(2, shortcut.getSlot());
            statement.setInt(3, shortcut.getPage());
            statement.setInt(4, _owner.getClassIndex());
            statement.execute();
        } catch (Exception e) {
            LOGGER.warn("Could not delete character shortcut: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean restoreMe() {
        shortCuts.clear();
        try (Connection con = DatabaseFactory.getInstance().getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT charId, slot, page, type, shortcut_id, level, sub_level FROM character_shortcuts WHERE charId=? AND class_index=?")) {
            statement.setInt(1, _owner.getObjectId());
            statement.setInt(2, _owner.getClassIndex());

            try (ResultSet rset = statement.executeQuery()) {
                while (rset.next()) {
                    final int slot = rset.getInt("slot");
                    final int page = rset.getInt("page");
                    final int type = rset.getInt("type");
                    final int id = rset.getInt("shortcut_id");
                    final int level = rset.getInt("level");
                    final int subLevel = rset.getInt("sub_level");
                    shortCuts.put(slot + (page * MAX_SHORTCUTS_PER_BAR), new Shortcut(slot, page, ShortcutType.values()[type], id, level, subLevel, 1));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not restore character shortcuts: " + e.getMessage(), e);
            return false;
        }

        // Verify shortcuts
        for (Shortcut sc : getAllShortCuts()) {
            if (sc.getType() == ShortcutType.ITEM) {
                final Item item = _owner.getInventory().getItemByObjectId(sc.getId());
                if (item == null) {
                    deleteShortCut(sc.getSlot(), sc.getPage());
                } else if (item.isEtcItem()) {
                    sc.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup());
                }
            }
        }

        return true;
    }

    /**
     * Updates the shortcut bars with the new skill.
     *
     * @param skillId       the skill Id to search and update.
     * @param skillLevel    the skill level to update.
     * @param skillSubLevel the skill sub level to update.
     */
    public synchronized void updateShortCuts(int skillId, int skillLevel, int skillSubLevel) {
        // Update all the shortcuts for this skill
        for (Shortcut sc : shortCuts.values()) {
            if ((sc.getId() == skillId) && (sc.getType() == ShortcutType.SKILL)) {
                final Shortcut newsc = new Shortcut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), skillLevel, skillSubLevel, 1);
                _owner.sendPacket(new ShortCutRegister(newsc));
                _owner.registerShortCut(newsc);
            }
        }
    }
}
