package com.corosus.zombie_players.client.entity.render;

import com.corosus.zombie_players.client.entity.model.ZombiePlayerModelZombieBased;
import com.corosus.zombie_players.entity.ZombiePlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombiePlayerRendererImpl extends ZombiePlayerRendererAbstract<ZombiePlayer, ZombiePlayerModelZombieBased<ZombiePlayer>> {
   public ZombiePlayerRendererImpl(EntityRendererProvider.Context p_174456_) {
      this(p_174456_, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_INNER_ARMOR, ModelLayers.ZOMBIE_OUTER_ARMOR);
   }

   public ZombiePlayerRendererImpl(EntityRendererProvider.Context p_174458_, ModelLayerLocation p_174459_, ModelLayerLocation p_174460_, ModelLayerLocation p_174461_) {
      super(p_174458_, new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174459_)), new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174460_)), new ZombiePlayerModelZombieBased<>(p_174458_.bakeLayer(p_174461_)));
   }

   @Override
   public void render(ZombiePlayer entity, float p_115456_, float partialTicks, PoseStack poseStack, MultiBufferSource p_115459_, int p_115460_) {


      if (entity.risingTime < entity.risingTimeMax) {

         //request data ASAP, load while still invis
         getCachedPlayerData(entity);

         float f = 1F - ((entity.risingTime + partialTicks) / (float) entity.risingTimeMax);
         float shadowDelay = 20;
         float fInv = ((entity.risingTime - shadowDelay + partialTicks) / (float) (entity.risingTimeMax - shadowDelay));

         if (f > 1.0F) {
            f = 1.0F;
         }

         poseStack.translate(0, -f * 0.7F, 0);

         shadowStrength = 0;
      } else {
         shadowStrength = 1;


      }

      if (entity.risingTime >= 0) {
         super.render(entity, p_115456_, partialTicks, poseStack, p_115459_, p_115460_);
      }
   }

   @Override
   protected void setupRotations(ZombiePlayer entityLiving, PoseStack p_115318_, float p_115319_, float p_115320_, float partialTicks) {

      if (entityLiving.risingTime < entityLiving.risingTimeMax) {
         float f = 1F - ((entityLiving.risingTime + partialTicks) / (float) entityLiving.risingTimeMax);

         if (f > 1.0F) {
            f = 1.0F;
         }

         /**
          * angle = rise
          * angle2 = get up from side
          * angle3 = shake
          */

         double rotScale = Math.sin(f);
         //use 180 to play with rate change scale lazily
         double angle = (f * 90F) * rotScale;
         double angle2 = 0;//(f * 90F) * rotScale;

         double shakeRange = 30;
         double shakeSpeed = 70;
         double timeVal = ((entityLiving.level.getGameTime() + partialTicks) * shakeSpeed);
         double angle3 = ((shakeRange / 2) + Math.sin(Math.toRadians((timeVal % 360))) * shakeRange) * rotScale;

         p_115318_.mulPose(Vector3f.XP.rotationDegrees((float) (angle)));
         p_115318_.mulPose(Vector3f.YP.rotationDegrees((float) (angle2 + angle3)));
      }

      super.setupRotations(entityLiving, p_115318_, p_115319_, p_115320_, partialTicks);
   }
}