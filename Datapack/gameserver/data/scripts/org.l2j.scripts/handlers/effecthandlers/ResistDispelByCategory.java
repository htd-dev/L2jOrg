package handlers.effecthandlers;

import org.l2j.gameserver.enums.DispelSlotType;
import org.l2j.gameserver.model.StatsSet;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.effects.AbstractEffect;
import org.l2j.gameserver.engine.skill.api.Skill;
import org.l2j.gameserver.model.stats.Stat;

/**
 * @author Sdw
 */
public class ResistDispelByCategory extends AbstractEffect {
    public final DispelSlotType slot;
    public final double power;

    public ResistDispelByCategory(StatsSet params) {
        power = params.getDouble("power", 0);
        slot = params.getEnum("category", DispelSlotType.class, DispelSlotType.BUFF);
    }

    @Override
    public void pump(Creature effected, Skill skill) {
        // Only this one is in use it seems
        if (slot == DispelSlotType.BUFF) {
            effected.getStats().mergeMul(Stat.RESIST_DISPEL_BUFF, 1 + (power / 100));
        }
    }
}
