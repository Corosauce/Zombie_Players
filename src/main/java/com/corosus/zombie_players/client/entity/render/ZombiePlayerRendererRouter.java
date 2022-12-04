package com.corosus.zombie_players.client.entity.render;

import com.corosus.zombie_players.client.entity.model.ZombiePlayerModelZombieBased;
import com.corosus.zombie_players.entity.ZombiePlayer;
import com.corosus.zombie_players.util.UtilProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombiePlayerRendererRouter extends ZombiePlayerRendererAbstract<ZombiePlayer, ZombiePlayerModelZombieBased<ZombiePlayer>> {

   private ZombiePlayerRendererImpl renderNormal;
   private ZombiePlayerRendererImpl renderSlim;

   public ZombiePlayerRendererRouter(EntityRendererProvider.Context p_174456_) {
      this(p_174456_, ModelLayers.PLAYER_SLIM, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);
      renderNormal = new ZombiePlayerRendererImpl(p_174456_, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);
      renderSlim = new ZombiePlayerRendererImpl(p_174456_, ModelLayers.PLAYER_SLIM, ModelLayers.PLAYER_SLIM_INNER_ARMOR, ModelLayers.PLAYER_SLIM_OUTER_ARMOR);
   }

   public ZombiePlayerRendererRouter(EntityRendererProvider.Context p_174458_, ModelLayerLocation p_174459_, ModelLayerLocation p_174460_, ModelLayerLocation p_174461_) {
      super(p_174458_, new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174459_)), new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174460_)), new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174461_)));
   }

   @Override
   public void render(ZombiePlayer entity, float p_115456_, float partialTicks, PoseStack poseStack, MultiBufferSource p_115459_, int p_115460_) {
      UtilProfile.CachedPlayerData data = getCachedPlayerData(entity);
      if (data != null) {
         if (data.isSlim()) {
            renderSlim.render(entity, p_115456_, partialTicks, poseStack, p_115459_, p_115460_);
         } else {
            renderNormal.render(entity, p_115456_, partialTicks, poseStack, p_115459_, p_115460_);
         }
      }
   }

   @Override
   protected void setupRotations(ZombiePlayer entityLiving, PoseStack p_115318_, float p_115319_, float p_115320_, float partialTicks) {
      UtilProfile.CachedPlayerData data = getCachedPlayerData(entityLiving);
      if (data != null) {
         if (data.isSlim()) {
            renderSlim.setupRotations(entityLiving, p_115318_, p_115319_, p_115320_, partialTicks);
         } else {
            renderNormal.setupRotations(entityLiving, p_115318_, p_115319_, p_115320_, partialTicks);
         }
      }
   }
}