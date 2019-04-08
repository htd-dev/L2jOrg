package org.l2j.gameserver.data.database.dao;

import io.github.joealisson.primitive.sets.IntSet;
import org.l2j.commons.database.DAO;
import org.l2j.commons.database.annotation.Query;

public interface IdFactoryDAO extends DAO {

    @Query("SELECT charId AS id FROM characters " +
            "UNION SELECT object_id AS id FROM items " +
            "UNION SELECT clan_id AS id FROM clan_data " +
            "UNION SELECT object_id AS id FROM itemsonground " +
            "UNION SELECT messageId AS id FROM messages " +
            "UNION SELECT id FROM couples")
    IntSet findUsedObjectIds();

}