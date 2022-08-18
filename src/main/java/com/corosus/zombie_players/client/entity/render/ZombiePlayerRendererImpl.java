package com.corosus.zombie_players.client.entity.render;

import com.corosus.zombie_players.client.entity.model.ZombiePlayerModelZombieBased;
import com.corosus.zombie_players.entity.ZombiePlayerNew;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombiePlayerRendererImpl extends ZombiePlayerRendererAbstract<ZombiePlayerNew, ZombiePlayerModelZombieBased<ZombiePlayerNew>> {
   public ZombiePlayerRendererImpl(EntityRendererProvider.Context p_174456_) {
      this(p_174456_, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);
   }

   public ZombiePlayerRendererImpl(EntityRendererProvider.Context p_174458_, ModelLayerLocation p_174459_, ModelLayerLocation p_174460_, ModelLayerLocation p_174461_) {
      super(p_174458_, new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174459_)), new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174460_)), new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174461_)));
   }
}