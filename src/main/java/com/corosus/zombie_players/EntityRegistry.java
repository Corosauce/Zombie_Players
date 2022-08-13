package com.corosus.zombie_players;

import com.corosus.zombie_players.entity.ZombiePlayerNew;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRegistry {

    @ObjectHolder(Zombie_Players.MODID + ":zombie_player")
    public static EntityType<ZombiePlayerNew> zombie_player;

    @SubscribeEvent
    public static void registerEntity(RegistryEvent.Register<EntityType<?>> e) {
        IForgeRegistry<EntityType<?>> r = e.getRegistry();
        r.register(
                EntityType.Builder.of(ZombiePlayerNew::new, MobCategory.MISC)
                        .setShouldReceiveVelocityUpdates(false)
                        .setUpdateInterval(20)
                        .setTrackingRange(128)
                        .sized(0f, 0f)
                        .build("zombie_player")
                        .setRegistryName("zombie_player"));
    }

}
