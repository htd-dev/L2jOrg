package org.l2j.gameserver.model.instances;

import org.l2j.commons.collections.MultiValueSet;
import org.l2j.commons.util.Rnd;

import org.l2j.gameserver.Config;
import org.l2j.gameserver.ai.CtrlEvent;
import org.l2j.gameserver.data.QuestHolder;
import org.l2j.gameserver.data.xml.holder.NpcHolder;
import org.l2j.gameserver.data.xml.holder.SkillHolder;
import org.l2j.gameserver.idfactory.IdFactory;
import org.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import org.l2j.gameserver.model.*;
import org.l2j.gameserver.model.entity.Hero;
import org.l2j.gameserver.model.entity.HeroDiary;
import org.l2j.gameserver.model.quest.Quest;
import org.l2j.gameserver.model.quest.QuestState;
import org.l2j.gameserver.network.l2.components.SystemMsg;
import org.l2j.gameserver.settings.ServerSettings;
import org.l2j.gameserver.templates.StatsSet;
import org.l2j.gameserver.templates.npc.NpcTemplate;
import org.l2j.gameserver.utils.ItemFunctions;
import org.l2j.gameserver.utils.NpcUtils;

import static org.l2j.commons.configuration.Configurator.getSettings;

public class RaidBossInstance extends MonsterInstance
{
	private static final long serialVersionUID = 1L;
	private final boolean _spawnDeathKnight;

	public RaidBossInstance(int objectId, NpcTemplate template, MultiValueSet<String> set)
	{
		super(objectId, template, set);
		_spawnDeathKnight = getParameter("spawn_death_knight", true);
	}

	@Override
	public boolean isRaid()
	{
		return true;
	}

	@Override
	public double getRewardRate(Player player) {
		return getSettings(ServerSettings.class).rateDropItemsRaidboss();
	}

	@Override
	public double getDropChanceMod(Player player)
	{
		return getSettings(ServerSettings.class).dropChanceRaidbossModifier();
	}

	@Override
	protected void onDeath(Creature killer)
	{
		if(this instanceof ReflectionBossInstance)
		{
			super.onDeath(killer);
			return;
		}
		if(killer != null && killer.isPlayable())
		{
			Player player = killer.getPlayer();
			if(player.isInParty())
			{
				for(Player member : player.getParty().getPartyMembers())
					if(member.isHero())
						Hero.getInstance().addHeroDiary(member.getObjectId(), HeroDiary.ACTION_RAID_KILLED, getNpcId());
				player.getParty().broadCast(SystemMsg.CONGRATULATIONS_YOUR_RAID_WAS_SUCCESSFUL);
			}
			else
			{
				if(player.isHero())
					Hero.getInstance().addHeroDiary(player.getObjectId(), HeroDiary.ACTION_RAID_KILLED, getNpcId());
				player.sendPacket(SystemMsg.CONGRATULATIONS_YOUR_RAID_WAS_SUCCESSFUL);
			}

			Quest q = QuestHolder.getInstance().getQuest(508);
			if(q != null)
			{
				if(player.getClan() != null && player.getClan().getLeader().isOnline())
				{
					QuestState st = player.getClan().getLeader().getPlayer().getQuestState(q);
					if(st != null)
						st.getQuest().onKill(this, st);
				}
			}
		}

		int boxId = 0;
		switch(getNpcId())
		{
			case 25035: // Shilens Messenger Cabrio
				boxId = 31027;
				break;
			case 25054: // Demon Kernon
				boxId = 31028;
				break;
			case 25126: // Golkonda, the Longhorn General
				boxId = 31029;
				break;
			case 25220: // Death Lord Hallate
				boxId = 31030;
				break;
		}

		if(boxId != 0)
		{
			NpcTemplate boxTemplate = NpcHolder.getInstance().getTemplate(boxId);
			if(boxTemplate != null)
			{
				final NpcInstance box = new NpcInstance(IdFactory.getInstance().getNextId(), boxTemplate, StatsSet.EMPTY);
				box.spawnMe(getLoc());
				box.setSpawnedLoc(getLoc());

				box.startDeleteTask(60000);
			}
		}
		
		if(killer != null && killer.getPlayer() != null && Config.RAID_DROP_GLOBAL_ITEMS && getLevel() >= Config.MIN_RAID_LEVEL_TO_DROP)
		{
			for(Config.RaidGlobalDrop drop_inf : Config.RAID_GLOBAL_DROP)
			{
				int id = drop_inf.getId();
				long count = drop_inf.getCount();
				double chance = drop_inf.getChance();
				if(Rnd.chance(chance))
					ItemFunctions.addItem(killer.getPlayer(), id, count, true);
			}
		}
		if(_spawnDeathKnight && !isBoss() && Rnd.chance(10))
		{
			int knightId = 0;
			if(getLevel() >= 20 && getLevel() < 30)
				knightId = 25787;
			else if(getLevel() >= 30 && getLevel() < 40)
				knightId = 25788;
			else if(getLevel() >= 40 && getLevel() < 50)
				knightId = 25789;
			else if(getLevel() >= 50 && getLevel() < 60)
				knightId = 25790;
			else if(getLevel() >= 60 && getLevel() < 70)
				knightId = 25791;
			else if(getLevel() >= 70 && getLevel() < 80)
				knightId = 25792;

			if(knightId > 0)
			{
				NpcInstance npc = NpcUtils.spawnSingle(knightId, getLoc(), 900000L);
				npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, killer, 1000);
			}
		}

		super.onDeath(killer);
		RaidBossSpawnManager.getInstance().onBossDeath(this);
	}

	@Override
	protected void onSpawn()
	{
		super.onSpawn();
		addSkill(SkillHolder.getInstance().getSkillEntry(4045, 1)); // Resist Full Magic Attack
		RaidBossSpawnManager.getInstance().onBossSpawned(this);
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isParalyzeImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	@Override
	public boolean hasRandomWalk()
	{
		return false;
	}

	@Override
	public boolean canChampion()
	{
		return false;
	}

	@Override
	public void onZoneEnter(Zone zone)
	{
		if(!zone.checkIfInZone(getSpawnedLoc().getX(), getSpawnedLoc().getY(), getSpawnedLoc().getZ()))
		{
			if(zone.getType() == Zone.ZoneType.peace_zone || zone.getType() == Zone.ZoneType.battle_zone || zone.getType() == Zone.ZoneType.SIEGE)
				getAI().returnHomeAndRestore(isRunning());
		}
	}
}