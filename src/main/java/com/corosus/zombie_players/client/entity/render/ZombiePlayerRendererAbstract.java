package com.corosus.zombie_players.client.entity.render;

import com.corosus.zombie_players.client.entity.layer.ZombificationLayer;
import com.corosus.zombie_players.client.entity.model.ZombiePlayerModelZombieBased;
import com.corosus.zombie_players.entity.ZombiePlayerNew;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.SaddleLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ZombiePlayerRendererAbstract<T extends ZombiePlayerNew, M extends ZombiePlayerModelZombieBased<T>> extends HumanoidMobRenderer<T, M> {
   private static final ResourceLocation ZOMBIE_LOCATION = new ResourceLocation("textures/entity/zombie/zombie.png");

   protected ZombiePlayerRendererAbstract(EntityRendererProvider.Context p_173910_, M p_173911_, M p_173912_, M p_173913_) {
      super(p_173910_, p_173911_, 0.5F);
      this.addLayer(new HumanoidArmorLayer<>(this, p_173912_, p_173913_));
      //this.addLayer(new SaddleLayer<>(this, new PigModel<>(p_173910_.bakeLayer(ModelLayers.PIG_SADDLE)), new ResourceLocation("textures/entity/pig/pig_saddle.png")));
      this.addLayer(new ZombificationLayer<>(this, new ZombiePlayerModelZombieBased<>(p_173910_.bakeLayer(ModelLayers.PIG_SADDLE)), new ResourceLocation("textures/entity/zombification.png")));
   }

   public ResourceLocation getTextureLocation(ZombiePlayerNew p_113771_) {
      return ZOMBIE_LOCATION;
   }

   protected boolean isShaking(T p_113773_) {
      return super.isShaking(p_113773_) || p_113773_.isUnderWaterConverting();
   }
}