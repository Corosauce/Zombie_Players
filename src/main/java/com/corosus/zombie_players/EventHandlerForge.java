package com.corosus.zombie_players;

import com.corosus.zombie_players.entity.EntityZombiePlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EventHandlerForge {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingDeath(LivingDeathEvent event) {
		EntityLivingBase ent = event.getEntityLiving();
		if (ent.world.isRemote) return;
		if (event.isCanceled()) return;

		if (ent instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();

			EntityZombiePlayer.spawnInPlaceOfPlayer(player);
		}
	}
}
