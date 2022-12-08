package com.corosus.zombie_players;

import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRegistry {

    @ObjectHolder(Zombie_Players.MODID + ":zombie_player")
    public static EntityType<ZombiePlayer> zombie_player;

    @SubscribeEvent
    public static void registerEntity(RegistryEvent.Register<EntityType<?>> e) {
        IForgeRegistry<EntityType<?>> r = e.getRegistry();
        r.register(
                EntityType.Builder.of(ZombiePlayer::new, MobCategory.MONSTER)
                        .setShouldReceiveVelocityUpdates(false)
                        .setUpdateInterval(3)
                        .setTrackingRange(128)
                        .sized(0.6F, 1.95F)
                        .build("zombie_player")
                        .setRegistryName("zombie_player"));
    }

    @SubscribeEvent
    public static void initializeAttributes(EntityAttributeCreationEvent event) {
        event.put(zombie_player, ZombiePlayer.createAttributes().build());
    }

}
