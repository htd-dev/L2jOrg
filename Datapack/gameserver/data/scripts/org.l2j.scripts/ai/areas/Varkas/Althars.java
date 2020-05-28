package ai.areas.Varkas;

import ai.AbstractNpcAI;
import org.l2j.commons.util.Rnd;
import org.l2j.gameserver.Config;
import org.l2j.gameserver.data.xml.impl.SpawnsData;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.actor.Npc;
import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.model.skills.AbnormalVisualEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Althars extends AbstractNpcAI {
    private static Logger LOGGER = LoggerFactory.getLogger(Althars.class);

    private final int _DELAY = 30000; // Loop time for checking Althars state
    private final int altharsID = 18926;

    private boolean[] althars_state = new boolean[12];
    private Map<String, Npc> althars = new HashMap<String, Npc>();

    // TODO: set heading of Althars
    private int[] ALTHARS_1_LOCATION = new int[] {124809, -43595, 3221, 0};
    private int[] ALTHARS_2_LOCATION = new int[] {119008, -42127, -3213, 0};
    private int[] ALTHARS_3_LOCATION = new int[] {123355, -47015, -2867, 11547};
    private int[] ALTHARS_4_LOCATION = new int[] {119906, -45557, -2805, 0};
    private int[] ALTHARS_5_LOCATION = new int[] {124543, -52061, -2456, 0};
    private int[] ALTHARS_6_LOCATION = new int[] {121611, -57169, -2174, 0};
    private int[] ALTHARS_7_LOCATION = new int[] {114120, -46272, -2582, 0};
    private int[] ALTHARS_8_LOCATION = new int[] {106936, -47288, -1888, 0};
    private int[] ALTHARS_9_LOCATION = new int[] {112536, -55496, -2832, 0};
    private int[] ALTHARS_10_LOCATION = new int[] {115256, -39144, -2488, 0};
    private int[] ALTHARS_11_LOCATION = new int[] {105736, -41768, -1776, 0};
    private int[] ALTHARS_12_LOCATION = new int[] {109176, -36024, -896, 0};


    private Althars() {
        for(int i = 0; i < althars_state.length; i++) {
            althars_state[i] = false;
        }

        althars.put("althar_1", addSpawn(altharsID, ALTHARS_1_LOCATION[0], ALTHARS_1_LOCATION[1], ALTHARS_1_LOCATION[2], ALTHARS_1_LOCATION[3], false, 0, false, 0));
        althars.put("althar_2", addSpawn(altharsID, ALTHARS_2_LOCATION[0], ALTHARS_2_LOCATION[1], ALTHARS_2_LOCATION[2], ALTHARS_2_LOCATION[3], false, 0, false, 0));
        althars.put("althar_3", addSpawn(altharsID, ALTHARS_3_LOCATION[0], ALTHARS_3_LOCATION[1], ALTHARS_3_LOCATION[2], ALTHARS_3_LOCATION[3], false, 0, false, 0));
        althars.put("althar_4", addSpawn(altharsID, ALTHARS_4_LOCATION[0], ALTHARS_4_LOCATION[1], ALTHARS_4_LOCATION[2], ALTHARS_4_LOCATION[3], false, 0, false, 0));
        althars.put("althar_5", addSpawn(altharsID, ALTHARS_5_LOCATION[0], ALTHARS_5_LOCATION[1], ALTHARS_5_LOCATION[2], ALTHARS_5_LOCATION[3], false, 0, false, 0));
        althars.put("althar_6", addSpawn(altharsID, ALTHARS_6_LOCATION[0], ALTHARS_6_LOCATION[1], ALTHARS_6_LOCATION[2], ALTHARS_6_LOCATION[3], false, 0, false, 0));
        althars.put("althar_7", addSpawn(altharsID, ALTHARS_7_LOCATION[0], ALTHARS_7_LOCATION[1], ALTHARS_7_LOCATION[2], ALTHARS_7_LOCATION[3], false, 0, false, 0));
        althars.put("althar_8", addSpawn(altharsID, ALTHARS_8_LOCATION[0], ALTHARS_8_LOCATION[1], ALTHARS_8_LOCATION[2], ALTHARS_8_LOCATION[3], false, 0, false, 0));
        althars.put("althar_9", addSpawn(altharsID, ALTHARS_9_LOCATION[0], ALTHARS_9_LOCATION[1], ALTHARS_9_LOCATION[2], ALTHARS_9_LOCATION[3], false, 0, false, 0));
        althars.put("althar_10", addSpawn(altharsID, ALTHARS_10_LOCATION[0], ALTHARS_10_LOCATION[1], ALTHARS_10_LOCATION[2], ALTHARS_10_LOCATION[3], false, 0, false, 0));
        althars.put("althar_11", addSpawn(altharsID, ALTHARS_11_LOCATION[0], ALTHARS_11_LOCATION[1], ALTHARS_11_LOCATION[2], ALTHARS_11_LOCATION[3], false, 0, false, 0));
        althars.put("althar_12", addSpawn(altharsID, ALTHARS_12_LOCATION[0], ALTHARS_12_LOCATION[1], ALTHARS_12_LOCATION[2], ALTHARS_12_LOCATION[3], false, 0, false, 0));

        startQuestTimer("ALTHARS_TIMER", _DELAY, null,null);
    }

    @Override
    public String onAdvEvent(String event, Npc npc, Player player) {
        LOGGER.info("running task {}", event);
        if("ALTHARS_TIMER".equals(event)) {
            for(int i = 0; i < Config.ALTHARS_MAX_ACTIVE - getAltharsActiveCount() ; i++) {
                if (Rnd.get(100) < Config.ALTHARS_ACTIVATE_CHANCE_RATE) {
                    int altharsRndIndex = Rnd.get(12 - getAltharsActiveCount());
                    LOGGER.info("Picking althars virtual index {}", altharsRndIndex);
                    int altharsIndex = getAltharsIndex(altharsRndIndex);
                    spawnMonsters(altharsIndex);
                    int AltharsDurationCycle = Rnd.get(Config.ALTHARS_MIN_DURATION_CYCLE, Config.ALTHARS_MAX_DURATION_CYCLE);
                    LOGGER.info("starting althars_" + (altharsIndex + 1) + " for {} sec", AltharsDurationCycle);
                    startQuestTimer("STOP_ALTHARS_" + (altharsIndex + 1), AltharsDurationCycle, null,null);
                }
            }
            startQuestTimer("ALTHARS_TIMER", _DELAY, null,null);
        } else if(event.startsWith("STOP_ALTHARS_")) {
            unSpawnMonsters(Integer.parseInt(event.substring(event.length() - 1)) - 1);
        }

        return super.onAdvEvent(event, npc, player);
    }

    private int getAltharsActiveCount() {
        int count = 0;
        for(int i = 0; i < althars_state.length; i++) {
            if (althars_state[i] == true) {
                count++;
            }
        }
        return count;
    }

    private int getAltharsIndex(int rndIndex) {
        int virtualIndex = -1;
        for(int realIndex = 0; realIndex < althars_state.length; realIndex++) {
            if (althars_state[realIndex] == true) {
                continue;
            } else {
                virtualIndex++;
            }

            if (rndIndex == virtualIndex) {
                return realIndex;
            }
        }
        LOGGER.error("No Althars index found. " + Arrays.toString(althars_state));
        return -1;
    }

    private void spawnMonsters(int altharIndex) {
        LOGGER.info("spawning mobs for althars_{}", (altharIndex + 1));
        althars.get("althar_" + (altharIndex + 1)).getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
        SpawnsData.getInstance().spawnByName("althar_" + (altharIndex + 1));
        althars_state[altharIndex] = true;
    }

    private void unSpawnMonsters(int altharIndex) {
        LOGGER.info("unspawning mobs for althars_{}", (altharIndex + 1));
        althars.get("althar_" + (altharIndex + 1)).getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
        SpawnsData.getInstance().deSpawnByName("althar_" + (altharIndex + 1));
        althars_state[altharIndex] = false;
    }

    public static AbstractNpcAI provider() {
        return new Althars();
    }
}