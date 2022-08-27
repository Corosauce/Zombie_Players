package com.corosus.zombie_players;

import com.corosus.modconfig.ConfigMod;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.EnumTrainType;
import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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

		if (ent instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) event.getEntityLiving();

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
		if (!event.getWorld().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND) {
			if (Zombie_Players.getWorkAreaStage(event.getPlayer()) == 1) {
				event.getPlayer().getPersistentData().putInt(Zombie_Players.ZP_SET_WORK_AREA_STAGE, 2);
				List<ZombiePlayer> listEnts = event.getWorld().getEntitiesOfClass(ZombiePlayer.class, new AABB(event.getPlayer().blockPosition()).inflate(10, 5, 10));
				for (ZombiePlayer ent : listEnts) {
					if (ent.isCalm() && ent.getWorkInfo().isInAreaSetMode() && ent.getOwnerUUID().equals(event.getPlayer().getUUID())) {
						ent.getWorkInfo().setWorkAreaPos1(event.getPos());
						event.getPlayer().sendMessage(new TextComponent("First work area position set, right click second block with golden hoe"), new UUID(0, 0));
					}
				}
			} else if (Zombie_Players.getWorkAreaStage(event.getPlayer()) == 2) {
				event.getPlayer().getPersistentData().putInt(Zombie_Players.ZP_SET_WORK_AREA_STAGE, 0);

				List<ZombiePlayer> listEnts = event.getWorld().getEntitiesOfClass(ZombiePlayer.class, new AABB(event.getPlayer().blockPosition()).inflate(10, 5, 10));
				for (ZombiePlayer ent : listEnts) {
					if (ent.isCalm() && ent.getWorkInfo().isInAreaSetMode() && ent.getOwnerUUID().equals(event.getPlayer().getUUID())) {
						AABB aabb = new AABB(
								Math.min(ent.getWorkInfo().getWorkAreaPos1().getX(), event.getPos().getX()),
								Math.min(ent.getWorkInfo().getWorkAreaPos1().getY(), event.getPos().getY()),
								Math.min(ent.getWorkInfo().getWorkAreaPos1().getZ(), event.getPos().getZ()),
								Math.max(ent.getWorkInfo().getWorkAreaPos1().getX(), event.getPos().getX()),
								Math.max(ent.getWorkInfo().getWorkAreaPos1().getY(), event.getPos().getY()),
								Math.max(ent.getWorkInfo().getWorkAreaPos1().getZ(), event.getPos().getZ()));
						ent.getWorkInfo().setPosWorkArea(aabb);
						ent.getWorkInfo().setInAreaSetMode(false);
						event.getPlayer().sendMessage(new TextComponent("Zombie Player " + ent.getGameProfile().getName() + " work area set to " + aabb), new UUID(0, 0));
					}
				}


			}
		}
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
			if (ent.isCalm() && ent.getWorkInfo().isInTrainingMode() && ent.getOwnerUUID().equals(player.getUUID())) {
				//ent.workClickLastObserved = trainType;
				ent.getWorkInfo().setWorkClickLastObserved(trainType);
				BlockState state = ent.level.getBlockState(pos);
				ent.getWorkInfo().setStateWorkLastObserved(state);
				ent.getWorkInfo().setWorkClickDirectionLastObserved(direction);
				ent.getWorkInfo().setItemNeededForWork(player.getMainHandItem());
				player.sendMessage(new TextComponent("Zombie Player " + ent.getName() + " observed " + state + " using " + player.getMainHandItem()), new UUID(0, 0));
			}
		}
	}
}
