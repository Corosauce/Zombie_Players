package com.corosus.zombie_players;

import com.corosus.zombie_players.client.entity.render.ZombiePlayerRendererImpl;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRegistry {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerRenderers(FMLClientSetupEvent event) {
        EntityRenderers.register(EntityRegistry.zombie_player, render -> new ZombiePlayerRendererImpl(render));
    }

}
