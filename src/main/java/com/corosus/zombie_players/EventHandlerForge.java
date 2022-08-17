package com.corosus.zombie_players;

import com.corosus.modconfig.ConfigMod;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.ZombiePlayerNew;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandlerForge {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingDeath(LivingDeathEvent event) {
		LivingEntity ent = event.getEntityLiving();
		if (ent.level.isClientSide()) return;
		if (event.isCanceled()) return;

		if (ent instanceof Player) {
			Player player = (Player) event.getEntityLiving();

			if (!ConfigZombiePlayers.requiresDeathByZombieToSpawnZombiePlayer || event.getSource().getDirectEntity() instanceof Zombie) {
				ZombiePlayerNew.spawnInPlaceOfPlayer(player);
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
}
