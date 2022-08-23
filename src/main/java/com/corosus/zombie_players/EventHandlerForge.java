package com.corosus.zombie_players;

import com.corosus.modconfig.ConfigMod;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.EnumTrainType;
import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class EventHandlerForge {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingDeath(LivingDeathEvent event) {
		LivingEntity ent = event.getEntityLiving();
		if (ent.level.isClientSide()) return;
		if (event.isCanceled()) return;

		if (ent instanceof Player) {
			Player player = (Player) event.getEntityLiving();

			if (!ConfigZombiePlayers.requiresDeathByZombieToSpawnZombiePlayer || event.getSource().getDirectEntity() instanceof Zombie) {
				ZombiePlayer.spawnInPlaceOfPlayer(player);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		if (ConfigZombiePlayers.automaticallyAddJoinedPlayersToNamesList) {
			if (!ConfigZombiePlayers.Spawning_playerNamesToUse.contains(event.getEntity().getName().getString())) {
				if (ConfigZombiePlayers.Spawning_playerNamesToUse.length() == 0) {
					ConfigZombiePlayers.Spawning_playerNamesToUse = event.getEntity().getName().getString();
				} else {
					ConfigZombiePlayers.Spawning_playerNamesToUse += ", " + event.getEntity().getName().toString();
				}
				ConfigMod.forceSaveAllFilesFromRuntimeSettings();
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		trainZombiePlayer(event.getPlayer(), event.getWorld(), event.getPos(), EnumTrainType.BLOCK_RIGHT_CLICK, event.getFace());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {

		trainZombiePlayer(event.getPlayer(), event.getWorld(), event.getPos(), EnumTrainType.BLOCK_LEFT_CLICK, event.getFace());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
		//trainZombiePlayer(event.getWorld(), event.getPlayer().blockPosition(), EnumTrainType.ENTITY_RIGHT_CLICK);
	}



	public void trainZombiePlayer(Player player, Level level, BlockPos pos, EnumTrainType trainType, Direction direction) {
		List<ZombiePlayer> listEnts = level.getEntitiesOfClass(ZombiePlayer.class, new AABB(pos).inflate(10, 5, 10));
		for (ZombiePlayer ent : listEnts) {
			if (ent.isCalm() && ent.getWorkInfo().isInTrainingMode()) {
				//ent.workClickLastObserved = trainType;
				ent.getWorkInfo().setWorkClickLastObserved(trainType);
				BlockState state = ent.level.getBlockState(pos);
				ent.getWorkInfo().setStateWorkLastObserved(state);
				ent.getWorkInfo().setWorkClickDirectionLastObserved(direction);
				player.sendMessage(new TextComponent("Zombie Player observed " + state), new UUID(0, 0));
			}
		}
	}
}
